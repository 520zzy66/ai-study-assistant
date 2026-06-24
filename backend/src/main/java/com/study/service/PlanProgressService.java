package com.study.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.common.BusinessException;
import com.study.common.UserContext;
import com.study.dto.request.UpdatePlanProgressRequest;
import com.study.entity.StudyPlan;
import com.study.entity.StudyPlanProgress;
import com.study.mapper.StudyPlanMapper;
import com.study.mapper.StudyPlanProgressMapper;
import com.study.vo.PlanProgressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习计划进度服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanProgressService {

    private final StudyPlanProgressMapper progressMapper;
    private final StudyPlanMapper planMapper;

    /**
     * 更新某天的进度
     *
     * @param planId  计划ID
     * @param dayIndex 第几天
     * @param request 更新请求
     * @return 更新后的进度
     */
    @Transactional
    public PlanProgressVO updateProgress(Long planId, Integer dayIndex, UpdatePlanProgressRequest request) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 验证计划归属
        StudyPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException(404, "学习计划不存在");
        }

        // 2. 验证 dayIndex 范围
        if (dayIndex < 1 || (plan.getTotalDays() != null && dayIndex > plan.getTotalDays())) {
            throw new BusinessException(400, "天数超出计划范围");
        }

        // 3. 查询或创建进度记录
        StudyPlanProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<StudyPlanProgress>()
                        .eq(StudyPlanProgress::getPlanId, planId)
                        .eq(StudyPlanProgress::getDayIndex, dayIndex)
        );

        if (progress == null) {
            progress = new StudyPlanProgress();
            progress.setPlanId(planId);
            progress.setUserId(userId);
            progress.setDayIndex(dayIndex);
            progress.setCompleted(0);
            progress.setActualHours(BigDecimal.ZERO);
            progressMapper.insert(progress);
        }

        // 4. 更新字段
        if (request.getCompleted() != null) {
            progress.setCompleted(request.getCompleted() ? 1 : 0);
            if (request.getCompleted()) {
                progress.setCompleteTime(LocalDateTime.now());
            } else {
                progress.setCompleteTime(null);
            }
        }
        if (request.getActualHours() != null) {
            progress.setActualHours(request.getActualHours());
        }
        if (request.getNote() != null) {
            progress.setNote(request.getNote());
        }

        progressMapper.updateById(progress);

        // 5. 构建返回
        return buildVO(progress, plan);
    }

    /**
     * 获取计划的所有进度
     *
     * @param planId 计划ID
     * @return 进度列表
     */
    public List<PlanProgressVO> getProgressList(Long planId) {
        Long userId = UserContext.getCurrentUserId();

        // 验证计划归属
        StudyPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException(404, "学习计划不存在");
        }

        List<StudyPlanProgress> progressList = progressMapper.selectList(
                new LambdaQueryWrapper<StudyPlanProgress>()
                        .eq(StudyPlanProgress::getPlanId, planId)
                        .orderByAsc(StudyPlanProgress::getDayIndex)
        );

        return progressList.stream()
                .map(p -> buildVO(p, plan))
                .collect(Collectors.toList());
    }

    /**
     * 获取计划进度统计
     *
     * @param planId 计划ID
     * @return 统计信息
     */
    public Map<String, Object> getProgressStats(Long planId) {
        Long userId = UserContext.getCurrentUserId();

        // 验证计划归属
        StudyPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException(404, "学习计划不存在");
        }

        // 查询已完成天数
        Long completedDays = progressMapper.selectCount(
                new LambdaQueryWrapper<StudyPlanProgress>()
                        .eq(StudyPlanProgress::getPlanId, planId)
                        .eq(StudyPlanProgress::getCompleted, 1)
        );

        // 查询总实际学习时长
        List<StudyPlanProgress> allProgress = progressMapper.selectList(
                new LambdaQueryWrapper<StudyPlanProgress>()
                        .eq(StudyPlanProgress::getPlanId, planId)
        );

        BigDecimal totalHours = allProgress.stream()
                .map(StudyPlanProgress::getActualHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalDays = plan.getTotalDays() != null ? plan.getTotalDays() : 0;
        int progressPercent = totalDays > 0 ? (int) (completedDays * 100 / totalDays) : 0;

        return Map.of(
                "planId", planId,
                "totalDays", totalDays,
                "completedDays", completedDays,
                "progressPercent", progressPercent,
                "totalActualHours", totalHours
        );
    }

    /**
     * 构建 VO
     */
    private PlanProgressVO buildVO(StudyPlanProgress progress, StudyPlan plan) {
        PlanProgressVO vo = new PlanProgressVO();
        vo.setPlanId(progress.getPlanId());
        vo.setDayIndex(progress.getDayIndex());
        vo.setCompleted(progress.getCompleted() == 1);
        vo.setActualHours(progress.getActualHours());
        vo.setNote(progress.getNote());
        vo.setCompleteTime(progress.getCompleteTime());
        vo.setTotalDays(plan.getTotalDays());

        // 计算统计数据
        Long completedDays = progressMapper.selectCount(
                new LambdaQueryWrapper<StudyPlanProgress>()
                        .eq(StudyPlanProgress::getPlanId, progress.getPlanId())
                        .eq(StudyPlanProgress::getCompleted, 1)
        );
        vo.setCompletedDays(completedDays);

        int totalDays = plan.getTotalDays() != null ? plan.getTotalDays() : 0;
        vo.setProgressPercent(totalDays > 0 ? (int) (completedDays * 100 / totalDays) : 0);

        return vo;
    }
}
