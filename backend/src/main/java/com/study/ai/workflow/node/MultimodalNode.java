package com.study.ai.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.workflow.graph.RouteKeys;
import com.study.entity.TemporaryMaterial;
import com.study.entity.TemporaryMaterialChunk;
import com.study.service.TemporaryMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 临时资料预处理节点。
 *
 * <p>该节点只接受服务端签发的临时资料令牌，验证用户与会话归属，首次提问时生成并缓存
 * 结构化摘要，然后把摘要和元信息注入 state。正式学习资料不会经过本节点。</p>
 */
@Slf4j
@Component
public class MultimodalNode implements NodeAction {

    private static final int MAX_SUMMARY_INPUT = 8000;

    private final ChatClient multimodalClient;
    private final TemporaryMaterialService temporaryMaterialService;

    public MultimodalNode(AgentClientFactory factory,
                          TemporaryMaterialService temporaryMaterialService) {
        this.multimodalClient = factory.getOrCreate("multimodal");
        this.temporaryMaterialService = temporaryMaterialService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String token = state.value(RouteKeys.TEMPORARY_MATERIAL_TOKEN, "");
        if (token.isBlank()) {
            return Map.of();
        }
        Long userId = parseLong(state.value(RouteKeys.USER_ID));
        String conversationId = state.value(RouteKeys.CONVERSATION_ID, "");
        String query = state.value(RouteKeys.QUERY, "");
        TemporaryMaterial material = temporaryMaterialService.requireReady(
                userId, conversationId, token);

        String summary = material.getSummary();
        if (summary == null || summary.isBlank()) {
            summary = generateAndCacheSummary(material, query);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(RouteKeys.MATERIAL_SUMMARY, summary);
        result.put(RouteKeys.MATERIAL_META, String.format(
                "文件名：%s；类型：%s；大小：%d字节；临时资料；到期时间：%s",
                material.getOriginalName(), material.getFileType(), material.getFileSize(),
                material.getExpiresAt()));
        log.info("[MultimodalNode] 临时资料上下文就绪: token={}, summaryChars={}",
                token, summary.length());
        return result;
    }

    private String generateAndCacheSummary(TemporaryMaterial material, String query) {
        List<TemporaryMaterialChunk> chunks = temporaryMaterialService.getChunks(material.getId());
        StringBuilder content = new StringBuilder();
        for (TemporaryMaterialChunk chunk : chunks) {
            if (content.length() >= MAX_SUMMARY_INPUT) break;
            int remaining = MAX_SUMMARY_INPUT - content.length();
            String text = chunk.getContent();
            content.append(text, 0, Math.min(text.length(), remaining)).append('\n');
        }
        String prompt = "用户问题：" + query + "\n\n"
                + "资料文件名：" + material.getOriginalName() + "\n"
                + "资料类型：" + material.getFileType() + "\n\n"
                + "已提取资料内容：\n" + content;
        String summary = multimodalClient.prompt().user(prompt).call().content();
        if (summary == null || summary.isBlank()) {
            summary = "临时资料《" + material.getOriginalName() + "》已完成解析，可通过临时资料检索工具查询。";
        }
        summary = summary.trim();
        temporaryMaterialService.saveSummary(material.getId(), summary);
        return summary;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
