package com.study.service;

import com.study.entity.UserMemoryEntry;

import java.util.List;

/**
 * 用户记忆服务接口
 *
 * <p>管理非结构化长期记忆条目（用户不可见）。
 * 支持记忆写入、语义检索、重要度分层清理。
 */
public interface UserMemoryService {

    /**
     * 写入一条记忆条目
     *
     * @param userId     用户 ID
     * @param category   记忆类别
     * @param content    记忆内容
     * @param importance 重要度 0~1
     * @param sourceType 来源类型
     * @param sourceId   来源记录 ID
     */
    void addMemory(Long userId, String category, String content, float importance,
                   String sourceType, Long sourceId);

    /**
     * 批量写入记忆条目
     *
     * @param entries 记忆条目列表（userId 必须已设置）
     */
    void addMemories(List<UserMemoryEntry> entries);

    /**
     * 语义检索记忆条目
     *
     * <p>按时间+重要度分层召回：
     * <ul>
     *   <li>90 天内：全量检索</li>
     *   <li>90 天以上：仅检索 importance >= 0.7 的记忆</li>
     * </ul>
     *
     * @param userId 用户 ID
     * @param query  查询文本（用于语义匹配）
     * @param limit  返回条数上限
     * @return 按相关性排序的记忆列表
     */
    List<UserMemoryEntry> searchMemories(Long userId, String query, int limit);

    /**
     * 获取用户的全部记忆条目（按重要度降序）
     *
     * @param userId 用户 ID
     * @param limit  条数上限
     * @return 记忆列表
     */
    List<UserMemoryEntry> listMemories(Long userId, int limit);

    /**
     * 清理过期记忆条目（定时任务调用）
     *
     * <p>清理规则：
     * <ul>
     *   <li>90 天前 且 importance < 0.9 → 删除</li>
     *   <li>importance = 1.0 → 永不删除</li>
     * </ul>
     *
     * @return 清理的条目数
     */
    int cleanupExpiredMemories();
}
