package com.study.ai;

import com.study.ai.service.WrongQuestionPdfService;
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

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class WrongQuestionPdfServiceTest {

    private UserWrongQuestionMapper wrongQuestionMapper;
    private AiQuestionBankMapper questionBankMapper;
    private LearningMaterialMapper materialMapper;
    private WrongQuestionPdfService pdfService;

    @BeforeEach
    public void setUp() {
        wrongQuestionMapper = Mockito.mock(UserWrongQuestionMapper.class);
        questionBankMapper = Mockito.mock(AiQuestionBankMapper.class);
        materialMapper = Mockito.mock(LearningMaterialMapper.class);

        pdfService = new WrongQuestionPdfService(
                wrongQuestionMapper,
                questionBankMapper,
                materialMapper
        );

        // 设置当前用户上下文
        UserContext.setCurrentUser(new UserContext.UserInfo(1L, "test_user"));
    }

    @AfterEach
    public void tearDown() {
        UserContext.clear();
    }

    @Test
    public void testExportWrongQuestions() throws Exception {
        // 1. Mock 错题数据
        UserWrongQuestion wq = new UserWrongQuestion();
        wq.setId(1L);
        wq.setUserId(1L);
        wq.setQuestionId(101L);
        wq.setMaterialId(201L);
        wq.setQuestionType("choice");
        wq.setUserAnswer("A");
        wq.setCorrectAnswer("B");
        wq.setWrongCount(3);
        wq.setLastWrongTime(LocalDateTime.now());

        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(wq));

        // 2. Mock 题目数据
        AiQuestionBank question = new AiQuestionBank();
        question.setId(101L);
        question.setQuestion("以下哪项是软件工程中关于高内聚低耦合的正确理解？测试中文字体是否正常渲染。");
        when(questionBankMapper.selectBatchIds(Set.of(101L))).thenReturn(List.of(question));

        // 3. Mock 资料数据
        LearningMaterial material = new LearningMaterial();
        material.setId(201L);
        material.setOriginalName("软件工程导论.pdf");
        when(materialMapper.selectBatchIds(Set.of(201L))).thenReturn(List.of(material));

        // 4. 执行导出
        byte[] pdfBytes = pdfService.exportWrongQuestions(null);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // 保存文件到本地以供检查是否乱码
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File pdfFile = new File(targetDir, "wrong-questions-test.pdf");
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            fos.write(pdfBytes);
        }
        System.out.println("测试 PDF 已成功导出至：" + pdfFile.getAbsolutePath());
    }
}
