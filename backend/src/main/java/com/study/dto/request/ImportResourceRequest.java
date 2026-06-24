package com.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 导入在线资源请求
 */
@Data
public class ImportResourceRequest {

    /** 资源 URL */
    @NotBlank(message = "URL 不能为空")
    @Size(max = 2000, message = "URL 最多2000个字符")
    private String url;

    /** 资源标题（可选，默认从 URL 提取） */
    @Size(max = 200, message = "标题最多200个字符")
    private String title;
}
