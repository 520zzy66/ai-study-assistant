package com.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建文件夹请求
 */
@Data
public class FolderCreateRequest {

    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 100, message = "文件夹名称不能超过 100 个字符")
    private String name;

    /** 父文件夹 ID（为空表示根层级） */
    private Long parentId;
}
