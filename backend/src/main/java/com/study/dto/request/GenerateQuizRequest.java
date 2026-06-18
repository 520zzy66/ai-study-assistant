package com.study.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * AI 出题请求
 */
@Data
public class GenerateQuizRequest {

    /** 单选题数量 */
    @Min(0) @Max(10)
    private Integer choiceCount;

    /** 判断题数量 */
    @Min(0) @Max(10)
    private Integer judgeCount;

    /** 简答题数量 */
    @Min(0) @Max(5)
    private Integer shortAnswerCount;

    /** 难度 easy/medium/hard */
    @Pattern(regexp = "^(easy|medium|hard)$", message = "难度必须是 easy、medium 或 hard")
    private String difficulty;
}
