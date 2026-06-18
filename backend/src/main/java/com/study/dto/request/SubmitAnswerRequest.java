package com.study.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交答案请求
 */
@Data
public class SubmitAnswerRequest {

    /** 答案列表 */
    @NotEmpty(message = "答案不能为空")
    @Valid
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {

        /** 题目ID */
        @NotNull(message = "题目ID不能为空")
        private Long questionId;

        /** 用户答案 */
        private String answer;
    }
}
