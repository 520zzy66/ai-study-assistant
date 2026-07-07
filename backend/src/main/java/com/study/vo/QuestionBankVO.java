package com.study.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题库 VO
 * 隐藏 userId、materialId、batchId 等内部字段
 */
@Data
public class QuestionBankVO {

    private Long id;
    private String batchName;
    private String questionType;
    private String difficulty;
    private String question;
    private String options;
    private String answer;
    private String explanation;
    private Integer isFavorite;
    private LocalDateTime createTime;
}
