package com.study.ai.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 Apache Tika 的文档解析器
 * 支持 PDF / DOC / DOCX / TXT 格式
 */
@Slf4j
@Component
public class TikaDocumentParser implements DocumentParser {

    /** 文本上限 10MB */
    private static final int MAX_TEXT_LENGTH = 10 * 1024 * 1024;

    /** Tika 实例（线程安全，全局复用） */
    private static final Tika TIKA = createTika();

    private static Tika createTika() {
        Tika tika = new Tika();
        tika.setMaxStringLength(MAX_TEXT_LENGTH);
        return tika;
    }

    @Override
    public String parse(InputStream input, String fileType) {
        try {
            String text = TIKA.parseToString(input);

            // 清理：去除多余空白和连续换行
            text = text.trim()
                    .replaceAll("[ \\t]+", " ")        // 合并多余空格/Tab
                    .replaceAll("\\n{3,}", "\n\n");    // 最多保留两个换行

            log.info("文档解析完成，类型={}，文本长度={}", fileType, text.length());

            if (text.length() >= MAX_TEXT_LENGTH) {
                log.warn("文档文本达到上限({}字符)，已截断", MAX_TEXT_LENGTH);
            }

            return text;
        } catch (IOException | TikaException e) {
            log.error("文档解析失败，类型={}", fileType, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }
}
