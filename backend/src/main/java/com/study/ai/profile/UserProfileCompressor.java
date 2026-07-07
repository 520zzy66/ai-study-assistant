package com.study.ai.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.AiChatHistory;
import com.study.entity.UserProfile;
import com.study.mapper.AiChatHistoryMapper;
import com.study.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户学习画像压缩器 — 用 LLM 把最近 50 轮对话压缩成 500 字画像描述
 *
 * <p>方案 B：异步触发 + 数据库缓存。
 * GeneralNode 每次路由时检查缓存是否过期（超过 10 轮对话未更新），
 * 如果过期则异步触发压缩任务，不阻塞当前请求。
 * 本次路由使用上一版缓存（或空），下一轮即可使用最新的压缩结果。
 *
 * @author AI Study Assistant
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileCompressor {

    /** 压缩时的对话轮次窗口 */
    private static final int COMPRESSION_WINDOW = 50;

    /** 画像最大字数限制 */
    private static final int MAX_PROFILE_LENGTH = 500;

    /** 过期判定：新增多少轮对话后触发重新压缩 */
    private static final int EXPIRY_CHAT_COUNT = 10;

    /** 压缩 prompt 文件路径 */
    private static final String COMPRESSION_PROMPT_FILE = "prompts/profile-compression.txt";

    @Qualifier("routerChatClient")
    private final ChatClient routerChatClient;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final UserProfileMapper userProfileMapper;

    /**
     * 检查用户画像是否过期（需要重新压缩）
     *
     * @param profile     当前用户画像
     * @param totalChats  当前总对话轮次（来自 UserProfile.totalQuestions）
     * @return true = 需要重新压缩
     */
    public boolean isStale(UserProfile profile, int totalChats) {
        // 从未压缩过
        if (profile.getCompressedProfile() == null || profile.getCompressedProfile().isBlank()) {
            return true;
        }

        // 超过 24 小时未更新
        if (profile.getCompressedAt() == null
                || profile.getCompressedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            return true;
        }

        // 新增对话轮次超过阈值
        int version = profile.getProfileVersion() != null ? profile.getProfileVersion() : 0;
        return (totalChats - version) >= EXPIRY_CHAT_COUNT;
    }

    /**
     * 异步触发 LLM 压缩任务（带过期检查，避免无效 LLM 调用）
     *
     * <p>由 GeneralNode 调用，不阻塞问答主流程。
     * 每次调用先检查画像是否过期（距上次压缩新增 10 轮对话或超过 24 小时），
     * 未过期则直接跳过，节省 LLM 调用成本。
     *
     * @param userId 用户 ID
     */
    @Async
    public void compressAsync(Long userId) {
        try {
            // 检查是否过期，未过期则跳过
            if (!needsCompression(userId)) {
                log.debug("[ProfileCompressor] 用户 {} 画像未过期，跳过压缩", userId);
                return;
            }
            doCompress(userId);
        } catch (Exception e) {
            log.warn("[ProfileCompressor] 压缩失败：userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 判断用户画像是否需要重新压缩
     *
     * @param userId 用户 ID
     * @return true = 需要压缩
     */
    private boolean needsCompression(Long userId) {
        try {
            UserProfile profile = userProfileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
            );

            // 用户画像不存在 → 需要创建并压缩
            if (profile == null) return true;

            // 从未压缩过
            if (profile.getCompressedProfile() == null || profile.getCompressedProfile().isBlank()) {
                return true;
            }

            // 超过 24 小时未更新
            if (profile.getCompressedAt() == null
                    || profile.getCompressedAt().isBefore(LocalDateTime.now().minusHours(24))) {
                return true;
            }

            // 检查新增对话轮次：统计 QA 对话总数 vs 压缩时的版本号
            Long totalQaChats = chatHistoryMapper.selectCount(
                    new LambdaQueryWrapper<AiChatHistory>()
                            .eq(AiChatHistory::getUserId, userId)
                            .eq(AiChatHistory::getChatType, "qa")
            );
            int version = profile.getProfileVersion() != null ? profile.getProfileVersion() : 0;
            return (totalQaChats - version) >= EXPIRY_CHAT_COUNT;

        } catch (Exception e) {
            log.debug("[ProfileCompressor] 过期检查失败，默认触发压缩：{}", e.getMessage());
            return true;  // 出错时宁可多压缩一次，也不能让画像过期
        }
    }

    /**
     * 同步执行压缩（供管理接口或调试使用）
     *
     * @param userId 用户 ID
     * @return 压缩后的画像文本
     */
    public String compressSync(Long userId) {
        try {
            return doCompress(userId);
        } catch (Exception e) {
            log.warn("[ProfileCompressor] 压缩失败：userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 核心压缩逻辑
     */
    private String doCompress(Long userId) {
        // 1. 取最近 N 轮 QA 对话
        List<AiChatHistory> recentChats = chatHistoryMapper.selectList(
                new LambdaQueryWrapper<AiChatHistory>()
                        .eq(AiChatHistory::getUserId, userId)
                        .eq(AiChatHistory::getChatType, "qa")
                        .orderByDesc(AiChatHistory::getCreateTime)
                        .last("LIMIT " + COMPRESSION_WINDOW)
        );

        if (recentChats.isEmpty()) {
            log.info("[ProfileCompressor] 用户 {} 无对话记录，跳过压缩", userId);
            return null;
        }

        // 按时间正序（早的在前）
        java.util.Collections.reverse(recentChats);

        // 2. 拼对话文本
        String conversationText = buildConversationText(recentChats);

        // 3. 调用 LLM 压缩
        String compressed = callLlmForCompression(userId, conversationText);

        if (compressed == null || compressed.isBlank()) {
            return null;
        }

        // 4. 写回 user_profile 表
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );

        if (profile == null) {
            // 用户画像尚未创建，创建一个空的
            profile = new UserProfile();
            profile.setUserId(userId);
            userProfileMapper.insert(profile);
            // 重新查询
            profile = userProfileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
            );
        }

        profile.setCompressedProfile(compressed.length() > MAX_PROFILE_LENGTH
                ? compressed.substring(0, MAX_PROFILE_LENGTH) : compressed);
        profile.setProfileVersion(recentChats.size());
        profile.setCompressedAt(LocalDateTime.now());
        userProfileMapper.updateById(profile);

        log.info("[ProfileCompressor] 用户 {} 画像压缩完成：{} 字（来自 {} 轮对话）",
                userId, compressed.length(), recentChats.size());

        return compressed;
    }

    /**
     * 构建对话文本（供 LLM 压缩用）
     */
    private String buildConversationText(List<AiChatHistory> chats) {
        return chats.stream()
                .map(chat -> "用户：" + truncate(chat.getUserMessage(), 200)
                        + "\n助手：" + truncate(chat.getAiResponse(), 200))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 调用 LLM 执行压缩
     */
    private String callLlmForCompression(Long userId, String conversationText) {
        String prompt = String.format("""
                请根据以下用户最近 %d 轮问答对话，压缩成一段 %d 字以内的「用户学习画像描述」。

                ## 需要提炼的维度
                1. 备考目标（考公/考研/其他，具体考试类型）
                2. 当前备考阶段（入门/基础/强化/冲刺）
                3. 常考科目和模块
                4. 薄弱知识点（用户经常问的、容易混淆的）
                5. 学习偏好（喜欢的讲解风格、是否需要举一反三、易错点）
                6. 最近关注的热点（最近 5 轮对话的主题趋势）

                ## 对话记录
                %s

                ## 输出要求
                - 直接输出画像描述文字，不要列表、开场白或结束语
                - %d 字以内
                - 基于对话内容合理推断，不要编造未提及的信息
                - 如果对话太少无法推断，如实说明""",
                COMPRESSION_WINDOW, MAX_PROFILE_LENGTH, conversationText, MAX_PROFILE_LENGTH);

        try {
            return routerChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();
        } catch (Exception e) {
            log.error("[ProfileCompressor] LLM 调用失败：userId={}", userId, e);
            return null;
        }
    }

    /**
     * 读取已缓存的用户画像描述（供 GeneralNode 快速获取，无 LLM 调用）
     *
     * @param userId 用户 ID
     * @return 缓存的画像描述；未压缩过则返回 null
     */
    public String getCachedProfile(Long userId) {
        if (userId == null || userId <= 0) return null;
        try {
            UserProfile profile = userProfileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
            );
            return profile != null ? profile.getCompressedProfile() : null;
        } catch (Exception e) {
            log.debug("[ProfileCompressor] 读取缓存失败：userId={}", userId);
            return null;
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
