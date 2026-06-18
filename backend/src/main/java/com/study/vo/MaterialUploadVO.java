package com.study.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资料上传响应 VO
 * 继承 MaterialVO 减少字段重复
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialUploadVO extends MaterialVO {
    // 上传响应与列表详情字段一致，不再重复定义
    // 基类 MaterialVO 已包含：id, originalName, fileType, fileSize, category, status, createTime
}
