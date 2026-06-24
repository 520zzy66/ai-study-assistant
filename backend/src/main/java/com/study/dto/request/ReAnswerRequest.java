package com.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重新作答请求
 */
@Data
public class ReAnswerRequest {

    /** 用户答案 */
    @NotBlank(message = "答案不能为空")
    @Size(max = 5000, message = "答案最多5000个字符")
    private String answer;
}
