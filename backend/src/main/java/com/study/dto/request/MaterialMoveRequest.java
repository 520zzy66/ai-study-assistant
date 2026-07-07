package com.study.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 将资料移动到文件夹的请求
 */
@Data
public class MaterialMoveRequest {

    @NotEmpty(message = "资料 ID 列表不能为空")
    private List<Long> materialIds;

    /**
     * 目标文件夹 ID
     * <ul>
     *   <li>非空 → 移动到该文件夹</li>
     *   <li>null → 移出所有文件夹（根层级）</li>
     * </ul>
     */
    private Long folderId;
}
