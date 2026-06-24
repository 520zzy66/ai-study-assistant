package com.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重命名批次请求
 */
@Data
public class RenameBatchRequest {

    /** 批次名称 */
    @NotBlank(message = "批次名称不能为空")
    @Size(max = 100, message = "批次名称最多100个字符")
    private String name;
}
