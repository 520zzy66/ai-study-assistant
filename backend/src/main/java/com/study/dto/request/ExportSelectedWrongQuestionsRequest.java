package com.study.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 导出选中错题请求。
 */
@Data
public class ExportSelectedWrongQuestionsRequest {

    /**
     * 错题记录 ID 列表。
     */
    @NotEmpty(message = "请先选择要导出的错题")
    private List<Long> wrongQuestionIds;
}
