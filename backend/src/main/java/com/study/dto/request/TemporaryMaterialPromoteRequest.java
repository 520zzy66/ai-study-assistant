package com.study.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 将临时资料添加到“我的资料”的请求。
 */
@Data
public class TemporaryMaterialPromoteRequest {

    private Long folderId;

    @Size(max = 50, message = "分类名称最多50个字符")
    private String category;
}
