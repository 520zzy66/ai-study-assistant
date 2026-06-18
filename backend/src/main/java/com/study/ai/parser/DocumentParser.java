package com.study.ai.parser;

import java.io.InputStream;

/**
 * 文档解析器接口
 * 将不同格式的文档解析为纯文本
 */
public interface DocumentParser {

    /**
     * 解析文档为纯文本
     *
     * @param input    文件输入流
     * @param fileType 文件类型（pdf/doc/docx/txt）
     * @return 解析后的纯文本内容
     * @throws RuntimeException 解析失败时抛出
     */
    String parse(InputStream input, String fileType);
}
