package com.study.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.BaseFont;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 错题 PDF 导出服务
 * 将用户错题本导出为格式化的 PDF 文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WrongQuestionPdfService {

    private final UserWrongQuestionMapper wrongQuestionMapper;
    private final AiQuestionBankMapper questionBankMapper;
    private final LearningMaterialMapper materialMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 导出错题为 PDF
     *
     * @param mastered 是否已掌握（null = 全部）
     * @return PDF 字节数组
     */
    public byte[] exportWrongQuestions(Boolean mastered) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 查询错题
        LambdaQueryWrapper<UserWrongQuestion> wrapper = new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(mastered != null, UserWrongQuestion::getIsMastered, mastered != null ? (mastered ? 1 : 0) : null)
                .orderByDesc(UserWrongQuestion::getLastWrongTime);

        List<UserWrongQuestion> wrongList = wrongQuestionMapper.selectList(wrapper);
        if (wrongList.isEmpty()) {
            throw new BusinessException(404, "没有可导出的错题");
        }

        // 2. 批量查询题目和资料
        Set<Long> questionIds = wrongList.stream().map(UserWrongQuestion::getQuestionId).collect(Collectors.toSet());
        Set<Long> materialIds = wrongList.stream().map(UserWrongQuestion::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, AiQuestionBank> questionMap = questionBankMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(AiQuestionBank::getId, q -> q));
        Map<Long, String> materialNameMap = materialIds.isEmpty() ? Map.of()
                : materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(LearningMaterial::getId, LearningMaterial::getOriginalName));

        // 3. 生成 PDF
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // 获取中文字体
            BaseFont bf = getChineseBaseFont();

            Font titleFont = bf != null ? new Font(bf, 18, Font.BOLD, Color.DARK_GRAY) : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font labelFont = bf != null ? new Font(bf, 11, Font.BOLD, Color.GRAY) : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.GRAY);
            Font bodyFont = bf != null ? new Font(bf, 11, Font.NORMAL, Color.BLACK) : FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font answerFont = bf != null ? new Font(bf, 11, Font.NORMAL, new Color(0, 128, 0)) : FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(0, 128, 0));
            Font wrongFont = bf != null ? new Font(bf, 11, Font.NORMAL, Color.RED) : FontFactory.getFont(FontFactory.HELVETICA, 11, Color.RED);
            Font headerBg = bf != null ? new Font(bf, 10, Font.BOLD, Color.WHITE) : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            Paragraph title = new Paragraph("错题本", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            Paragraph subtitle = new Paragraph("导出时间：" + java.time.LocalDateTime.now().format(DATE_FMT)
                    + "  |  共 " + wrongList.size() + " 道错题", labelFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(16);
            doc.add(subtitle);

            // 表格
            PdfPTable table = new PdfPTable(new float[]{1.5f, 5f, 2f, 2f, 1.5f, 2.5f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);

            // 表头
            String[] headers = {"题型", "题目", "你的答案", "正确答案", "错误次数", "来源"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerBg));
                cell.setBackgroundColor(new Color(51, 122, 183));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // 数据行
            for (int i = 0; i < wrongList.size(); i++) {
                UserWrongQuestion w = wrongList.get(i);
                AiQuestionBank q = questionMap.get(w.getQuestionId());
                String matName = materialNameMap.getOrDefault(w.getMaterialId(), "-");

                // 题型
                table.addCell(cell(getTypeName(w.getQuestionType()), bodyFont));

                // 题目
                String questionText = q != null ? q.getQuestion() : "(题目已删除)";
                if (questionText.length() > 150) questionText = questionText.substring(0, 150) + "...";
                table.addCell(cell(questionText, bodyFont));

                // 你的答案（红色）
                table.addCell(cell(w.getUserAnswer() != null ? w.getUserAnswer() : "-", wrongFont));

                // 正确答案（绿色）
                table.addCell(cell(w.getCorrectAnswer() != null ? w.getCorrectAnswer() : "-", answerFont));

                // 错误次数
                table.addCell(cell(w.getWrongCount() != null ? String.valueOf(w.getWrongCount()) : "0", bodyFont));

                // 来源
                table.addCell(cell(matName.length() > 15 ? matName.substring(0, 15) + "..." : matName, bodyFont));
            }

            doc.add(table);
            doc.close();

            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF 导出失败", e);
            throw new BusinessException(500, "PDF 导出失败");
        }
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(5);
        return c;
    }

    private String getTypeName(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "choice" -> "单选";
            case "judge" -> "判断";
            case "short_answer" -> "简答";
            case "fill_blank" -> "填空";
            case "multi_choice" -> "多选";
            case "math_fill" -> "数学";
            default -> type;
        };
    }

    private BaseFont getChineseBaseFont() {
        String[] paths = {
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/simsun.ttc",
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
            log.warn("无法加载任何中文字体，将使用默认字体，中文可能显示为乱码", e);
        }
        
        return null;
    }
}
