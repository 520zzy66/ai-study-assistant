package com.study.controller;

import com.study.common.Result;
import com.study.dto.request.UpdatePlanProgressRequest;
import com.study.service.PlanProgressService;
import com.study.vo.PlanProgressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习计划进度控制器
 */
@Tag(name = "学习计划进度", description = "学习计划进度追踪")
@RestController
@RequestMapping("/ai/plan")
@RequiredArgsConstructor
public class PlanProgressController {

    private final PlanProgressService progressService;

    /**
     * 更新某天的进度
     */
    @Operation(summary = "更新进度", description = "标记某天的学习任务完成状态")
    @PutMapping("/{planId}/day/{dayIndex}")
    public Result<PlanProgressVO> updateProgress(
            @PathVariable Long planId,
            @PathVariable Integer dayIndex,
            @Valid @RequestBody UpdatePlanProgressRequest request) {
        return Result.success(progressService.updateProgress(planId, dayIndex, request));
    }

    /**
     * 获取计划的所有进度
     */
    @Operation(summary = "获取进度列表", description = "获取计划的所有天数进度")
    @GetMapping("/{planId}/progress")
    public Result<List<PlanProgressVO>> getProgressList(@PathVariable Long planId) {
        return Result.success(progressService.getProgressList(planId));
    }

    /**
     * 获取计划进度统计
     */
    @Operation(summary = "获取进度统计", description = "获取计划的完成统计")
    @GetMapping("/{planId}/stats")
    public Result<Map<String, Object>> getProgressStats(@PathVariable Long planId) {
        return Result.success(progressService.getProgressStats(planId));
    }
}
