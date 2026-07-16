package com.study.ai.service;

import com.study.ai.MaterialContentReader;
import com.study.ai.agent.tool.QwenTools;
import com.study.ai.client.AiClient;
import com.study.ai.prompt.PromptTemplates;
import com.study.ai.validator.MaterialValidator;
import com.study.common.BusinessException;
import com.study.common.Constants;
import com.study.common.UserContext;
import com.study.entity.AiChatHistory;
import com.study.entity.LearningMaterial;
import com.study.entity.MaterialFolder;
import com.study.mapper.AiChatHistoryMapper;
import com.study.mapper.LearningMaterialMapper;
import com.study.mapper.MaterialFolderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

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
    private final QwenTools qwenTools;
    private final AiToolService aiToolService;
    private final MaterialFolderMapper folderMapper;

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
        validateMaterialStatus(material);

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
        saveSummary(materialId, summary);

        // 6. 保存对话历史
        saveChatHistory(userId, materialId, "生成总结", summary);

        log.info("总结生成成功: materialId={}, summaryLen={}", materialId, summary.length());
        return summary;
    }

    /**
     * 流式生成文档总结（SSE）
     * 使用 ChatClient.stream() 逐 token 输出，实现打字机效果
     *
     * @param materialId 资料ID
     * @return 流式文本片段
     */
    public Flux<String> generateSummaryStream(Long materialId) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验资料归属和状态
        LearningMaterial material = materialValidator.validateOwnership(materialId, userId);
        validateMaterialStatus(material);

        // 2. 读取切片内容
        String content = contentReader.readContent(materialId);
        if (content.isBlank()) {
            throw new BusinessException(3001, "资料内容为空，无法生成总结");
        }

        // 3. 构建 Prompt
        String prompt = PromptTemplates.buildSummaryPrompt(content);

        // 4. 流式调用 AI，完成后保存总结
        StringBuilder fullSummary = new StringBuilder();
        return aiClient.chatStream(prompt)
                .doOnNext(token -> fullSummary.append(token))
                .doOnComplete(() -> {
                    String summary = fullSummary.toString();
                    if (!summary.isBlank()) {
                        saveSummary(materialId, summary);
                        saveChatHistory(userId, materialId, "生成总结（流式）", summary);
                        log.info("流式总结完成: materialId={}, summaryLen={}", materialId, summary.length());
                    }
                })
                .doOnError(e -> log.error("流式总结失败: materialId={}", materialId, e));
    }

    /**
     * 生成思维导图
     * 调用 QwenTools 生成思维导图 JSON 并保存到数据库
     *
     * @param materialId 资料ID
     * @return 思维导图 JSON 字符串
     */
    public String generateMindMap(Long materialId) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验资料归属
        LearningMaterial material = materialValidator.validateOwnership(materialId, userId);
        validateMaterialStatus(material);

        // 2. 已有思维导图，直接返回
        if (material.getMindMap() != null && !material.getMindMap().isBlank()) {
            log.info("已有思维导图，直接返回: materialId={}", materialId);
            return material.getMindMap();
        }

        // 3. 调用 QwenTools 生成思维导图
        log.info("开始生成思维导图: materialId={}", materialId);
        String mindMapJson = qwenTools.generateMindMap(materialId);

        // 4. 保存到数据库
        LearningMaterial update = new LearningMaterial();
        update.setId(materialId);
        update.setMindMap(mindMapJson);
        materialMapper.updateById(update);

        log.info("思维导图生成成功: materialId={}", materialId);
        return mindMapJson;
    }

    /**
     * 获取已生成的思维导图
     *
     * @param materialId 资料ID
     * @return 思维导图 JSON，未生成时返回 null
     */
    public String getMindMap(Long materialId) {
        Long userId = UserContext.getCurrentUserId();
        LearningMaterial material = materialValidator.validateOwnership(materialId, userId);
        return material.getMindMap();
    }

    /**
     * 生成文件夹总结
     * 从向量数据库读取该文件夹下所有资料的切片内容，由AI做综合总结
     *
     * @param folderId 文件夹ID
     * @param force    是否强制重新生成
     * @return Markdown 格式的总结内容
     */
    public String generateFolderSummary(Long folderId, boolean force) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 读取文件夹下所有资料的切片内容
        String content = contentReader.readFolderContent(folderId, userId);
        if (content.isBlank()) {
            throw new BusinessException(3001, "文件夹下没有可用资料或资料内容为空");
        }

        // 2. 获取文件夹下的资料ID列表（用于保存历史记录）
        List<Long> materialIds = contentReader.getFolderMaterialIds(folderId, userId);

        // 3. 构建 Prompt 并调用 AI
        String prompt = PromptTemplates.buildSummaryPrompt(content);
        String summary = aiClient.chat(prompt);

        // 4. 保存对话历史（关联到第一个资料，或使用 folderId 作为标识）
        Long targetMaterialId = materialIds.isEmpty() ? null : materialIds.get(0);
        saveChatHistory(userId, targetMaterialId, "生成文件夹总结: folderId=" + folderId, summary);

        // 5. 保存到文件夹表
        MaterialFolder update = new MaterialFolder();
        update.setId(folderId);
        update.setSummary(summary);
        folderMapper.updateById(update);

        log.info("文件夹总结生成成功: folderId={}, materials={}, summaryLen={}",
                folderId, materialIds.size(), summary.length());
        return summary;
    }

    /**
     * 生成文件夹思维导图
     */
    public String generateFolderMindMap(Long folderId) {
        Long userId = UserContext.getCurrentUserId();

        MaterialFolder folder = folderMapper.selectById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(404, "文件夹不存在或无权限");
        }

        if (folder.getMindMap() != null && !folder.getMindMap().isBlank()) {
            log.info("已有文件夹思维导图，直接返回: folderId={}", folderId);
            return folder.getMindMap();
        }

        log.info("开始生成文件夹思维导图: folderId={}", folderId);
        String mindMapJson = aiToolService.generateFolderMindMap(folderId);

        MaterialFolder update = new MaterialFolder();
        update.setId(folderId);
        update.setMindMap(mindMapJson);
        folderMapper.updateById(update);

        log.info("文件夹思维导图生成成功: folderId={}", folderId);
        return mindMapJson;
    }

    /**
     * 获取已生成的文件夹思维导图
     */
    public String getFolderMindMap(Long folderId) {
        Long userId = UserContext.getCurrentUserId();
        MaterialFolder folder = folderMapper.selectById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(404, "文件夹不存在或无权限");
        }
        return folder.getMindMap();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验资料状态是否可生成总结
     */
    private void validateMaterialStatus(LearningMaterial material) {
        if (Constants.STATUS_PROCESSING.equals(material.getStatus())) {
            throw new BusinessException(3001, "资料尚未处理完成，请稍后再试");
        }
        if (Constants.STATUS_FAILED.equals(material.getStatus())) {
            throw new BusinessException(3002, "资料处理失败，无法生成总结");
        }
    }

    /**
     * 保存总结到资料表
     */
    private void saveSummary(Long materialId, String summary) {
        LearningMaterial update = new LearningMaterial();
        update.setId(materialId);
        update.setSummary(summary);
        materialMapper.updateById(update);
    }

    /**
     * 保存对话历史
     */
    private void saveChatHistory(Long userId, Long materialId, String userMessage, String aiResponse) {
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setChatType(Constants.CHAT_TYPE_SUMMARY);
        history.setMaterialId(materialId);
        history.setUserMessage(userMessage);
        history.setAiResponse(aiResponse);
        chatHistoryMapper.insert(history);
    }
}
