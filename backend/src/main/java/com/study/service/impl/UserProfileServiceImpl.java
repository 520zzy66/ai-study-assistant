package com.study.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.dto.request.UpdateUserProfileRequest;
import com.study.entity.UserProfile;
import com.study.mapper.UserProfileMapper;
import com.study.service.UserProfileService;
import com.study.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * 用户画像服务实现
 *
 * <p>负责用户画像的查询、创建和更新。
 * 首次访问时自动创建默认画像记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileVO getProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));

        if (profile == null) {
            // 无记录时返回默认值，不抛异常
            return defaultProfile(userId);
        }

        return toVO(profile);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));

        if (profile == null) {
            // 首次设置：创建新记录
            profile = new UserProfile();
            profile.setUserId(userId);
            applyUpdate(profile, request);
            userProfileMapper.insert(profile);
            log.info("创建用户画像: userId={}", userId);
        } else {
            // 已有记录：部分更新
            applyUpdate(profile, request);
            userProfileMapper.updateById(profile);
            log.info("更新用户画像: userId={}", userId);
        }
    }

    @Override
    public UserProfileVO getOrCreateProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));

        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setPreferredDifficulty("normal");
            profile.setDailyStudyGoalMinutes(60);
            profile.setOverallLevel(50);
            profile.setWeakPoints(Collections.emptyList());
            profile.setStrongPoints(Collections.emptyList());
            profile.setTotalStudyDays(0);
            profile.setTotalQuestions(0);
            profile.setAccuracyRate(BigDecimal.ZERO);
            userProfileMapper.insert(profile);
            log.info("自动创建用户画像: userId={}", userId);
        }

        return toVO(profile);
    }

    /**
     * 将请求中的非空字段应用到实体
     */
    private void applyUpdate(UserProfile profile, UpdateUserProfileRequest request) {
        if (request.getAge() != null) {
            profile.setAge(request.getAge());
        }
        if (StrUtil.isNotBlank(request.getEducation())) {
            profile.setEducation(request.getEducation());
        }
        if (StrUtil.isNotBlank(request.getTargetExam())) {
            profile.setTargetExam(request.getTargetExam());
        }
        if (StrUtil.isNotBlank(request.getStudySubject())) {
            profile.setStudySubject(request.getStudySubject());
        }
        if (request.getDailyStudyGoalMinutes() != null) {
            profile.setDailyStudyGoalMinutes(request.getDailyStudyGoalMinutes());
        }
        if (StrUtil.isNotBlank(request.getPreferredDifficulty())) {
            profile.setPreferredDifficulty(request.getPreferredDifficulty());
        }
        if (StrUtil.isNotBlank(request.getLearningStyle())) {
            profile.setLearningStyle(request.getLearningStyle());
        }
        if (request.getWeakPoints() != null) {
            profile.setWeakPoints(request.getWeakPoints());
        }
        if (request.getStrongPoints() != null) {
            profile.setStrongPoints(request.getStrongPoints());
        }
    }

    /**
     * 构造默认画像 VO（无记录时使用）
     */
    private UserProfileVO defaultProfile(Long userId) {
        return UserProfileVO.builder()
                .userId(userId)
                .preferredDifficulty("normal")
                .dailyStudyGoalMinutes(60)
                .overallLevel(50)
                .weakPoints(Collections.emptyList())
                .strongPoints(Collections.emptyList())
                .totalStudyDays(0)
                .totalQuestions(0)
                .accuracyRate(BigDecimal.ZERO)
                .build();
    }

    /**
     * 实体转 VO
     */
    private UserProfileVO toVO(UserProfile profile) {
        return UserProfileVO.builder()
                .userId(profile.getUserId())
                .age(profile.getAge())
                .education(profile.getEducation())
                .targetExam(profile.getTargetExam())
                .studySubject(profile.getStudySubject())
                .dailyStudyGoalMinutes(profile.getDailyStudyGoalMinutes())
                .preferredDifficulty(profile.getPreferredDifficulty())
                .learningStyle(profile.getLearningStyle())
                .weakPoints(profile.getWeakPoints() != null ? profile.getWeakPoints() : Collections.emptyList())
                .strongPoints(profile.getStrongPoints() != null ? profile.getStrongPoints() : Collections.emptyList())
                .overallLevel(profile.getOverallLevel())
                .totalStudyDays(profile.getTotalStudyDays())
                .totalQuestions(profile.getTotalQuestions())
                .accuracyRate(profile.getAccuracyRate())
                .lastActiveTime(profile.getLastActiveTime())
                .updateTime(profile.getUpdateTime())
                .build();
    }
}
