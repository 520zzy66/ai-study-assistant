package com.study.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.study.dto.request.GenerateQuizRequest;
import com.study.dto.request.SubmitAnswerRequest;
import com.study.entity.*;
import com.study.mapper.*;
import com.study.vo.WrongQuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     *
     * @param materialId 资料ID
     * @param request    出题参数（含可选的 batchName）
     * @return 题目列表 + batchId
     */
    public Map<String, Object> generateQuiz(Long materialId, GenerateQuizRequest request) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验资料
        LearningMaterial material = materialValidator.validateAndGet(materialId, userId);

        // 2. 参数默认值
        int choiceCount = request.getChoiceCount() != null ? request.getChoiceCount() : 5;
        int judgeCount = request.getJudgeCount() != null ? request.getJudgeCount() : 3;
        int shortAnswerCount = request.getShortAnswerCount() != null ? request.getShortAnswerCount() : 2;
        int fillBlankCount = request.getFillBlankCount() != null ? request.getFillBlankCount() : 0;
        int multiChoiceCount = request.getMultiChoiceCount() != null ? request.getMultiChoiceCount() : 0;
        int mathFillCount = request.getMathFillCount() != null ? request.getMathFillCount() : 0;
        String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "medium";

        int totalCount = choiceCount + judgeCount + shortAnswerCount + fillBlankCount + multiChoiceCount + mathFillCount;
        if (totalCount == 0) {
            throw new BusinessException(400, "至少需要生成一道题目");
        }
        if (totalCount > 20) {
            throw new BusinessException(3006, "单次出题最多20道");
        }

        // 3. 自动生成批次名称（用户指定优先，否则用资料名+时间）
        String batchName = request.getBatchName();
        if (batchName == null || batchName.isBlank()) {
            batchName = material.getOriginalName() + " - "
                    + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        }

        // 4. 读取资料内容（MaterialContentReader 自动处理超长截断）
        String content = contentReader.readContent(materialId);

        // 5. 构建 Prompt 并调用 AI（temperature 0.3 保证 JSON 格式稳定）
        String prompt = PromptTemplates.buildQuizPrompt(
                content, choiceCount, judgeCount, shortAnswerCount,
                fillBlankCount, multiChoiceCount, mathFillCount, difficulty);
        String aiResponse = aiClient.chat(prompt, 0.3);

        // 6. 解析 AI 返回的 JSON
        List<QuizQuestion> questions = parseQuizResponse(aiResponse);
        if (questions.isEmpty()) {
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }

        // 7. 生成 batchId 并批量保存到题库
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        List<AiQuestionBank> entities = new ArrayList<>();
        for (QuizQuestion q : questions) {
            AiQuestionBank entity = new AiQuestionBank();
            entity.setUserId(userId);
            entity.setMaterialId(materialId);
            entity.setBatchId(batchId);
            entity.setBatchName(batchName);
            entity.setQuestionType(q.getType());
            entity.setDifficulty(difficulty);
            entity.setQuestion(q.getQuestion());
            entity.setOptions(q.getOptions());
            entity.setAnswer(q.getAnswer());
            entity.setExplanation(q.getExplanation());
            entity.setIsFavorite(0);
            entities.add(entity);
        }
        // 批量插入（替代逐条 insert，性能提升 5-10 倍）
        Db.saveBatch(entities);
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setId(entities.get(i).getId());
        }

        // 8. 保存对话历史
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setChatType(Constants.CHAT_TYPE_QUIZ);
        history.setMaterialId(materialId);
        history.setUserMessage("生成" + totalCount + "道" + difficulty + "难度练习题：" + batchName);
        history.setAiResponse(aiResponse);
        history.setBatchId(batchId);
        chatHistoryMapper.insert(history);

        // 9. 构建返回
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

        return Map.of("materialId", materialId, "batchId", batchId, "batchName", batchName, "questions", questionList);
    }

    /**
     * 生成文件夹练习题（混合出题）
     * 从向量数据库读取该文件夹下所有资料的切片内容，由AI混合出题
     *
     * @param folderId 文件夹ID
     * @param request  出题参数
     * @return 题目列表 + batchId
     */
    public Map<String, Object> generateFolderQuiz(Long folderId, GenerateQuizRequest request) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 参数默认值
        int choiceCount = request.getChoiceCount() != null ? request.getChoiceCount() : 5;
        int judgeCount = request.getJudgeCount() != null ? request.getJudgeCount() : 3;
        int shortAnswerCount = request.getShortAnswerCount() != null ? request.getShortAnswerCount() : 2;
        int fillBlankCount = request.getFillBlankCount() != null ? request.getFillBlankCount() : 0;
        int multiChoiceCount = request.getMultiChoiceCount() != null ? request.getMultiChoiceCount() : 0;
        int mathFillCount = request.getMathFillCount() != null ? request.getMathFillCount() : 0;
        String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "medium";

        int totalCount = choiceCount + judgeCount + shortAnswerCount + fillBlankCount + multiChoiceCount + mathFillCount;
        if (totalCount == 0) {
            throw new BusinessException(400, "至少需要生成一道题目");
        }
        if (totalCount > 20) {
            throw new BusinessException(3006, "单次出题最多20道");
        }

        // 2. 读取文件夹下所有资料的切片内容
        String content = contentReader.readFolderContent(folderId, userId);
        if (content.isBlank()) {
            throw new BusinessException(3001, "文件夹下没有可用资料或资料内容为空");
        }

        // 3. 获取文件夹下的资料ID列表
        List<Long> materialIds = contentReader.getFolderMaterialIds(folderId, userId);

        // 4. 自动生成批次名称
        String batchName = request.getBatchName();
        if (batchName == null || batchName.isBlank()) {
            batchName = "文件夹混合出题 - "
                    + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        }

        // 5. 构建 Prompt 并调用 AI
        String prompt = PromptTemplates.buildQuizPrompt(
                content, choiceCount, judgeCount, shortAnswerCount,
                fillBlankCount, multiChoiceCount, mathFillCount, difficulty);
        String aiResponse = aiClient.chat(prompt, 0.3);

        // 6. 解析 AI 返回的 JSON
        List<QuizQuestion> questions = parseQuizResponse(aiResponse);
        if (questions.isEmpty()) {
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }

        // 7. 生成 batchId 并批量保存到题库
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        List<AiQuestionBank> entities = new ArrayList<>();
        for (QuizQuestion q : questions) {
            AiQuestionBank entity = new AiQuestionBank();
            entity.setUserId(userId);
            entity.setMaterialId(materialIds.isEmpty() ? null : materialIds.get(0)); // 关联第一个资料
            entity.setBatchId(batchId);
            entity.setBatchName(batchName);
            entity.setQuestionType(q.getType());
            entity.setDifficulty(difficulty);
            entity.setQuestion(q.getQuestion());
            entity.setOptions(q.getOptions());
            entity.setAnswer(q.getAnswer());
            entity.setExplanation(q.getExplanation());
            entity.setIsFavorite(0);
            entities.add(entity);
        }
        Db.saveBatch(entities);
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setId(entities.get(i).getId());
        }

        // 8. 保存对话历史
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setChatType(Constants.CHAT_TYPE_QUIZ);
        history.setMaterialId(materialIds.isEmpty() ? null : materialIds.get(0));
        history.setUserMessage("生成文件夹混合练习题: folderId=" + folderId + ", " + totalCount + "道" + difficulty + "难度");
        history.setAiResponse(aiResponse);
        history.setBatchId(batchId);
        chatHistoryMapper.insert(history);

        // 9. 构建返回
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

        return Map.of("folderId", folderId, "materialCount", materialIds.size(),
                "batchId", batchId, "batchName", batchName, "questions", questionList);
    }

    /**
     * 提交答案并判分
     *
     * @param batchId 批次ID
     * @param request 答案列表
     * @return 判分结果
     */
    @Transactional(rollbackFor = Exception.class)
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

            // 选择题和判断题：精确匹配
            if ("choice".equals(question.getQuestionType()) || "judge".equals(question.getQuestionType())) {
                isCorrect = item.getAnswer() != null && question.getAnswer() != null &&
                        item.getAnswer().trim().equalsIgnoreCase(question.getAnswer().trim());
                if (isCorrect) {
                    correctCount++;
                    score = BigDecimal.ONE;
                }
            }
            // 填空题：精确匹配 + AI 语义判分
            else if ("fill_blank".equals(question.getQuestionType())) {
                Map<String, Object> grading = gradeFillBlank(
                        question.getQuestion(), question.getAnswer(), item.getAnswer());
                Object scoreObj = grading.get("score");
                double gradingScore = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
                score = BigDecimal.valueOf(gradingScore);
                isCorrect = gradingScore >= 0.6;
                if (isCorrect) correctCount++;
                detail.put("feedback", grading.getOrDefault("feedback", ""));
            }
            // 多选题：集合匹配（全对才得分）
            else if ("multi_choice".equals(question.getQuestionType())) {
                isCorrect = gradeMultiChoice(question.getAnswer(), item.getAnswer());
                if (isCorrect) {
                    correctCount++;
                    score = BigDecimal.ONE;
                }
            }
            // 数学填空题：数值容差匹配
            else if ("math_fill".equals(question.getQuestionType())) {
                Map<String, Object> grading = gradeMathFill(
                        question.getQuestion(), question.getAnswer(), item.getAnswer());
                Object scoreObj = grading.get("score");
                double gradingScore = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
                score = BigDecimal.valueOf(gradingScore);
                isCorrect = gradingScore >= 0.6;
                if (isCorrect) correctCount++;
                detail.put("feedback", grading.getOrDefault("feedback", ""));
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
        if (wrongList.isEmpty()) return List.of();

        // 批量查询题目和资料，避免 N+1
        Set<Long> questionIds = wrongList.stream().map(UserWrongQuestion::getQuestionId).collect(Collectors.toSet());
        Set<Long> materialIds = wrongList.stream().map(UserWrongQuestion::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, AiQuestionBank> questionMap = questionBankMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(AiQuestionBank::getId, q -> q));
        Map<Long, LearningMaterial> materialMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(LearningMaterial::getId, m -> m));

        // 转换为 VO
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

            AiQuestionBank question = questionMap.get(wrong.getQuestionId());
            if (question != null) {
                vo.setQuestion(question.getQuestion());
                vo.setOptions(question.getOptions());
            }

            LearningMaterial material = materialMap.get(wrong.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getOriginalName());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 重做错题 — 随机抽取未掌握的错题
     *
     * @param count 抽取数量（默认10）
     * @return 错题列表（含题目详情）
     */
    public List<WrongQuestionVO> getWrongQuestionsForRepractice(int count) {
        Long userId = UserContext.getCurrentUserId();
        if (count <= 0) count = 10;
        count = Math.min(count, 50);

        // 查询未掌握的错题
        List<UserWrongQuestion> wrongList = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<UserWrongQuestion>()
                        .eq(UserWrongQuestion::getUserId, userId)
                        .eq(UserWrongQuestion::getIsMastered, 0)
                        .orderByDesc(UserWrongQuestion::getWrongCount));

        if (wrongList.isEmpty()) return List.of();

        // 随机打乱后取 count 条
        List<UserWrongQuestion> shuffled = new ArrayList<>(wrongList);
        Collections.shuffle(shuffled);
        List<UserWrongQuestion> selected = shuffled.subList(0, Math.min(count, shuffled.size()));

        // 批量查询题目和资料
        Set<Long> questionIds = selected.stream().map(UserWrongQuestion::getQuestionId).collect(Collectors.toSet());
        Set<Long> materialIds = selected.stream().map(UserWrongQuestion::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, AiQuestionBank> questionMap = questionBankMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(AiQuestionBank::getId, q -> q));
        Map<Long, LearningMaterial> materialMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(LearningMaterial::getId, m -> m));

        return selected.stream().map(wrong -> {
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

            AiQuestionBank question = questionMap.get(wrong.getQuestionId());
            if (question != null) {
                vo.setQuestion(question.getQuestion());
                vo.setOptions(question.getOptions());
            }

            LearningMaterial material = materialMap.get(wrong.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getOriginalName());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 错题统计 — 按知识点、按日期、题型分布
     *
     * @param startDate 开始时间（可选）
     * @param endDate   结束时间（可选）
     * @return 统计数据
     */
    public Map<String, Object> getWrongQuestionStats(LocalDateTime startDate, LocalDateTime endDate) {
        Long userId = UserContext.getCurrentUserId();

        // 查询用户所有错题
        LambdaQueryWrapper<UserWrongQuestion> wrapper = new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .ge(startDate != null, UserWrongQuestion::getLastWrongTime, startDate)
                .le(endDate != null, UserWrongQuestion::getLastWrongTime, endDate)
                .orderByDesc(UserWrongQuestion::getLastWrongTime);

        List<UserWrongQuestion> wrongList = wrongQuestionMapper.selectList(wrapper);

        // 总计
        int totalCount = wrongList.size();
        int masteredCount = (int) wrongList.stream().filter(w -> w.getIsMastered() != null && w.getIsMastered() == 1).count();
        int unmasteredCount = totalCount - masteredCount;

        // 按题型分布
        Map<String, Long> byType = wrongList.stream()
                .collect(Collectors.groupingBy(
                        w -> w.getQuestionType() != null ? w.getQuestionType() : "unknown",
                        Collectors.counting()));

        // 按日期分布（最近30天，按天分组）
        Map<String, Long> byDate = wrongList.stream()
                .filter(w -> w.getLastWrongTime() != null)
                .collect(Collectors.groupingBy(
                        w -> w.getLastWrongTime().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()));

        // 按知识点（资料来源）分布
        Set<Long> materialIds = wrongList.stream()
                .map(UserWrongQuestion::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> materialNameMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(LearningMaterial::getId, LearningMaterial::getOriginalName));

        Map<String, Long> byMaterial = wrongList.stream()
                .filter(w -> w.getMaterialId() != null)
                .collect(Collectors.groupingBy(
                        w -> materialNameMap.getOrDefault(w.getMaterialId(), "未知来源"),
                        Collectors.counting()));

        // 错误次数 Top10 题目
        List<Map<String, Object>> topWrong = wrongList.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getWrongCount() != null ? b.getWrongCount() : 0,
                        a.getWrongCount() != null ? a.getWrongCount() : 0))
                .limit(10)
                .map(w -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("questionId", w.getQuestionId());
                    m.put("wrongCount", w.getWrongCount());
                    m.put("questionType", w.getQuestionType());
                    m.put("materialName", materialNameMap.getOrDefault(w.getMaterialId(), ""));
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", totalCount);
        result.put("masteredCount", masteredCount);
        result.put("unmasteredCount", unmasteredCount);
        result.put("byType", byType);
        result.put("byDate", byDate);
        result.put("byMaterial", byMaterial);
        result.put("topWrong", topWrong);
        return result;
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
     * 填空题判分
     * 先精确匹配，失败则 AI 语义判分
     */
    private Map<String, Object> gradeFillBlank(String question, String referenceAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return Map.of("score", 0.0, "feedback", "未作答");
        }

        // 1. 精确匹配（trim + ignoreCase）
        if (referenceAnswer != null &&
                studentAnswer.trim().equalsIgnoreCase(referenceAnswer.trim())) {
            return Map.of("score", 1.0, "feedback", "完全正确");
        }

        // 2. AI 语义判分（降级）
        return gradeShortAnswer(question, referenceAnswer, studentAnswer);
    }

    /**
     * 多选题判分
     * 解析参考答案和用户答案为集合，全对才得分
     *
     * @param referenceAnswer 参考答案，如 "A,C,D"
     * @param studentAnswer   用户答案，如 "A,C,D"
     * @return 是否全对
     */
    private boolean gradeMultiChoice(String referenceAnswer, String studentAnswer) {
        if (referenceAnswer == null || studentAnswer == null) return false;

        Set<String> refSet = parseChoiceSet(referenceAnswer);
        Set<String> stuSet = parseChoiceSet(studentAnswer);
        return refSet.equals(stuSet);
    }

    /**
     * 解析多选题答案为有序集合（如 "A,C,D" → Set{"A","C","D"}）
     */
    private Set<String> parseChoiceSet(String answer) {
        if (answer == null || answer.isBlank()) return Set.of();
        return Arrays.stream(answer.split("[,，、\\s]+"))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * 数学填空题判分
     * 三级匹配：精确字符串 → 数值解析（容差 0.0001）→ AI 语义判分
     */
    private Map<String, Object> gradeMathFill(String question, String referenceAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return Map.of("score", 0.0, "feedback", "未作答");
        }

        // 1. 精确字符串匹配
        if (referenceAnswer != null &&
                studentAnswer.trim().equalsIgnoreCase(referenceAnswer.trim())) {
            return Map.of("score", 1.0, "feedback", "完全正确");
        }

        // 2. 数值解析 + 容差匹配
        Double refNum = parseNumericValue(referenceAnswer);
        Double stuNum = parseNumericValue(studentAnswer);
        if (refNum != null && stuNum != null) {
            double diff = Math.abs(refNum - stuNum);
            if (diff < 0.0001) {
                return Map.of("score", 1.0, "feedback", "数值正确（容差范围内）");
            }
            return Map.of("score", 0.0, "feedback", "数值不正确，正确答案: " + referenceAnswer);
        }

        // 3. AI 语义判分（降级）
        return gradeShortAnswer(question, referenceAnswer, studentAnswer);
    }

    /**
     * 解析数学表达式为数值
     * 支持：整数、小数、分数（1/3）、百分数（50%）、根号（√2、sqrt(2)）、π、e
     *
     * @param expr 数学表达式
     * @return 数值，解析失败返回 null
     */
    private Double parseNumericValue(String expr) {
        if (expr == null || expr.isBlank()) return null;
        String s = expr.trim().toLowerCase();

        try {
            // 常量替换
            s = s.replace("π", String.valueOf(Math.PI))
                 .replace("pi", String.valueOf(Math.PI))
                 .replace("e", String.valueOf(Math.E));

            // 百分数：50% → 0.5
            if (s.endsWith("%")) {
                String num = s.substring(0, s.length() - 1);
                return Double.parseDouble(num) / 100.0;
            }

            // 分数：1/3
            if (s.contains("/")) {
                String[] parts = s.split("/");
                if (parts.length == 2) {
                    double numerator = Double.parseDouble(parts[0].trim());
                    double denominator = Double.parseDouble(parts[1].trim());
                    if (denominator != 0) return numerator / denominator;
                }
            }

            // 根号：√2 或 sqrt(2)
            if (s.startsWith("√")) {
                String inner = s.substring(1).trim();
                // 处理 √(2) 格式
                if (inner.startsWith("(") && inner.endsWith(")")) {
                    inner = inner.substring(1, inner.length() - 1);
                }
                return Math.sqrt(Double.parseDouble(inner));
            }
            if (s.startsWith("sqrt(") && s.endsWith(")")) {
                String inner = s.substring(5, s.length() - 1);
                return Math.sqrt(Double.parseDouble(inner));
            }

            // 普通数字
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
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

    // ==================== 题库管理 ====================

    /**
     * 题库批次列表（按 batchId 分组，携带名称、题目数等）
     */
    public Map<String, Object> listBatches(int page, int size, String keyword) {
        Long userId = UserContext.getCurrentUserId();

        // 查询用户所有题库记录
        List<AiQuestionBank> allQuestions = questionBankMapper.selectList(
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getUserId, userId)
                        .orderByDesc(AiQuestionBank::getCreateTime));

        // 按 batchId 分组聚合
        Map<String, List<AiQuestionBank>> grouped = allQuestions.stream()
                .collect(Collectors.groupingBy(AiQuestionBank::getBatchId, LinkedHashMap::new, Collectors.toList()));

        // 批量查询关联的资料，避免 N+1
        Set<Long> materialIds = allQuestions.stream()
                .map(AiQuestionBank::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> materialNameMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(LearningMaterial::getId, LearningMaterial::getOriginalName));

        // 过滤关键词
        List<Map<String, Object>> batchList = grouped.values().stream()
                .map(qs -> {
                    String batchName = qs.get(0).getBatchName();
                    if (keyword != null && !keyword.isBlank()
                            && (batchName == null || !batchName.contains(keyword))) {
                        return null;
                    }
                    AiQuestionBank first = qs.get(0);
                    Map<String, Object> bm = new LinkedHashMap<>();
                    bm.put("batchId", first.getBatchId());
                    bm.put("batchName", batchName != null ? batchName : "未命名批次");
                    bm.put("questionCount", qs.size());
                    bm.put("difficulty", first.getDifficulty());
                    bm.put("createTime", first.getCreateTime());
                    // 资料名称（从批量查询的缓存中获取）
                    bm.put("materialName", materialNameMap.getOrDefault(first.getMaterialId(), ""));
                    return bm;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 分页
        int total = batchList.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Map<String, Object>> pageRecords = from < total ? batchList.subList(from, to) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageRecords);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 获取批次下所有题目
     */
    public List<AiQuestionBank> getBatchQuestions(String batchId) {
        Long userId = UserContext.getCurrentUserId();
        List<AiQuestionBank> questions = questionBankMapper.selectList(
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getBatchId, batchId)
                        .eq(AiQuestionBank::getUserId, userId)
                        .orderByAsc(AiQuestionBank::getId));
        if (questions.isEmpty()) {
            throw new BusinessException(404, "批次不存在或不属于当前用户");
        }
        return questions;
    }

    /**
     * 重命名批次
     */
    public void renameBatch(String batchId, String name) {
        Long userId = UserContext.getCurrentUserId();
        verifyBatchOwnership(batchId, userId);
        questionBankMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getBatchId, batchId)
                        .eq(AiQuestionBank::getUserId, userId)
                        .set(AiQuestionBank::getBatchName, name));
    }

    /**
     * 删除整个批次
     */
    public void deleteBatch(String batchId) {
        Long userId = UserContext.getCurrentUserId();
        verifyBatchOwnership(batchId, userId);
        questionBankMapper.delete(
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getBatchId, batchId)
                        .eq(AiQuestionBank::getUserId, userId));
    }

    /**
     * 切换收藏状态
     */
    public Map<String, Object> toggleFavorite(Long questionId) {
        Long userId = UserContext.getCurrentUserId();
        AiQuestionBank q = questionBankMapper.selectById(questionId);
        if (q == null || !q.getUserId().equals(userId)) {
            throw new BusinessException(404, "题目不存在");
        }
        int newValue = q.getIsFavorite() != null && q.getIsFavorite() == 1 ? 0 : 1;
        AiQuestionBank update = new AiQuestionBank();
        update.setId(questionId);
        update.setIsFavorite(newValue);
        questionBankMapper.updateById(update);
        return Map.of("isFavorite", newValue == 1);
    }

    /**
     * 收藏题目列表
     */
    public List<Map<String, Object>> getFavorites(int page, int size) {
        Long userId = UserContext.getCurrentUserId();
        // 限制分页大小，防止恶意请求
        size = Math.min(size, 100);
        Page<AiQuestionBank> pageParam = new Page<>(page, size);
        Page<AiQuestionBank> result = questionBankMapper.selectPage(pageParam,
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getUserId, userId)
                        .eq(AiQuestionBank::getIsFavorite, 1)
                        .orderByDesc(AiQuestionBank::getCreateTime));
        List<AiQuestionBank> list = result.getRecords();
        if (list.isEmpty()) return List.of();

        // 批量查询资料名称，避免 N+1
        Set<Long> matIds = list.stream().map(AiQuestionBank::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> matNameMap = matIds.isEmpty() ? Map.of()
                : materialMapper.selectBatchIds(matIds).stream()
                .collect(Collectors.toMap(LearningMaterial::getId, LearningMaterial::getOriginalName));

        return list.stream().map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("batchId", q.getBatchId());
            m.put("batchName", q.getBatchName());
            m.put("questionType", q.getQuestionType());
            m.put("difficulty", q.getDifficulty());
            m.put("question", q.getQuestion());
            m.put("options", q.getOptions());
            m.put("answer", q.getAnswer());
            m.put("explanation", q.getExplanation());
            m.put("isFavorite", true);
            m.put("materialName", matNameMap.getOrDefault(q.getMaterialId(), ""));
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 单题重新作答并判分
     */
    public Map<String, Object> reAnswer(Long questionId, String userAnswer) {
        Long userId = UserContext.getCurrentUserId();
        AiQuestionBank question = questionBankMapper.selectById(questionId);
        if (question == null || !question.getUserId().equals(userId)) {
            throw new BusinessException(404, "题目不存在");
        }

        boolean isCorrect;
        String feedback = "";
        double score = 0.0;

        String qType = question.getQuestionType();
        if ("choice".equals(qType) || "judge".equals(qType)) {
            isCorrect = userAnswer != null && question.getAnswer() != null &&
                    userAnswer.trim().equalsIgnoreCase(question.getAnswer().trim());
            score = isCorrect ? 1.0 : 0.0;
        } else if ("fill_blank".equals(qType)) {
            Map<String, Object> grading = gradeFillBlank(
                    question.getQuestion(), question.getAnswer(), userAnswer);
            Object scoreObj = grading.get("score");
            score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
            isCorrect = score >= 0.6;
            feedback = (String) grading.getOrDefault("feedback", "");
        } else if ("multi_choice".equals(qType)) {
            isCorrect = gradeMultiChoice(question.getAnswer(), userAnswer);
            score = isCorrect ? 1.0 : 0.0;
        } else if ("math_fill".equals(qType)) {
            Map<String, Object> grading = gradeMathFill(
                    question.getQuestion(), question.getAnswer(), userAnswer);
            Object scoreObj = grading.get("score");
            score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
            isCorrect = score >= 0.6;
            feedback = (String) grading.getOrDefault("feedback", "");
        } else {
            Map<String, Object> grading = gradeShortAnswer(
                    question.getQuestion(), question.getAnswer(), userAnswer);
            Object scoreObj = grading.get("score");
            score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
            isCorrect = score >= 0.6;
            feedback = (String) grading.getOrDefault("feedback", "");
        }

        // 错题本更新（答错加入/更新，答对且之前错过则标记掌握）
        if (!isCorrect) {
            addToWrongBook(userId, question, userAnswer);
        } else {
            markIfWasWrong(userId, questionId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("correctAnswer", question.getAnswer());
        result.put("explanation", question.getExplanation());
        result.put("score", score);
        if (!feedback.isEmpty()) result.put("feedback", feedback);
        return result;
    }

    /**
     * 如果该题在错题本中，标记为已掌握
     */
    private void markIfWasWrong(Long userId, Long questionId) {
        List<UserWrongQuestion> existing = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<UserWrongQuestion>()
                        .eq(UserWrongQuestion::getUserId, userId)
                        .eq(UserWrongQuestion::getQuestionId, questionId)
                        .eq(UserWrongQuestion::getIsMastered, 0));
        for (UserWrongQuestion w : existing) {
            UserWrongQuestion update = new UserWrongQuestion();
            update.setId(w.getId());
            update.setIsMastered(1);
            update.setMasterTime(LocalDateTime.now());
            wrongQuestionMapper.updateById(update);
        }
    }

    /**
     * 校验批次归属
     */
    private void verifyBatchOwnership(String batchId, Long userId) {
        Long count = questionBankMapper.selectCount(
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getBatchId, batchId)
                        .eq(AiQuestionBank::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(404, "批次不存在或不属于当前用户");
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
