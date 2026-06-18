package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.study.ai.parser.ChunkSplitter;
import com.study.ai.parser.DocumentParser;
import com.study.entity.LearningMaterial;
import com.study.entity.MaterialChunk;
import com.study.mapper.LearningMaterialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档异步处理器
 * 独立 Bean，确保 @Async 代理生效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAsyncProcessor {

    private final LearningMaterialMapper materialMapper;
    private final DocumentParser documentParser;
    private final ChunkSplitter chunkSplitter;

    /** 最大重试次数 */
    private static final int MAX_RETRY = 3;

    /**
     * 异步处理文档：解析 → 切片 → 保存切片
     * 不使用 @Transactional：切片逐条 auto-commit，失败时部分切片可残留（status=failed 标记）。
     *
     * @param materialId 资料ID
     * @param filePath   文件磁盘路径
     * @param fileType   文件类型
     * @param userId     用户ID（直接传入，避免事务竞态读不到数据）
     */
    @Async
    public void processMaterial(Long materialId, String filePath, String fileType, Long userId) {
        doProcess(materialId, filePath, fileType, userId);
    }

    /**
     * 手动重试失败的材料
     *
     * @param materialId 资料ID
     */
    @Async
    public void retryProcess(Long materialId) {
        LearningMaterial material = materialMapper.selectById(materialId);
        if (material == null) {
            log.warn("重试失败，资料不存在: materialId={}", materialId);
            return;
        }
        if (!"failed".equals(material.getStatus())) {
            log.warn("资料状态非 failed，跳过重试: materialId={}, status={}", materialId, material.getStatus());
            return;
        }

        log.info("手动重试文档处理: materialId={}", materialId);
        doProcess(materialId, material.getFilePath(), material.getFileType(), material.getUserId());
    }

    /**
     * 定时扫描失败材料并自动重试
     * 每 30 分钟执行一次
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void autoRetryFailedMaterials() {
        log.debug("定时扫描失败材料...");
        List<LearningMaterial> failedList = materialMapper.selectList(
                new LambdaQueryWrapper<LearningMaterial>()
                        .eq(LearningMaterial::getStatus, "failed")
                        .ge(LearningMaterial::getCreateTime,
                                LocalDateTime.now().minusHours(24))  // 只重试 24 小时内的
        );

        int retried = 0;
        for (LearningMaterial material : failedList) {
            // 检查重试次数（通过 errorMsg 前缀判断，避免加字段）
            String errorMsg = material.getErrorMsg();
            int retryCount = countRetryPrefix(errorMsg);
            if (retryCount >= MAX_RETRY) {
                log.info("材料已达最大重试次数，跳过: materialId={}", material.getId());
                continue;
            }

            log.info("自动重试: materialId={}, retry={}/{}", material.getId(), retryCount + 1, MAX_RETRY);
            // 更新 errorMsg 标记重试次数
            updateStatus(material.getId(), "parsing",
                    "[自动重试 " + (retryCount + 1) + "/" + MAX_RETRY + "] " +
                    (errorMsg != null ? errorMsg : ""));
            doProcess(material.getId(), material.getFilePath(),
                    material.getFileType(), material.getUserId());
            retried++;
        }

        if (retried > 0) {
            log.info("定时扫描完成，已重试 {} 条失败材料", retried);
        }
    }

    /**
     * 从 errorMsg 中提取重试次数
     */
    private int countRetryPrefix(String errorMsg) {
        if (errorMsg == null) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = errorMsg.indexOf("[自动重试 ", idx)) != -1) {
            count++;
            idx++;
        }
        return count;
    }

    /**
     * 执行处理的核心逻辑
     */
    private void doProcess(Long materialId, String filePath, String fileType, Long userId) {
        log.info("开始处理文档: materialId={}", materialId);

        // 并发保护：原子更新状态为 parsing，只有非 parsing/ready 状态的记录才会被更新
        // 避免 @Scheduled 与 @Async 并发处理同一材料的 TOCTOU 竞态
        int affected = materialMapper.update(null, new LambdaUpdateWrapper<LearningMaterial>()
                .eq(LearningMaterial::getId, materialId)
                .ne(LearningMaterial::getStatus, "parsing")
                .ne(LearningMaterial::getStatus, "ready")
                .set(LearningMaterial::getStatus, "parsing"));

        if (affected == 0) {
            log.warn("材料已在处理中或已就绪，跳过重复处理: materialId={}", materialId);
            return;
        }

        // 读取当前记录（包含 errorMsg 用于保留重试计数前缀）
        LearningMaterial current = materialMapper.selectById(materialId);

        try {

            // 2. 解析文档为纯文本
            String text;
            try (InputStream input = Files.newInputStream(Paths.get(filePath))) {
                text = documentParser.parse(input, fileType);
            }

            if (text == null || text.isBlank()) {
                updateStatus(materialId, "failed", "文档内容为空");
                return;
            }

            // 3. 切片
            List<String> chunks = chunkSplitter.split(text);
            if (chunks.isEmpty()) {
                updateStatus(materialId, "failed", "文档切片失败");
                return;
            }

            // 4. 批量保存切片到数据库
            // 注意：Db.saveBatch 通过静态工具类自行获取 SqlSession，
            // 不参与 Spring 托管事务，适合当前无 @Transactional 的 @Async 方法。
            // 若未来加 @Transactional，需改用注入的 Mapper 以保持事务一致性。
            List<MaterialChunk> chunkEntities = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                MaterialChunk chunk = new MaterialChunk();
                chunk.setMaterialId(materialId);
                chunk.setUserId(userId);
                chunk.setChunkIndex(i);
                chunk.setContent(chunks.get(i));
                chunk.setChunkSize(chunks.get(i).length());
                chunkEntities.add(chunk);
            }
            Db.saveBatch(chunkEntities);

            // 5. 更新状态为 ready，记录切片数
            LearningMaterial update = new LearningMaterial();
            update.setId(materialId);
            update.setStatus("ready");
            update.setChunkCount(chunks.size());
            update.setErrorMsg(null);
            materialMapper.updateById(update);

            log.info("文档处理完成: materialId={}, chunkCount={}", materialId, chunks.size());

        } catch (Exception e) {
            log.error("文档处理失败: materialId={}", materialId, e);
            try {
                // 保留 errorMsg 中已有的重试前缀，追加当前错误信息
                String existing = current != null ? current.getErrorMsg() : null;
                String retryPrefix = "";
                if (existing != null && existing.contains("[自动重试")) {
                    // 提取已有的重试前缀（如 "[自动重试 1/3] 处理失败: ..." → "[自动重试 1/3] "）
                    int idx = existing.indexOf("[自动重试");
                    int end = existing.indexOf("] ", idx);
                    if (end > 0) retryPrefix = existing.substring(idx, end + 2);
                }
                String errorMsg = (retryPrefix.isEmpty() ? "" : retryPrefix)
                        + truncate("处理失败: " + e.getMessage(), 500 - retryPrefix.length());
                updateStatus(materialId, "failed", errorMsg);
            } catch (Exception ex) {
                log.error("更新失败状态也失败: materialId={}", materialId, ex);
            }
        }
    }

    /**
     * 更新资料状态
     */
    private void updateStatus(Long materialId, String status, String errorMsg) {
        LearningMaterial update = new LearningMaterial();
        update.setId(materialId);
        update.setStatus(status);
        update.setErrorMsg(errorMsg);
        materialMapper.updateById(update);
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
