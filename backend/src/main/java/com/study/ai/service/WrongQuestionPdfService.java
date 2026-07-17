package com.study.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.study.common.BusinessException;
import com.study.common.UserContext;
import com.study.entity.AiQuestionBank;
import com.study.entity.LearningMaterial;
import com.study.entity.UserWrongQuestion;
import com.study.mapper.AiQuestionBankMapper;
import com.study.mapper.LearningMaterialMapper;
import com.study.mapper.UserWrongQuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 错题和题库 PDF 导出服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WrongQuestionPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color PRIMARY = new Color(45, 90, 39);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LIGHT_BORDER = new Color(226, 232, 240);

    private final UserWrongQuestionMapper wrongQuestionMapper;
    private final AiQuestionBankMapper questionBankMapper;
    private final LearningMaterialMapper materialMapper;

    /**
     * 导出当前用户的错题 PDF，保留原有“全部/按掌握状态筛选”行为。
     *
     * @param mastered 是否已掌握，null 表示全部
     * @return PDF 字节数组
     */
    public byte[] exportWrongQuestions(Boolean mastered) {
        Long userId = UserContext.getCurrentUserId();
        LambdaQueryWrapper<UserWrongQuestion> wrapper = new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(mastered != null, UserWrongQuestion::getIsMastered, mastered != null && mastered ? 1 : 0)
                .orderByDesc(UserWrongQuestion::getLastWrongTime);

        List<UserWrongQuestion> wrongList = wrongQuestionMapper.selectList(wrapper);
        return exportWrongQuestionList(wrongList, "错题本");
    }

    /**
     * 导出用户勾选的错题。
     *
     * @param wrongQuestionIds 错题记录 ID 列表
     * @return PDF 字节数组
     */
    public byte[] exportSelectedWrongQuestions(List<Long> wrongQuestionIds) {
        if (wrongQuestionIds == null || wrongQuestionIds.isEmpty()) {
            throw new BusinessException(400, "请先选择要导出的错题");
        }

        Long userId = UserContext.getCurrentUserId();
        List<Long> distinctIds = wrongQuestionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException(400, "请先选择要导出的错题");
        }

        List<UserWrongQuestion> wrongList = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<UserWrongQuestion>()
                        .eq(UserWrongQuestion::getUserId, userId)
                        .in(UserWrongQuestion::getId, distinctIds)
                        .orderByDesc(UserWrongQuestion::getLastWrongTime));

        if (wrongList.size() != distinctIds.size()) {
            throw new BusinessException(403, "包含无权导出的错题");
        }

        return exportWrongQuestionList(wrongList, "错题本（选中题目）");
    }

    /**
     * 导出指定题库批次为试卷式 PDF。
     *
     * @param batchId 题库批次 ID
     * @return PDF 字节数组
     */
    public byte[] exportBatchQuestions(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            throw new BusinessException(400, "批次ID不能为空");
        }

        Long userId = UserContext.getCurrentUserId();
        List<AiQuestionBank> questions = questionBankMapper.selectList(
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getUserId, userId)
                        .eq(AiQuestionBank::getBatchId, batchId)
                        .orderByAsc(AiQuestionBank::getId));

        if (questions.isEmpty()) {
            throw new BusinessException(404, "题库批次不存在或没有可导出的题目");
        }

        String batchName = firstText(questions.get(0).getBatchName(), "题库试卷");
        return exportQuestionPaper(batchName, questions, false, Map.of(), Map.of());
    }

    private byte[] exportWrongQuestionList(List<UserWrongQuestion> wrongList, String title) {
        if (wrongList == null || wrongList.isEmpty()) {
            throw new BusinessException(404, "没有可导出的错题");
        }

        Set<Long> questionIds = wrongList.stream()
                .map(UserWrongQuestion::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> materialIds = wrongList.stream()
                .map(UserWrongQuestion::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, AiQuestionBank> questionMap = questionIds.isEmpty()
                ? Map.of()
                : questionBankMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(AiQuestionBank::getId, q -> q));
        Map<Long, String> materialNameMap = materialIds.isEmpty()
                ? Map.of()
                : materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(LearningMaterial::getId, LearningMaterial::getOriginalName));

        List<AiQuestionBank> questions = new ArrayList<>();
        Map<Long, WrongExportMeta> wrongMetaMap = new LinkedHashMap<>();
        for (UserWrongQuestion wrong : wrongList) {
            AiQuestionBank question = questionMap.get(wrong.getQuestionId());
            if (question == null) {
                question = new AiQuestionBank();
                question.setId(wrong.getQuestionId());
                question.setQuestionType(wrong.getQuestionType());
                question.setQuestion("题目已删除");
                question.setAnswer(wrong.getCorrectAnswer());
            }
            questions.add(question);
            wrongMetaMap.put(question.getId(), new WrongExportMeta(
                    firstText(wrong.getUserAnswer(), "-"),
                    firstText(wrong.getCorrectAnswer(), firstText(question.getAnswer(), "-")),
                    wrong.getWrongCount() == null ? 0 : wrong.getWrongCount(),
                    materialNameMap.getOrDefault(wrong.getMaterialId(), "-")
            ));
        }

        return exportQuestionPaper(title, questions, true, wrongMetaMap, materialNameMap);
    }

    private byte[] exportQuestionPaper(String title,
                                       List<AiQuestionBank> questions,
                                       boolean wrongBookMode,
                                       Map<Long, WrongExportMeta> wrongMetaMap,
                                       Map<Long, String> materialNameMap) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(doc, out);
            doc.open();

            FontSet fonts = createFontSet();
            addTitle(doc, title, questions.size(), fonts);
            addQuestionSection(doc, questions, fonts);
            doc.newPage();
            addAnswerSection(doc, questions, wrongBookMode, wrongMetaMap, materialNameMap, fonts);
            doc.close();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF 导出失败", e);
            throw new BusinessException(500, "PDF 导出失败");
        }
    }

    private void addTitle(Document doc, String title, int questionCount, FontSet fonts) throws Exception {
        Paragraph heading = new Paragraph(firstText(title, "题目导出"), fonts.title);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingAfter(8);
        doc.add(heading);

        Paragraph subtitle = new Paragraph(
                "导出时间：" + LocalDateTime.now().format(DATE_FMT) + "    共 " + questionCount + " 题",
                fonts.meta
        );
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(18);
        doc.add(subtitle);
    }

    private void addQuestionSection(Document doc, List<AiQuestionBank> questions, FontSet fonts) throws Exception {
        addSectionTitle(doc, "一、题目", fonts);
        for (int i = 0; i < questions.size(); i++) {
            AiQuestionBank question = questions.get(i);
            Paragraph q = new Paragraph((i + 1) + ". [" + typeName(question.getQuestionType()) + "] "
                    + firstText(question.getQuestion(), "题目内容为空"), fonts.body);
            q.setSpacingBefore(8);
            q.setSpacingAfter(6);
            q.setLeading(16);
            doc.add(q);

            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                PdfPTable optionTable = new PdfPTable(1);
                optionTable.setWidthPercentage(100);
                for (Map.Entry<String, String> option : sortedOptions(question.getOptions()).entrySet()) {
                    PdfPCell cell = new PdfPCell(new Phrase(option.getKey() + ". " + option.getValue(), fonts.body));
                    cell.setBorderColor(LIGHT_BORDER);
                    cell.setPadding(5);
                    optionTable.addCell(cell);
                }
                optionTable.setSpacingAfter(6);
                doc.add(optionTable);
            }
        }
    }

    private void addAnswerSection(Document doc,
                                  List<AiQuestionBank> questions,
                                  boolean wrongBookMode,
                                  Map<Long, WrongExportMeta> wrongMetaMap,
                                  Map<Long, String> materialNameMap,
                                  FontSet fonts) throws Exception {
        addSectionTitle(doc, "二、答案与解析", fonts);
        for (int i = 0; i < questions.size(); i++) {
            AiQuestionBank question = questions.get(i);
            WrongExportMeta wrongMeta = wrongMetaMap.get(question.getId());

            Paragraph answer = new Paragraph((i + 1) + ". 正确答案："
                    + firstText(wrongMeta != null ? wrongMeta.correctAnswer() : question.getAnswer(), "-"), fonts.body);
            answer.setSpacingBefore(8);
            answer.setLeading(16);
            doc.add(answer);

            if (wrongBookMode && wrongMeta != null) {
                Paragraph wrongInfo = new Paragraph("你的答案：" + wrongMeta.userAnswer()
                        + "    错误次数：" + wrongMeta.wrongCount()
                        + "    来源资料：" + wrongMeta.materialName(), fonts.meta);
                wrongInfo.setSpacingAfter(3);
                doc.add(wrongInfo);
            }

            if (!wrongBookMode) {
                Paragraph meta = new Paragraph("题型：" + typeName(question.getQuestionType())
                        + "    难度：" + difficultyName(question.getDifficulty()), fonts.meta);
                meta.setSpacingAfter(3);
                doc.add(meta);
            }

            String explanation = firstText(question.getExplanation(), "暂无解析");
            Paragraph exp = new Paragraph("解析：" + explanation, fonts.body);
            exp.setLeading(16);
            exp.setSpacingAfter(6);
            doc.add(exp);
        }
    }

    private void addSectionTitle(Document doc, String text, FontSet fonts) throws Exception {
        Paragraph section = new Paragraph(text, fonts.section);
        section.setSpacingBefore(4);
        section.setSpacingAfter(10);
        doc.add(section);
    }

    private Map<String, String> sortedOptions(Map<String, String> options) {
        return options.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private String typeName(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "choice" -> "单选题";
            case "judge" -> "判断题";
            case "short_answer" -> "简答题";
            case "fill_blank" -> "填空题";
            case "multi_choice" -> "多选题";
            case "math_fill" -> "数学填空";
            default -> type;
        };
    }

    private String difficultyName(String difficulty) {
        if (difficulty == null) return "未知";
        return switch (difficulty) {
            case "easy" -> "简单";
            case "medium" -> "中等";
            case "hard" -> "困难";
            default -> difficulty;
        };
    }

    private String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private FontSet createFontSet() {
        BaseFont baseFont = getChineseBaseFont();
        if (baseFont != null) {
            return new FontSet(
                    new Font(baseFont, 18, Font.BOLD, PRIMARY),
                    new Font(baseFont, 14, Font.BOLD, PRIMARY),
                    new Font(baseFont, 11, Font.NORMAL, Color.BLACK),
                    new Font(baseFont, 10, Font.NORMAL, MUTED)
            );
        }

        return new FontSet(
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY),
                FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK),
                FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED)
        );
    }

    private BaseFont getChineseBaseFont() {
        String[] paths = {
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/simhei.ttf",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
                "/usr/share/fonts/wqy-microhei/wqy-microhei.ttc,0",
                "/usr/share/fonts/google-noto-cjk/NotoSansCJK-Regular.ttc,0",
                "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc,0",
                "/System/Library/Fonts/PingFang.ttc",
                "/System/Library/Fonts/STHeiti Light.ttc"
        };

        for (String path : paths) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
            }
        }

        try {
            return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            log.warn("无法加载中文字体，PDF 中文可能无法正常显示", e);
            return null;
        }
    }

    private record WrongExportMeta(String userAnswer, String correctAnswer, int wrongCount, String materialName) {
    }

    private record FontSet(Font title, Font section, Font body, Font meta) {
    }
}
