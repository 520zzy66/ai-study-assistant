package com.study.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.ai.MaterialContentReader;
import com.study.ai.client.AiClient;
import com.study.ai.prompt.PromptTemplates;
import com.study.ai.validator.MaterialValidator;
import com.study.common.BusinessException;
import com.study.common.Constants;
import com.study.common.JsonUtils;
import com.study.common.UserContext;
import static com.study.common.JsonUtils.toJson;
import com.study.dto.request.GenerateQuizRequest;
import com.study.dto.request.SubmitAnswerRequest;
import com.study.entity.*;
import com.study.mapper.*;
import com.study.vo.WrongQuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 自动出题服务
 * 负责生成练习题、自动判分、错题本管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuizService {

    private final AiClient aiClient;
    private final MaterialValidator materialValidator;
    private final MaterialContentReader contentReader;
    private final AiQuestionBankMapper questionBankMapper;
    private final AiQuizRecordMapper quizRecordMapper;
    private final UserWrongQuestionMapper wrongQuestionMapper;
    private final LearningMaterialMapper materialMapper;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;

    /** 批量插入每批次大小 */
    private static final int BATCH_SIZE = 100;

    /**
     * 生成练习题
     * 注意：无 @Transactional — Db.saveBatch 自行管理 SqlSession 不参与 Spring 事务。
     * 若需保证题目入库与对话历史写入的原子性，需将 Db.saveBatch 替换为注入 Mapper 的批量插入。
     *
     * @param materialId 资料ID
     * @param request    出题参数
     * @return 题目列表 + batchId
     */
    public Map<String, Object> generateQuiz(Long materialId, GenerateQuizRequest request) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验资料
        materialValidator.validateAndGet(materialId, userId);

        // 2. 参数默认值
        int choiceCount = request.getChoiceCount() != null ? request.getChoiceCount() : 5;
        int judgeCount = request.getJudgeCount() != null ? request.getJudgeCount() : 3;
        int shortAnswerCount = request.getShortAnswerCount() != null ? request.getShortAnswerCount() : 2;
        String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "medium";

        int totalCount = choiceCount + judgeCount + shortAnswerCount;
        if (totalCount == 0) {
            throw new BusinessException(400, "至少需要生成一道题目");
        }
        if (totalCount > 20) {
            throw new BusinessException(3006, "单次出题最多20道");
        }

        // 3. 读取资料内容（MaterialContentReader 自动处理超长截断）
        String content = contentReader.readContent(materialId);

        // 4. 构建 Prompt 并调用 AI（temperature 0.3 保证 JSON 格式稳定）
        String prompt = PromptTemplates.buildQuizPrompt(
                content, choiceCount, judgeCount, shortAnswerCount, difficulty);
        String aiResponse = aiClient.chat(prompt, 0.3);

        // 5. 解析 AI 返回的 JSON
        List<QuizQuestion> questions = parseQuizResponse(aiResponse);
        if (questions.isEmpty()) {
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }

        // 6. 生成 batchId 并批量保存到题库
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        List<AiQuestionBank> entities = new ArrayList<>();
        for (QuizQuestion q : questions) {
            AiQuestionBank entity = new AiQuestionBank();
            entity.setUserId(userId);
            entity.setMaterialId(materialId);
            entity.setBatchId(batchId);
            entity.setQuestionType(q.getType());
            entity.setDifficulty(difficulty);
            entity.setQuestion(q.getQuestion());
            entity.setOptions(q.getOptions() != null ? toJson(q.getOptions()) : null);
            entity.setAnswer(q.getAnswer());
            entity.setExplanation(q.getExplanation());
            entities.add(entity);
        }
        // 逐条插入，参与 Spring 事务（替代 Db.saveBatch，避免绕过事务）
        for (AiQuestionBank entity : entities) {
            questionBankMapper.insert(entity);
        }
        // 回填 ID（MyBatis-Plus insert 后自动回填到 entity）
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setId(entities.get(i).getId());
        }

        // 7. 保存对话历史
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setChatType(Constants.CHAT_TYPE_QUIZ);
        history.setMaterialId(materialId);
        history.setUserMessage("生成" + totalCount + "道" + difficulty + "难度练习题");
        history.setAiResponse(aiResponse);
        history.setBatchId(batchId);
        chatHistoryMapper.insert(history);

        // 8. 构建返回（隐藏选择题答案中的 options 数据过大问题）
        List<Map<String, Object>> questionList = questions.stream()
                .map(q -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", q.getId());
                    map.put("type", q.getType());
                    map.put("difficulty", difficulty);
                    map.put("question", q.getQuestion());
                    map.put("options", q.getOptions());
                    map.put("answer", q.getAnswer());
                    map.put("explanation", q.getExplanation());
                    return map;
                }).collect(Collectors.toList());

        return Map.of("materialId", materialId, "batchId", batchId, "questions", questionList);
    }

    /**
     * 提交答案并判分
     *
     * @param batchId 批次ID
     * @param request 答案列表
     * @return 判分结果
     */
    public Map<String, Object> submitAnswers(String batchId, SubmitAnswerRequest request) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 加载批次题目
        List<AiQuestionBank> questions = questionBankMapper.selectList(
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getBatchId, batchId)
                        .eq(AiQuestionBank::getUserId, userId)
        );

        if (questions.isEmpty()) {
            throw new BusinessException(3007, "批次不存在或不属于当前用户");
        }

        // 2. 建立题目索引
        Map<Long, AiQuestionBank> questionMap = questions.stream()
                .collect(Collectors.toMap(AiQuestionBank::getId, q -> q));

        // 3. 逐题判分
        List<Map<String, Object>> details = new ArrayList<>();
        int correctCount = 0;
        int totalCount = 0;

        for (SubmitAnswerRequest.AnswerItem item : request.getAnswers()) {
            AiQuestionBank question = questionMap.get(item.getQuestionId());
            if (question == null) {
                throw new BusinessException(3008, "题目 " + item.getQuestionId() + " 不属于该批次");
            }

            totalCount++;
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("questionId", item.getQuestionId());
            detail.put("userAnswer", item.getAnswer());
            detail.put("correctAnswer", question.getAnswer());
            detail.put("explanation", question.getExplanation());

            boolean isCorrect;
            BigDecimal score = BigDecimal.ZERO;

            // 选择题和判断题：精确匹配（防御 AI 返回 answer 为 null）
            if ("choice".equals(question.getQuestionType()) || "judge".equals(question.getQuestionType())) {
                isCorrect = item.getAnswer() != null && question.getAnswer() != null &&
                        item.getAnswer().trim().equalsIgnoreCase(question.getAnswer().trim());
                if (isCorrect) {
                    correctCount++;
                    score = BigDecimal.ONE;
                }
            }
            // 简答题：AI 语义判分
            else {
                Map<String, Object> grading = gradeShortAnswer(
                        question.getQuestion(), question.getAnswer(), item.getAnswer());
                Object scoreObj = grading.get("score");
                double gradingScore = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
                score = BigDecimal.valueOf(gradingScore);
                isCorrect = gradingScore >= 0.6;
                if (isCorrect) correctCount++;
                detail.put("feedback", grading.getOrDefault("feedback", ""));
            }

            detail.put("isCorrect", isCorrect);
            detail.put("score", score.doubleValue());

            // 保存作答记录
            AiQuizRecord record = new AiQuizRecord();
            record.setUserId(userId);
            record.setBatchId(batchId);
            record.setQuestionId(item.getQuestionId());
            record.setUserAnswer(item.getAnswer());
            record.setIsCorrect(isCorrect ? 1 : 0);
            record.setScore(score);
            quizRecordMapper.insert(record);

            // 错题自动加入错题本
            if (!isCorrect) {
                addToWrongBook(userId, question, item.getAnswer());
            }

            details.add(detail);
        }

        double scorePercent = totalCount > 0
                ? Math.round((double) correctCount / totalCount * 1000.0) / 10.0
                : 0;

        return Map.of(
                "batchId", batchId,
                "totalCount", totalCount,
                "correctCount", correctCount,
                "score", scorePercent,
                "details", details
        );
    }

    /**
     * 获取错题列表
     *
     * @param materialId  可选，按资料筛选
     * @param isMastered  可选，按掌握状态筛选
     * @return 错题列表
     */
    public List<WrongQuestionVO> getWrongQuestions(Long materialId, Boolean isMastered) {
        Long userId = UserContext.getCurrentUserId();

        LambdaQueryWrapper<UserWrongQuestion> wrapper = new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(materialId != null, UserWrongQuestion::getMaterialId, materialId)
                .eq(isMastered != null, UserWrongQuestion::getIsMastered, isMastered != null ? (isMastered ? 1 : 0) : null)
                .orderByDesc(UserWrongQuestion::getLastWrongTime);

        List<UserWrongQuestion> wrongList = wrongQuestionMapper.selectList(wrapper);

        // 转换为 VO，补充题目文本和资料名称
        return wrongList.stream().map(wrong -> {
            WrongQuestionVO vo = new WrongQuestionVO();
            vo.setId(wrong.getId());
            vo.setQuestionId(wrong.getQuestionId());
            vo.setMaterialId(wrong.getMaterialId());
            vo.setQuestionType(wrong.getQuestionType());
            vo.setUserAnswer(wrong.getUserAnswer());
            vo.setCorrectAnswer(wrong.getCorrectAnswer());
            vo.setWrongCount(wrong.getWrongCount());
            vo.setLastWrongTime(wrong.getLastWrongTime());
            vo.setIsMastered(wrong.getIsMastered());

            // 查询题目文本
            AiQuestionBank question = questionBankMapper.selectById(wrong.getQuestionId());
            if (question != null) {
                vo.setQuestion(question.getQuestion());
            }

            // 查询资料名称
            LearningMaterial material = materialMapper.selectById(wrong.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getOriginalName());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 标记错题已掌握
     */
    public void markWrongQuestionMastered(Long id) {
        Long userId = UserContext.getCurrentUserId();

        UserWrongQuestion wrong = wrongQuestionMapper.selectById(id);
        if (wrong == null || !wrong.getUserId().equals(userId)) {
            throw new BusinessException(404, "错题记录不存在");
        }

        UserWrongQuestion update = new UserWrongQuestion();
        update.setId(id);
        update.setIsMastered(1);
        update.setMasterTime(LocalDateTime.now());
        wrongQuestionMapper.updateById(update);
    }

    // === 私有方法 ===

    /**
     * 解析 AI 返回的题目 JSON
     */
    private List<QuizQuestion> parseQuizResponse(String aiResponse) {
        // 防御：AI 返回 null 或 blank 时提前返回
        if (aiResponse == null || aiResponse.isBlank()) {
            log.warn("AI 返回为空");
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }
        try {
            // 清理可能的 markdown 代码块标记
            String json = JsonUtils.cleanJsonResponse(aiResponse);

            Map<String, Object> map = objectMapper.readValue(json,
                    new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionList = (List<Map<String, Object>>) map.get("questions");

            if (questionList == null || questionList.isEmpty()) {
                log.warn("AI 返回的 questions 为空");
                return List.of();
            }

            return questionList.stream().map(q -> {
                QuizQuestion qq = new QuizQuestion();
                qq.setType((String) q.get("type"));
                qq.setQuestion((String) q.get("question"));
                qq.setAnswer((String) q.get("answer"));
                qq.setExplanation((String) q.get("explanation"));

                // options 可能是 Map 或 null
                Object opts = q.get("options");
                if (opts instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> optMap = (Map<String, String>) opts;
                    qq.setOptions(optMap);
                }
                return qq;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            // aiResponse 已在入口判空，此处安全截取前 200 字符用于日志
            String preview = aiResponse.length() > 200 ? aiResponse.substring(0, 200) : aiResponse;
            log.error("解析 AI 返回的题目 JSON 失败: {}", preview, e);
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }
    }

    /**
     * 简答题 AI 语义判分
     */
    private Map<String, Object> gradeShortAnswer(String question, String referenceAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return Map.of("score", 0.0, "feedback", "未作答");
        }

        try {
            String prompt = PromptTemplates.buildGradingPrompt(question, referenceAnswer, studentAnswer);
            String response = aiClient.chat(prompt, 0.1);

            // 清理 markdown 标记
            String json = JsonUtils.cleanJsonResponse(response);

            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("AI 判分失败，降级为模糊匹配", e);
            // 降级：字符串模糊匹配
            double score = fuzzyMatch(referenceAnswer, studentAnswer);
            return Map.of("score", score, "feedback", score >= 0.6 ? "基本正确" : "与参考答案差异较大");
        }
    }

    /**
     * 简单模糊匹配（降级方案）
     */
    private double fuzzyMatch(String reference, String student) {
        if (reference == null || student == null) return 0;
        String ref = reference.toLowerCase().trim();
        String stu = student.toLowerCase().trim();
        if (ref.equals(stu)) return 1.0;
        if (stu.contains(ref) || ref.contains(stu)) return 0.9;

        // 关键词重叠度
        Set<String> refWords = new HashSet<>(Arrays.asList(ref.split("[,，。；;\\s]+")));
        Set<String> stuWords = new HashSet<>(Arrays.asList(stu.split("[,，。；;\\s]+")));
        if (refWords.isEmpty()) return 0;

        long overlap = refWords.stream().filter(stuWords::contains).count();
        return Math.min(0.8, (double) overlap / refWords.size());
    }

    /**
     * 错题加入错题本
     * 使用 INSERT 尝试 + 捕获唯一键冲突后原子 UPDATE，避免 TOCTOU 竞态；
     * wrong_count 通过 SQL SET wrong_count = wrong_count + 1 原子递增
     */
    private void addToWrongBook(Long userId, AiQuestionBank question, String userAnswer) {
        try {
            // 尝试新增（依赖 uk_user_question 唯一索引）
            UserWrongQuestion wrong = new UserWrongQuestion();
            wrong.setUserId(userId);
            wrong.setQuestionId(question.getId());
            wrong.setMaterialId(question.getMaterialId());
            wrong.setQuestionType(question.getQuestionType());
            wrong.setUserAnswer(userAnswer);
            wrong.setCorrectAnswer(question.getAnswer());
            wrong.setWrongCount(1);
            wrong.setLastWrongTime(LocalDateTime.now());
            wrong.setIsMastered(0);
            wrongQuestionMapper.insert(wrong);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 已存在 → 原子累加错误次数，不覆盖原始 user_answer 和 correct_answer
            wrongQuestionMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserWrongQuestion>()
                            .eq(UserWrongQuestion::getUserId, userId)
                            .eq(UserWrongQuestion::getQuestionId, question.getId())
                            .set(UserWrongQuestion::getIsMastered, 0)
                            .set(UserWrongQuestion::getLastWrongTime, LocalDateTime.now())
                            .setSql("wrong_count = wrong_count + 1"));
        }
    }

    /**
     * 内部题目解析类
     */
    @lombok.Data
    private static class QuizQuestion {
        private Long id;
        private String type;
        private String question;
        private Map<String, String> options;
        private String answer;
        private String explanation;
    }
}
