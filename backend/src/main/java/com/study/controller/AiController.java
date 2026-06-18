package com.study.controller;

import com.study.ai.service.*;
import com.study.common.Result;
import com.study.dto.request.*;
import com.study.entity.StudyPlan;
import com.study.vo.SummaryVO;
import com.study.vo.WrongQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI 功能控制器
 * 统一管理 AI 总结、问答、出题、学习计划等接口
 */
@Tag(name = "AI 功能", description = "AI 知识总结、RAG 问答、自动出题、学习计划")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiSummaryService summaryService;
    private final AiQaService qaService;
    private final AiQuizService quizService;
    private final AiPlanService planService;

    // ==================== Spec-04: AI 文档总结 ====================

    /**
     * 生成文档总结
     */
    @Operation(summary = "生成文档总结", description = "根据学习资料内容生成 Markdown 格式知识点总结")
    @PostMapping("/summary/{materialId}")
    public Result<SummaryVO> generateSummary(
            @PathVariable Long materialId,
            @RequestBody(required = false) GenerateSummaryRequest request) {

        boolean force = request != null && Boolean.TRUE.equals(request.getForce());
        String summary = summaryService.generateSummary(materialId, force);

        return Result.success(new SummaryVO(materialId, summary));
    }

    // ==================== Spec-05: RAG 文档问答 ====================

    /**
     * RAG 同步问答
     */
    @Operation(summary = "文档问答（同步）", description = "基于 RAG 检索增强生成回答用户问题")
    @PostMapping("/qa")
    public Result<Map<String, Object>> askQuestion(@Valid @RequestBody QaRequest request) {
        Map<String, Object> result = qaService.ask(request.getMaterialId(), request.getQuestion());
        return Result.success(result);
    }

    /**
     * RAG 流式问答（SSE）
     */
    @Operation(summary = "文档问答（流式）", description = "基于 RAG 的流式问答，SSE 逐字返回")
    @PostMapping(value = "/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askQuestionStream(@Valid @RequestBody QaRequest request) {
        return qaService.askStream(request.getMaterialId(), request.getQuestion());
    }

    // ==================== Spec-06: AI 自动出题 ====================

    /**
     * 生成练习题
     */
    @Operation(summary = "生成练习题", description = "根据学习资料自动生成选择题、判断题、简答题")
    @PostMapping("/quiz/{materialId}")
    public Result<Map<String, Object>> generateQuiz(
            @PathVariable Long materialId,
            @Valid @RequestBody GenerateQuizRequest request) {

        Map<String, Object> result = quizService.generateQuiz(materialId, request);
        return Result.success(result);
    }

    /**
     * 提交答案并判分
     */
    @Operation(summary = "提交答案", description = "提交练习题答案并自动判分")
    @PostMapping("/quiz/{batchId}/answer")
    public Result<Map<String, Object>> submitAnswers(
            @PathVariable String batchId,
            @Valid @RequestBody SubmitAnswerRequest request) {

        Map<String, Object> result = quizService.submitAnswers(batchId, request);
        return Result.success(result);
    }

    /**
     * 获取错题列表
     */
    @Operation(summary = "错题本列表", description = "获取当前用户的错题记录")
    @GetMapping("/quiz/wrong")
    public Result<List<WrongQuestionVO>> getWrongQuestions(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Boolean isMastered) {

        List<WrongQuestionVO> list = quizService.getWrongQuestions(materialId, isMastered);
        return Result.success(list);
    }

    /**
     * 标记错题已掌握
     */
    @Operation(summary = "标记错题已掌握", description = "将错题标记为已掌握状态")
    @PutMapping("/quiz/wrong/{id}/master")
    public Result<Void> markMastered(@PathVariable Long id) {
        quizService.markWrongQuestionMastered(id);
        return Result.successMsg("已标记为掌握");
    }

    // ==================== Spec-07: AI 学习计划 ====================

    /**
     * 生成学习计划
     */
    @Operation(summary = "生成学习计划", description = "根据学习目标、考试日期、每日时长生成个性化学习计划")
    @PostMapping("/plan")
    public Result<Map<String, Object>> generatePlan(@Valid @RequestBody GeneratePlanRequest request) {
        Map<String, Object> result = planService.generatePlan(
                request.getGoal(),
                request.getExamDate(),
                request.getDailyHours() != null ? request.getDailyHours() : 2,
                request.getMaterialIds()
        );
        return Result.success(result);
    }

    /**
     * 获取学习计划列表
     */
    @Operation(summary = "学习计划列表", description = "获取当前用户的所有学习计划")
    @GetMapping("/plan")
    public Result<List<StudyPlan>> listPlans() {
        List<StudyPlan> list = planService.listPlans();
        return Result.success(list);
    }

    /**
     * 获取学习计划详情
     */
    @Operation(summary = "学习计划详情", description = "查看指定学习计划的完整内容")
    @GetMapping("/plan/{id}")
    public Result<StudyPlan> getPlanDetail(@PathVariable Long id) {
        StudyPlan plan = planService.getPlanDetail(id);
        return Result.success(plan);
    }
}
