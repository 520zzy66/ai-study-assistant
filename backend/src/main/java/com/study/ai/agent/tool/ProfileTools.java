package com.study.ai.agent.tool;

import com.study.dto.request.GenerateResourcePackageRequest;
import com.study.entity.LearningMaterial;
import com.study.service.UserProfileService;
import com.study.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ProfileTools {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final UserProfileService userProfileService;

    /**
     * Loads or creates the current user's learning profile.
     *
     * @param userId current user id
     * @return user profile
     */
    public UserProfileVO getOrCreateProfile(Long userId) {
        return userProfileService.getOrCreateProfile(userId);
    }

    /**
     * Resolves the study goal from request, profile, and material metadata.
     *
     * @param request resource package request
     * @param material material entity
     * @param profile user profile
     * @return resolved study goal
     */
    public String resolveGoal(GenerateResourcePackageRequest request, LearningMaterial material, UserProfileVO profile) {
        if (request.getGoal() != null && !request.getGoal().isBlank()) {
            return request.getGoal().trim();
        }
        if (profile.getStudySubject() != null && !profile.getStudySubject().isBlank()) {
            return "围绕" + profile.getStudySubject() + "掌握《" + material.getOriginalName() + "》核心知识";
        }
        return "掌握《" + material.getOriginalName() + "》核心知识并完成阶段复习";
    }

    /**
     * Resolves the target difficulty from request or profile preference.
     *
     * @param request resource package request
     * @param profile user profile
     * @return easy, medium, or hard
     */
    public String resolveDifficulty(GenerateResourcePackageRequest request, UserProfileVO profile) {
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            return request.getDifficulty();
        }
        String preferred = profile.getPreferredDifficulty();
        if ("easy".equals(preferred)) return "easy";
        if ("hard".equals(preferred)) return "hard";
        return "medium";
    }

    /**
     * Resolves the target exam date.
     *
     * @param request resource package request
     * @return yyyy-MM-dd date string
     */
    public String resolveExamDate(GenerateResourcePackageRequest request) {
        if (request.getExamDate() != null && !request.getExamDate().isBlank()) {
            return request.getExamDate();
        }
        return LocalDate.now().plusDays(14).format(DATE_FMT);
    }

    /**
     * Resolves daily study hours from request or profile minutes.
     *
     * @param request resource package request
     * @param profile user profile
     * @return daily study hours
     */
    public int resolveDailyHours(GenerateResourcePackageRequest request, UserProfileVO profile) {
        if (request.getDailyHours() != null) {
            return request.getDailyHours();
        }
        Integer minutes = profile.getDailyStudyGoalMinutes();
        if (minutes == null || minutes <= 0) {
            return 2;
        }
        return Math.max(1, Math.min(12, (int) Math.ceil(minutes / 60.0)));
    }

    /**
     * Formats profile fields into compact text for downstream prompts.
     *
     * @param profile user profile
     * @return compact profile text
     */
    public String formatProfile(UserProfileVO profile) {
        StringBuilder sb = new StringBuilder();
        appendProfile(sb, "学籍", profile.getEducation());
        appendProfile(sb, "目标考试", profile.getTargetExam());
        appendProfile(sb, "学习科目", profile.getStudySubject());
        appendProfile(sb, "难度偏好", profile.getPreferredDifficulty());
        appendProfile(sb, "学习风格", profile.getLearningStyle());
        appendProfile(sb, "每日学习目标", profile.getDailyStudyGoalMinutes() == null ? null : profile.getDailyStudyGoalMinutes() + "分钟");
        appendProfile(sb, "薄弱知识点", profile.getWeakPoints() == null ? null : String.join("、", profile.getWeakPoints()));
        appendProfile(sb, "擅长知识点", profile.getStrongPoints() == null ? null : String.join("、", profile.getStrongPoints()));
        appendProfile(sb, "综合水平", profile.getOverallLevel() == null ? null : String.valueOf(profile.getOverallLevel()));
        return sb.length() == 0 ? "暂无明确画像，按通用高校学习者处理" : sb.toString();
    }

    private void appendProfile(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append("：").append(value).append("\n");
        }
    }
}
