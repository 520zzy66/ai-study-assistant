package com.study.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.ai.client.AiClient;
import com.study.ai.prompt.PromptTemplates;
import com.study.common.BusinessException;
import com.study.common.Constants;
import com.study.common.JsonUtils;
import com.study.common.UserContext;
import static com.study.common.JsonUtils.toJson;
import com.study.entity.AiChatHistory;
import com.study.entity.LearningMaterial;
import com.study.entity.StudyPlan;
import com.study.mapper.AiChatHistoryMapper;
import com.study.mapper.LearningMaterialMapper;
import com.study.mapper.StudyPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 学习计划服务
 * 根据学习目标、考试日期、每日时间生成个性化学习计划
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPlanService {

    private final AiClient aiClient;
    private final StudyPlanMapper studyPlanMapper;
    private final LearningMaterialMapper materialMapper;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 生成学习计划
     *
     * @param goal        学习目标
     * @param examDate    考试日期
     * @param dailyHours  每日学习时长
     * @param materialIds 关联资料ID列表
     * @return 学习计划 Map（含 id、plan 详情等）
     */
    public Map<String, Object> generatePlan(String goal, String examDate, int dailyHours,
                                            List<Long> materialIds) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验考试日期
        LocalDate exam = LocalDate.parse(examDate, DATE_FMT);
        LocalDate today = LocalDate.now();
        if (exam.isBefore(today) || exam.isEqual(today)) {
            throw new BusinessException(400, "考试日期必须晚于今天");
        }

        long totalDays = ChronoUnit.DAYS.between(today, exam);
        if (totalDays > 90) {
            log.warn("学习周期超过90天({}), 建议拆分学习计划", totalDays);
        }

        // 2. 获取关联资料名称（SQL 层面过滤 userId，避免查询其他用户资料）
        String materialNames = "无";
        if (materialIds != null && !materialIds.isEmpty()) {
            materialNames = materialMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningMaterial>()
                            .in(LearningMaterial::getId, materialIds)
                            .eq(LearningMaterial::getUserId, userId)
                            .select(LearningMaterial::getOriginalName)
            ).stream()
                    .map(LearningMaterial::getOriginalName)
                    .collect(Collectors.joining("、"));
            if (materialNames.isEmpty()) materialNames = "无";
        }

        // 3. 构建 Prompt 并调用 AI
        String prompt = PromptTemplates.buildPlanPrompt(
                goal, examDate, today.format(DATE_FMT), dailyHours, materialNames);
        String aiResponse = aiClient.chat(prompt);

        // 4. 解析 AI 返回的 JSON
        List<Map<String, Object>> planItems = parsePlanResponse(aiResponse);

        // 5. 构建计划标题
        String title = "学习计划 - " + goal;
        if (title.length() > 200) title = title.substring(0, 197) + "...";

        // 6. 保存到数据库
        String materialIdsStr = materialIds != null
                ? materialIds.stream().map(String::valueOf).collect(Collectors.joining(","))
                : null;

        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setTitle(title);
        plan.setGoal(goal);
        plan.setExamDate(exam);
        plan.setDailyHours(dailyHours);
        plan.setTotalDays((int) totalDays);
        plan.setPlanContent(toJson(planItems, "[]"));
        plan.setMaterialIds(materialIdsStr);
        studyPlanMapper.insert(plan);

        // 7. 保存对话历史
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setChatType(Constants.CHAT_TYPE_PLAN);
        history.setUserMessage("生成学习计划：" + goal);
        history.setAiResponse(aiResponse);
        chatHistoryMapper.insert(history);

        // 8. 构建返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", plan.getId());
        result.put("title", title);
        result.put("goal", goal);
        result.put("examDate", examDate);
        result.put("dailyHours", dailyHours);
        result.put("totalDays", (int) totalDays);
        result.put("plan", planItems);
        return result;
    }

    /**
     * 获取用户的学习计划列表
     */
    public List<StudyPlan> listPlans() {
        Long userId = UserContext.getCurrentUserId();
        return studyPlanMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .orderByDesc(StudyPlan::getCreateTime)
        );
    }

    /**
     * 获取学习计划详情
     */
    public StudyPlan getPlanDetail(Long id) {
        Long userId = UserContext.getCurrentUserId();
        StudyPlan plan = studyPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException(404, "学习计划不存在");
        }
        return plan;
    }

    /**
     * 解析 AI 返回的学习计划 JSON
     */
    private List<Map<String, Object>> parsePlanResponse(String aiResponse) {
        try {
            String json = JsonUtils.cleanJsonResponse(aiResponse);

            Map<String, Object> map = objectMapper.readValue(json,
                    new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> planList = (List<Map<String, Object>>) map.get("plan");

            if (planList == null || planList.isEmpty()) {
                log.warn("AI 返回的 plan 为空");
                return List.of();
            }
            return planList;

        } catch (Exception e) {
            log.error("解析 AI 返回的计划 JSON 失败", e);
            throw new BusinessException(3005, "AI 返回格式异常，请重试");
        }
    }
}
