package com.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户记忆条目实体（非结构化长期记忆）
 *
 * <p>存储 AI 从对话/答题/计划中提炼的用户记忆碎片，
 * 通过向量检索在后续对话中注入 Prompt，实现跨会话记忆。
 *
 * <p><b>用户不可见</b>：此类记忆纯系统内部使用，不暴露给前端。
 */
@Data
@TableName("user_memory_entry")
public class UserMemoryEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /**
     * 记忆类别
     * <ul>
     *   <li>preference — 偏好</li>
     *   <li>goal — 目标</li>
     *   <li>knowledge — 知识点</li>
     *   <li>mistake — 错题关联</li>
     *   <li>habit — 学习习惯</li>
     * </ul>
     */
    private String category;

    /** 记忆内容（自然语言短句） */
    private String content;

    /**
     * 重要度 0~1
     * <ul>
     *   <li>1.0 = 永不淘汰（如目标考试）</li>
     *   <li>0.7~0.9 = 90 天后仅保留高重要度</li>
     *   <li>&lt; 0.7 = 30 天后可能被淘汰</li>
     * </ul>
     */
    private Float importance;

    /** 来源类型：dialog / quiz / plan / profile */
    private String sourceType;

    /** 来源记录 ID（可选） */
    private Long sourceId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
