package com.study.dto.request;

import lombok.Data;

/**
 * 生成摘要请求
 */
@Data
public class GenerateSummaryRequest {

    /** 是否强制重新生成（已有摘要时） */
    private Boolean force;
}
