package com.study.ai.service;

import com.study.ai.MaterialContentReader;
import com.study.ai.client.AiClient;
import com.study.ai.prompt.PromptTemplates;
import com.study.ai.validator.MaterialValidator;
import com.study.common.BusinessException;
import com.study.common.Constants;
import com.study.common.UserContext;
import com.study.entity.AiChatHistory;
import com.study.entity.LearningMaterial;
import com.study.mapper.AiChatHistoryMapper;
import com.study.mapper.LearningMaterialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 文档总结服务
 * 根据学习资料内容生成 Markdown 格式的知识点总结
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private final AiClient aiClient;
    private final MaterialValidator materialValidator;
    private final LearningMaterialMapper materialMapper;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final MaterialContentReader contentReader;

    /**
     * 生成文档总结
     *
     * @param materialId 资料ID
     * @param force      是否强制重新生成
     * @return Markdown 格式的总结内容
     */
    public String generateSummary(Long materialId, boolean force) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验资料归属
        LearningMaterial material = materialValidator.validateOwnership(materialId, userId);
        if (Constants.STATUS_PROCESSING.equals(material.getStatus())) {
            throw new BusinessException(3001, "资料尚未处理完成，请稍后再试");
        }
        if (Constants.STATUS_FAILED.equals(material.getStatus())) {
            throw new BusinessException(3002, "资料处理失败，无法生成总结");
        }

        // 2. 已有摘要且未强制重新生成，直接返回
        if (material.getSummary() != null && !material.getSummary().isBlank() && !force) {
            log.info("已有摘要，直接返回: materialId={}", materialId);
            return material.getSummary();
        }

        // 3. 读取切片内容（MaterialContentReader 自动处理超长截断）
        String content = contentReader.readContent(materialId);
        if (content.isBlank()) {
            throw new BusinessException(3001, "资料内容为空，无法生成总结");
        }

        // 4. 构建 Prompt 并调用 AI
        String prompt = PromptTemplates.buildSummaryPrompt(content);
        String summary = aiClient.chat(prompt);

        // 5. 保存总结到资料表
        LearningMaterial update = new LearningMaterial();
        update.setId(materialId);
        update.setSummary(summary);
        materialMapper.updateById(update);

        // 6. 保存对话历史
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setChatType(Constants.CHAT_TYPE_SUMMARY);
        history.setMaterialId(materialId);
        history.setUserMessage("生成总结");
        history.setAiResponse(summary);
        chatHistoryMapper.insert(history);

        log.info("总结生成成功: materialId={}, summaryLen={}", materialId, summary.length());
        return summary;
    }
}
