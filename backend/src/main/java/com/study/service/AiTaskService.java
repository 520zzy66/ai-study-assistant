package com.study.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.ai.service.AiPlanService;
import com.study.ai.service.AiQuizService;
import com.study.ai.service.AiSummaryService;
import com.study.ai.service.ResourcePackageService;
import com.study.common.BusinessException;
import com.study.common.Constants;
import com.study.common.JsonUtils;
import com.study.common.UserContext;
import com.study.dto.request.GenerateQuizRequest;
import com.study.dto.request.GenerateResourcePackageRequest;
import com.study.entity.AiTask;
import com.study.mapper.AiTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 异步任务服务
 * 将耗时的 AI 生成操作（出题/总结/计划/资源包）放入后台线程执行，
 * 前端通过 taskId 轮询任务状态和结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskService {

    private final AiTaskMapper taskMapper;
    private final AiQuizService quizService;
    private final AiSummaryService summaryService;
    private final AiPlanService planService;
    private final ResourcePackageService resourcePackageService;

    /**
     * 创建异步任务
     */
    public AiTask createTask(String type, Long refId, Long userId) {
        AiTask task = new AiTask();
        task.setTaskId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        task.setUserId(userId);
        task.setType(type);
        task.setRefId(refId);
        task.setStatus(Constants.TASK_STATUS_PENDING);
        task.setProgress(0);
        task.setMessage("等待执行...");
        taskMapper.insert(task);
        return task;
    }

    /**
     * 更新任务状态
     */
    private void updateTask(String taskId, String status, int progress, String message) {
        taskMapper.update(null, new LambdaUpdateWrapper<AiTask>()
                .eq(AiTask::getTaskId, taskId)
                .set(AiTask::getStatus, status)
                .set(AiTask::getProgress, progress)
                .set(AiTask::getMessage, message)
                .set(AiTask::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 设置任务结果（成功）
     */
    private void completeTask(String taskId, Object result) {
        AiTask updateEntity = new AiTask();
        updateEntity.setStatus(Constants.TASK_STATUS_SUCCESS);
        updateEntity.setProgress(100);
        updateEntity.setMessage("完成");
        updateEntity.setResult(JsonUtils.toJson(result));
        updateEntity.setUpdateTime(LocalDateTime.now());

        taskMapper.update(updateEntity, new LambdaUpdateWrapper<AiTask>()
                .eq(AiTask::getTaskId, taskId));
    }

    /**
     * 设置任务失败
     * 仅将 BusinessException 的消息暴露给前端，系统异常返回通用提示
     */
    private void failTask(String taskId, Throwable e) {
        String userMsg = (e instanceof BusinessException)
                ? e.getMessage()
                : "系统繁忙，请稍后再试";
        taskMapper.update(null, new LambdaUpdateWrapper<AiTask>()
                .eq(AiTask::getTaskId, taskId)
                .set(AiTask::getStatus, Constants.TASK_STATUS_FAILED)
                .set(AiTask::getErrorMsg, userMsg)
                .set(AiTask::getMessage, "失败")
                .set(AiTask::getUpdateTime, LocalDateTime.now()));
    }

    // ==================== 异步执行入口 ====================

    /**
     * 异步生成练习题
     */
    @Async("taskExecutor")
    public void executeQuizTask(String taskId, Long materialId, GenerateQuizRequest request, Long userId) {
        UserContext.setCurrentUser(new UserContext.UserInfo(userId, "system"));
        try {
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 10, "正在读取资料内容...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 30, "AI 正在分析知识点...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 60, "AI 正在生成题目...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 90, "正在整理结果...");
            if (isCancelled(taskId)) return;
            Map<String, Object> result = quizService.generateQuiz(materialId, request);
            if (isCancelled(taskId)) return;
            completeTask(taskId, result);
            log.info("异步出题完成: taskId={}, materialId={}", taskId, materialId);
        } catch (Exception e) {
            log.error("异步出题失败: taskId={}", taskId, e);
            failTask(taskId, e);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 异步生成文件夹练习题（混合出题）
     * 从向量数据库读取该文件夹下所有资料的切片内容，由AI混合出题
     */
    @Async("taskExecutor")
    public void executeFolderQuizTask(String taskId, Long folderId, GenerateQuizRequest request, Long userId) {
        UserContext.setCurrentUser(new UserContext.UserInfo(userId, "system"));
        try {
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 10, "正在读取文件夹资料...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 30, "正在从向量数据库检索内容...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 50, "AI 正在分析知识点...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 70, "AI 正在生成混合题目...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 90, "正在整理结果...");
            if (isCancelled(taskId)) return;
            Map<String, Object> result = quizService.generateFolderQuiz(folderId, request);
            if (isCancelled(taskId)) return;
            completeTask(taskId, result);
            log.info("异步文件夹出题完成: taskId={}, folderId={}", taskId, folderId);
        } catch (Exception e) {
            log.error("异步文件夹出题失败: taskId={}", taskId, e);
            failTask(taskId, e);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 异步生成文档总结
     */
    @Async("taskExecutor")
    public void executeSummaryTask(String taskId, Long materialId, boolean force, Long userId) {
        UserContext.setCurrentUser(new UserContext.UserInfo(userId, "system"));
        try {
            if (force) {
                updateTask(taskId, Constants.TASK_STATUS_RUNNING, 15, "正在读取资料内容...");
                if (isCancelled(taskId)) return;
                updateTask(taskId, Constants.TASK_STATUS_RUNNING, 40, "AI 正在生成总结...");
                if (isCancelled(taskId)) return;
                updateTask(taskId, Constants.TASK_STATUS_RUNNING, 70, "正在优化排版...");
            } else {
                updateTask(taskId, Constants.TASK_STATUS_RUNNING, 20, "正在检查缓存...");
                if (isCancelled(taskId)) return;
                updateTask(taskId, Constants.TASK_STATUS_RUNNING, 50, "正在读取资料内容...");
            }
            if (isCancelled(taskId)) return;
            String summary = summaryService.generateSummary(materialId, force);
            if (isCancelled(taskId)) return;
            completeTask(taskId, Map.of("materialId", materialId, "summary", summary));
            log.info("异步总结完成: taskId={}, materialId={}, force={}", taskId, materialId, force);
        } catch (Exception e) {
            log.error("异步总结失败: taskId={}", taskId, e);
            failTask(taskId, e);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 异步生成文件夹总结
     * 从向量数据库读取该文件夹下所有资料的切片内容，由AI做综合总结
     */
    @Async("taskExecutor")
    public void executeFolderSummaryTask(String taskId, Long folderId, boolean force, Long userId) {
        UserContext.setCurrentUser(new UserContext.UserInfo(userId, "system"));
        try {
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 10, "正在读取文件夹资料...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 30, "正在从向量数据库检索内容...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 50, "AI 正在生成综合总结...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 80, "正在优化排版...");
            if (isCancelled(taskId)) return;
            String summary = summaryService.generateFolderSummary(folderId, force);
            if (isCancelled(taskId)) return;
            completeTask(taskId, Map.of("folderId", folderId, "summary", summary));
            log.info("异步文件夹总结完成: taskId={}, folderId={}", taskId, folderId);
        } catch (Exception e) {
            log.error("异步文件夹总结失败: taskId={}", taskId, e);
            failTask(taskId, e);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 异步生成学习计划
     */
    @Async("taskExecutor")
    public void executePlanTask(String taskId, String goal, String examDate,
                                 int dailyHours, List<Long> materialIds, Long userId) {
        UserContext.setCurrentUser(new UserContext.UserInfo(userId, "system"));
        try {
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 10, "正在分析学习目标...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 30, "AI 正在规划学习路径...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 60, "AI 正在生成每日任务...");
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 85, "正在整理计划...");
            if (isCancelled(taskId)) return;
            Map<String, Object> result = planService.generatePlan(goal, examDate, dailyHours, materialIds);
            if (isCancelled(taskId)) return;
            completeTask(taskId, result);
            log.info("异步计划生成完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("异步计划生成失败: taskId={}", taskId, e);
            failTask(taskId, e);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 异步生成个性化资源包。
     */
    @Async("taskExecutor")
    public void executeResourcePackageTask(String taskId, GenerateResourcePackageRequest request, Long userId) {
        UserContext.setCurrentUser(new UserContext.UserInfo(userId, "system"));
        try {
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 8, "正在启动资源包多智能体编排...");
            if (isCancelled(taskId)) {
                throw new java.util.concurrent.CancellationException("任务取消");
            }
            Map<String, Object> result = resourcePackageService.generatePackage(request, taskId, event -> {
                if (isCancelled(taskId)) {
                    throw new java.util.concurrent.CancellationException("任务取消");
                }
                updateTask(taskId, Constants.TASK_STATUS_RUNNING, event.progress(), event.message());
            });
            if (isCancelled(taskId)) return;
            updateTask(taskId, Constants.TASK_STATUS_RUNNING, 98, "正在保存资源包结果...");
            completeTask(taskId, result);
            log.info("异步资源包生成完成: taskId={}, materialId={}", taskId, request.getMaterialId());
        } catch (java.util.concurrent.CancellationException e) {
            log.info("异步资源包任务已被用户取消: taskId={}", taskId);
            markCancelled(taskId);
        } catch (Exception e) {
            log.error("异步资源包生成失败: taskId={}", taskId, e);
            failTask(taskId, e);
        } finally {
            UserContext.clear();
        }
    }

    // ==================== 查询接口 ====================

    /**
     * 根据 taskId 查询任务（校验用户归属）
     */
    public AiTask getTask(String taskId) {
        Long userId = UserContext.getCurrentUserId();
        AiTask task = getByTaskId(taskId);
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该任务");
        }
        return task;
    }

    /**
     * 获取用户待处理/运行中的任务列表
     */
    public List<AiTask> getActiveTasks() {
        Long userId = UserContext.getCurrentUserId();
        return taskMapper.selectList(
                new LambdaQueryWrapper<AiTask>()
                        .eq(AiTask::getUserId, userId)
                        .in(AiTask::getStatus, Constants.TASK_STATUS_PENDING, Constants.TASK_STATUS_RUNNING)
                        .orderByDesc(AiTask::getCreateTime)
        );
    }

    /**
     * 按任务类型查询当前用户的最近任务。
     *
     * @param type 任务类型
     * @param size 返回数量
     * @return 最近任务列表
     */
    public List<AiTask> getRecentTasksByType(String type, int size) {
        Long userId = UserContext.getCurrentUserId();
        int limit = Math.max(1, Math.min(size, 50));
        return taskMapper.selectList(
                new LambdaQueryWrapper<AiTask>()
                        .eq(AiTask::getUserId, userId)
                        .eq(AiTask::getType, type)
                        .orderByDesc(AiTask::getCreateTime)
                        .last("LIMIT " + limit)
        );
    }

    /**
     * 请求取消任务
     *
     * @param taskId 任务ID
     */
    public void cancelTask(String taskId) {
        Long userId = UserContext.getCurrentUserId();
        AiTask task = getByTaskId(taskId);
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该任务");
        }

        // 只能取消 pending 或 running 状态的任务
        if (!Constants.TASK_STATUS_PENDING.equals(task.getStatus())
                && !Constants.TASK_STATUS_RUNNING.equals(task.getStatus())) {
            throw new BusinessException(400, "任务已完成或失败，无法取消");
        }

        // 设置取消标记
        taskMapper.update(null, new LambdaUpdateWrapper<AiTask>()
                .eq(AiTask::getTaskId, taskId)
                .set(AiTask::getCancelRequested, 1)
                .set(AiTask::getStatus, Constants.TASK_STATUS_CANCELLED)
                .set(AiTask::getMessage, "用户取消")
                .set(AiTask::getErrorMsg, "任务已被用户取消")
                .set(AiTask::getUpdateTime, LocalDateTime.now()));

        log.info("任务已取消: taskId={}", taskId);
    }

    /**
     * 检查任务是否被请求取消
     *
     * @param taskId 任务ID
     * @return 是否被取消
     */
    public boolean isCancelled(String taskId) {
        AiTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<AiTask>()
                        .eq(AiTask::getTaskId, taskId)
                        .select(AiTask::getCancelRequested));
        return task != null && Integer.valueOf(1).equals(task.getCancelRequested());
    }

    private void markCancelled(String taskId) {
        taskMapper.update(null, new LambdaUpdateWrapper<AiTask>()
                .eq(AiTask::getTaskId, taskId)
                .ne(AiTask::getStatus, Constants.TASK_STATUS_SUCCESS)
                .set(AiTask::getStatus, Constants.TASK_STATUS_CANCELLED)
                .set(AiTask::getMessage, "用户取消")
                .set(AiTask::getErrorMsg, "任务已被用户取消")
                .set(AiTask::getUpdateTime, LocalDateTime.now()));
    }

    private AiTask getByTaskId(String taskId) {
        AiTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<AiTask>().eq(AiTask::getTaskId, taskId));
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }
}
