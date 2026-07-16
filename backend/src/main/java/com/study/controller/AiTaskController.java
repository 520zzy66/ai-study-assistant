package com.study.controller;

import com.study.common.Constants;
import com.study.common.Result;
import com.study.common.UserContext;
import com.study.dto.request.GeneratePlanRequest;
import com.study.dto.request.GenerateQuizRequest;
import com.study.dto.request.GenerateSummaryRequest;
import com.study.entity.AiTask;
import com.study.service.AiTaskService;
import com.study.vo.AiTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 异步任务控制器
 * 将耗时的 AI 生成操作放入后台执行，前端轮询任务状态
 */
@Tag(name = "AI 异步任务", description = "AI 生成操作的异步任务管理")
@RestController
@RequestMapping("/ai/task")
@RequiredArgsConstructor
public class AiTaskController {

    private final AiTaskService taskService;

    /**
     * 异步生成练习题
     */
    @Operation(summary = "异步生成练习题", description = "创建后台出题任务，返回 taskId 供轮询")
    @PostMapping("/quiz/{materialId}")
    public Result<Map<String, String>> generateQuizAsync(
            @PathVariable Long materialId,
            @Valid @RequestBody GenerateQuizRequest request) {
        Long userId = UserContext.getCurrentUserId();
        AiTask task = taskService.createTask(Constants.CHAT_TYPE_QUIZ, materialId, userId);
        taskService.executeQuizTask(task.getTaskId(), materialId, request, userId);
        return Result.success(Map.of("taskId", task.getTaskId()));
    }

    /**
     * 异步生成文件夹练习题（混合出题）
     */
    @Operation(summary = "异步生成文件夹练习题", description = "创建后台文件夹出题任务，从文件夹所有资料中混合出题")
    @PostMapping("/quiz/folder/{folderId}")
    public Result<Map<String, String>> generateFolderQuizAsync(
            @PathVariable Long folderId,
            @Valid @RequestBody GenerateQuizRequest request) {
        Long userId = UserContext.getCurrentUserId();
        AiTask task = taskService.createTask(Constants.CHAT_TYPE_QUIZ, folderId, userId);
        taskService.executeFolderQuizTask(task.getTaskId(), folderId, request, userId);
        return Result.success(Map.of("taskId", task.getTaskId()));
    }

    /**
     * 异步生成文档总结
     */
    @Operation(summary = "异步生成总结", description = "创建后台总结任务，返回 taskId 供轮询")
    @PostMapping("/summary/{materialId}")
    public Result<Map<String, String>> generateSummaryAsync(
            @PathVariable Long materialId,
            @RequestBody(required = false) GenerateSummaryRequest request) {
        Long userId = UserContext.getCurrentUserId();
        boolean force = request != null && Boolean.TRUE.equals(request.getForce());
        AiTask task = taskService.createTask(Constants.CHAT_TYPE_SUMMARY, materialId, userId);
        taskService.executeSummaryTask(task.getTaskId(), materialId, force, userId);
        return Result.success(Map.of("taskId", task.getTaskId()));
    }

    /**
     * 异步生成文件夹总结
     */
    @Operation(summary = "异步生成文件夹总结", description = "创建后台文件夹总结任务，从文件夹所有资料中生成综合总结")
    @PostMapping("/summary/folder/{folderId}")
    public Result<Map<String, String>> generateFolderSummaryAsync(
            @PathVariable Long folderId,
            @RequestBody(required = false) GenerateSummaryRequest request) {
        Long userId = UserContext.getCurrentUserId();
        boolean force = request != null && Boolean.TRUE.equals(request.getForce());
        AiTask task = taskService.createTask(Constants.CHAT_TYPE_SUMMARY, folderId, userId);
        taskService.executeFolderSummaryTask(task.getTaskId(), folderId, force, userId);
        return Result.success(Map.of("taskId", task.getTaskId()));
    }

    /**
     * 异步生成学习计划
     */
    @Operation(summary = "异步生成学习计划", description = "创建后台计划生成任务，返回 taskId 供轮询")
    @PostMapping("/plan")
    public Result<Map<String, String>> generatePlanAsync(
            @Valid @RequestBody GeneratePlanRequest request) {
        Long userId = UserContext.getCurrentUserId();
        AiTask task = taskService.createTask(Constants.CHAT_TYPE_PLAN, null, userId);
        int dailyHours = request.getDailyHours() != null ? request.getDailyHours() : 2;
        taskService.executePlanTask(task.getTaskId(), request.getGoal(),
                request.getExamDate(), dailyHours, request.getMaterialIds(), userId);
        return Result.success(Map.of("taskId", task.getTaskId()));
    }

    /**
     * 查询单个任务
     */
    @Operation(summary = "查询任务状态", description = "根据 taskId 查询任务进度和结果")
    @GetMapping("/{taskId}")
    public Result<AiTaskVO> getTask(@PathVariable String taskId) {
        AiTask task = taskService.getTask(taskId);
        return Result.success(toVO(task));
    }

    /**
     * 获取活跃任务列表
     */
    @Operation(summary = "活跃任务列表", description = "获取当前用户待处理/运行中的任务")
    @GetMapping
    public Result<List<AiTaskVO>> getActiveTasks() {
        List<AiTask> tasks = taskService.getActiveTasks();
        return Result.success(tasks.stream().map(this::toVO).collect(Collectors.toList()));
    }

    /**
     * 取消任务
     */
    @Operation(summary = "取消任务", description = "取消正在执行的任务")
    @DeleteMapping("/{taskId}")
    public Result<Void> cancelTask(@PathVariable String taskId) {
        taskService.cancelTask(taskId);
        return Result.successMsg("任务已取消");
    }

    /**
     * Entity -> VO 转换
     */
    private AiTaskVO toVO(AiTask task) {
        AiTaskVO vo = new AiTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setMessage(task.getMessage());
        vo.setResult(task.getResult());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }
}
