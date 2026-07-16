package com.study.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 资料列表查询请求
 */
@Data
public class MaterialListRequest {

    /** 文件夹ID筛选（可选，null表示未分类） */
    private Long folderId;

    /** 文件名搜索（可选） */
    private String fileName;

    /** 分类筛选（可选） */
    private String category;

    /** 状态筛选（可选：processing/ready/failed） */
    private String status;

    /** 页码，从1开始 */
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer size = 10;
}
