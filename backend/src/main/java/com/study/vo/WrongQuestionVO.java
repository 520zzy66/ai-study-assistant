package com.study.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

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
    private Map<String, String> options; // 选项
    private String userAnswer;
    private String correctAnswer;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
    private Integer isMastered;
    private String materialName;    // 资料名称
}
