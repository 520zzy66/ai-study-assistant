package com.study.ai;

import com.study.ai.service.WrongQuestionPdfService;
import com.study.common.BusinessException;
import com.study.common.UserContext;
import com.study.entity.AiQuestionBank;
import com.study.entity.LearningMaterial;
import com.study.entity.UserWrongQuestion;
import com.study.mapper.AiQuestionBankMapper;
import com.study.mapper.LearningMaterialMapper;
import com.study.mapper.UserWrongQuestionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

class WrongQuestionPdfServiceTest {

    private UserWrongQuestionMapper wrongQuestionMapper;
    private AiQuestionBankMapper questionBankMapper;
    private LearningMaterialMapper materialMapper;
    private WrongQuestionPdfService pdfService;

    @BeforeEach
    void setUp() {
        wrongQuestionMapper = Mockito.mock(UserWrongQuestionMapper.class);
        questionBankMapper = Mockito.mock(AiQuestionBankMapper.class);
        materialMapper = Mockito.mock(LearningMaterialMapper.class);

        pdfService = new WrongQuestionPdfService(
                wrongQuestionMapper,
                questionBankMapper,
                materialMapper
        );

        UserContext.setCurrentUser(new UserContext.UserInfo(1L, "test_user"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void exportWrongQuestionsCreatesPdf() {
        UserWrongQuestion wrongQuestion = buildWrongQuestion();
        AiQuestionBank question = buildQuestion();
        LearningMaterial material = new LearningMaterial();
        material.setId(201L);
        material.setOriginalName("软件工程导论.pdf");

        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(wrongQuestion));
        when(questionBankMapper.selectBatchIds(anyCollection())).thenReturn(List.of(question));
        when(materialMapper.selectBatchIds(anyCollection())).thenReturn(List.of(material));

        byte[] pdfBytes = pdfService.exportWrongQuestions(null);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void exportSelectedWrongQuestionsCreatesPdf() {
        UserWrongQuestion wrongQuestion = buildWrongQuestion();
        AiQuestionBank question = buildQuestion();

        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(wrongQuestion));
        when(questionBankMapper.selectBatchIds(anyCollection())).thenReturn(List.of(question));
        when(materialMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        byte[] pdfBytes = pdfService.exportSelectedWrongQuestions(List.of(1L));

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void exportSelectedWrongQuestionsRejectsEmptySelection() {
        assertThrows(BusinessException.class, () -> pdfService.exportSelectedWrongQuestions(List.of()));
    }

    @Test
    void exportBatchQuestionsCreatesPdf() {
        when(questionBankMapper.selectList(any())).thenReturn(List.of(buildQuestion()));

        byte[] pdfBytes = pdfService.exportBatchQuestions("batch01");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    private UserWrongQuestion buildWrongQuestion() {
        UserWrongQuestion wrongQuestion = new UserWrongQuestion();
        wrongQuestion.setId(1L);
        wrongQuestion.setUserId(1L);
        wrongQuestion.setQuestionId(101L);
        wrongQuestion.setMaterialId(201L);
        wrongQuestion.setQuestionType("choice");
        wrongQuestion.setUserAnswer("A");
        wrongQuestion.setCorrectAnswer("B");
        wrongQuestion.setWrongCount(3);
        wrongQuestion.setLastWrongTime(LocalDateTime.now());
        return wrongQuestion;
    }

    private AiQuestionBank buildQuestion() {
        AiQuestionBank question = new AiQuestionBank();
        question.setId(101L);
        question.setUserId(1L);
        question.setMaterialId(201L);
        question.setBatchId("batch01");
        question.setBatchName("软件工程练习");
        question.setQuestionType("choice");
        question.setDifficulty("medium");
        question.setQuestion("以下哪一项是软件工程中关于高内聚低耦合的正确理解？");
        question.setOptions(Map.of("A", "模块之间互相依赖越多越好", "B", "模块内部职责集中且模块间依赖较少"));
        question.setAnswer("B");
        question.setExplanation("高内聚强调模块内部职责集中，低耦合强调模块之间依赖尽量少。");
        return question;
    }
}
