package com.study.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本 VO
 */
@Data
public class WrongQuestionVO {

    private Long id;
    private Long questionId;
    private Long materialId;
    private String questionType;
    private String question;        // 题目文本
    private String userAnswer;
    private String correctAnswer;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
    private Integer isMastered;
    private String materialName;    // 资料名称
}
