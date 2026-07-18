package com.study.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 讯飞 TTS 可选发音人选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceOption {

    /** 发音人 ID（传给后端 TTS 调用） */
    private String id;

    /** 展示名称 */
    private String name;

    /** 性别：female / male */
    private String gender;

    /**
     * 是否需要在讯飞控制台单独开通权限。
     *
     * <p>讯飞发音人分两类：
     * <ul>
     *   <li>普通发音人（如 xiaoyan）：默认可用，无需开通</li>
     *   <li>超自然发音人（ais* 开头）：需在讯飞控制台「语音合成」→「发音人管理」中单独开通</li>
     * </ul>
     * 前端据此给用户提示，避免选择未开通的发音人导致合成失败（错误码 10043）。
     */
    private Boolean needsPermission;

    public VoiceOption(String id, String name, String gender) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.needsPermission = false;
    }
}
