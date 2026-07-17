package com.study.ai.parser;

import com.study.ai.agent.config.AgentClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * 临时资料内容提取器：文本文档走 Tika，图片走 Qwen2.5-VL。
 */
@Slf4j
@Component
public class TemporaryMaterialContentExtractor {

    private static final Set<String> IMAGE_TYPES = Set.of("png", "jpg", "jpeg", "webp");

    private final DocumentParser documentParser;
    private final ChatClient multimodalClient;

    public TemporaryMaterialContentExtractor(DocumentParser documentParser,
                                             AgentClientFactory agentClientFactory) {
        this.documentParser = documentParser;
        this.multimodalClient = agentClientFactory.getOrCreate("multimodal");
    }

    /**
     * 提取可用于切片和检索的文字内容。
     *
     * @param file 文件路径
     * @param fileType 文件扩展名
     * @return 提取后的资料内容
     */
    public String extract(Path file, String fileType) {
        if (IMAGE_TYPES.contains(fileType)) {
            return extractImage(file, fileType);
        }
        try (InputStream input = Files.newInputStream(file)) {
            return documentParser.parse(input, fileType);
        } catch (Exception e) {
            throw new IllegalStateException("临时资料解析失败: " + e.getMessage(), e);
        }
    }

    private String extractImage(Path file, String fileType) {
        String mimeType = "jpg".equals(fileType) || "jpeg".equals(fileType)
                ? "image/jpeg" : "image/" + fileType;
        String content = multimodalClient.prompt()
                .user(user -> user
                        .text("请忠实提取这张学习资料图片中的全部可读文字、公式、表格关系和图示含义。"
                                + "按原有结构输出纯文本；看不清的部分明确标注，不要补写不存在的内容。")
                        .media(MimeTypeUtils.parseMimeType(mimeType), new FileSystemResource(file)))
                .call()
                .content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("视觉模型未提取到有效内容");
        }
        log.info("临时图片内容提取完成: file={}, chars={}", file.getFileName(), content.length());
        return content.trim();
    }
}
