package com.study.ai.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.parser.DocumentParser;
import com.study.entity.LearningMaterial;
import com.study.mapper.LearningMaterialMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

/**
 * 多模态预处理节点 — 职责：忠实还原用户对资料的理解需求 + 资料详细信息
 *
 * <p><b>不负责</b>：领域分类、关键词提炼、路由判断（这些交给 GeneralNode）。
 *
 * <p>输出到 state：
 * <pre>
 *   query            → 用户原始输入（保留，供下游专家节点使用）
 *   materialText     → 文档完整文本（供 GeneralNode 向量化）
 *   materialSummary  → 文档详细描述（结构化自然语言，含标题、类型、字数、结构、核心知识点）
 *   materialMeta     → 文档元信息（文件名、类型、大小等，JSON 字符串）
 * </pre>
 *
 * <p>ChatClient 由 {@link AgentClientFactory} 根据 agent-multimodal.yml 创建，
 * 配置独立于主对话 ChatClient（temperature=0.5）。
 */
@Slf4j
@Component
public class MultimodalNode implements NodeAction {

    /** 多模态 Agent 的 ChatClient（由 AgentClientFactory 从 agent-multimodal.yml 创建） */
    private final ChatClient multimodalClient;
    private final LearningMaterialMapper materialMapper;
    private final DocumentParser documentParser;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public MultimodalNode(AgentClientFactory factory,
                          LearningMaterialMapper materialMapper,
                          DocumentParser documentParser) {
        // 注意：getOrCreate 参数是文件名 agent-{id}.yml 的 {id} 部分，对应 agent-multimodal.yml
        this.multimodalClient = factory.getOrCreate("multimodal");
        this.materialMapper = materialMapper;
        this.documentParser = documentParser;
        log.info("MultimodalNode 初始化完成，ChatClient 已从 agent-multimodal.yml 加载");
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 从 state 中获取用户输入
        String query = state.value("query", "");
        Long materialId = state.value("materialId", 0L);

        if (materialId == null || materialId <= 0) {
            // 无文件上传：跳过文档处理，直接透传 query
            return Map.of("query", query != null ? query : "");
        }

        LearningMaterial material = materialMapper.selectById(materialId);
        if (material == null) {
            log.warn("资料不存在：materialId={}", materialId);
            return Map.of("query", query != null ? query : "");
        }

        // 读取上传文件的磁盘内容
        Path fullPath = Paths.get(uploadDir, material.getFilePath());
        String text;
        try (InputStream input = Files.newInputStream(fullPath)) {
            text = documentParser.parse(input, material.getFileType());
        }

        // 调用多模态 Agent 生成资料结构化描述（不含领域判断）
        String materialSummary = generateMaterialSummary(query, text, material);

        // 资料元信息
        String materialMeta = String.format("{\"fileName\":\"%s\",\"fileType\":\"%s\",\"charCount\":%d,\"materialId\":%d}",
                material.getOriginalName(), material.getFileType(), text.length(), materialId);

        log.info("多模态处理完成：fileName={}, charCount={}, summaryLen={}",
                material.getOriginalName(), text.length(), materialSummary.length());

        return Map.of(
                "query", query != null ? query : "",
                "materialText", text,
                "materialSummary", materialSummary,
                "materialMeta", materialMeta
        );
    }

    /**
     * 调用 LLM 生成资料详细描述（包含文档结构、核心知识点、关键概念等，不含领域判断）
     *
     * @param query     用户原始问题（帮助 LLM 理解用户对资料的关注点）
     * @param text      资料全文
     * @param material  资料数据库记录
     * @return 结构化的资料描述文本
     */
    private String generateMaterialSummary(String query, String text, LearningMaterial material) {
        // 限制 prompt 长度（避免上下文爆炸）
        String truncatedText = text.length() > 8000 ? text.substring(0, 8000) + "\n...(后续内容省略)" : text;

        String userPrompt = "用户原始问题：" + (query != null && !query.isBlank() ? query : "（用户未输入问题）") + "\n\n"
                + "资料文件名：" + material.getOriginalName() + "\n"
                + "资料类型：" + material.getFileType() + "\n"
                + "资料字数：" + text.length() + "\n\n"
                + "资料全文：\n" + truncatedText + "\n\n"
                + "请生成一段 150-300 字的详细资料描述，包含：\n"
                + "1. 资料标题/主题（从内容推断）\n"
                + "2. 资料整体结构（章节/段落分布）\n"
                + "3. 核心知识点/概念（列出 3-7 个）\n"
                + "4. 用户可能关注的内容（结合用户原始问题推断，如用户明确问某一类问题则重点描述相关部分）\n\n"
                + "注意：不要做领域分类（如考公、考研），只忠实描述资料本身内容和结构。";

        try {
            return multimodalClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content()
                    .trim();
        } catch (Exception e) {
            log.warn("多模态 Agent 调用失败，使用资料原文前 200 字作为描述：{}", e.getMessage());
            return String.format("【用户问题】%s\n【资料详情】文件名《%s》，类型 %s，共 %d 字，内容节选：%s",
                    query, material.getOriginalName(), material.getFileType(), text.length(),
                    text.substring(0, Math.min(200, text.length())));
        }
    }
}
