package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.common.UserContext;
import com.study.entity.AiChatHistory;
import com.study.entity.AiQuestionBank;
import com.study.entity.LearningMaterial;
import com.study.mapper.AiChatHistoryMapper;
import com.study.mapper.AiQuestionBankMapper;
import com.study.mapper.LearningMaterialMapper;
import com.study.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 历史记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final AiChatHistoryMapper chatHistoryMapper;
    private final AiQuestionBankMapper questionBankMapper;
    private final LearningMaterialMapper materialMapper;

    @Override
    public Page<AiChatHistory> listChatHistory(String chatType, Long materialId, int page, int size) {
        Long userId = UserContext.getCurrentUserId();

        Page<AiChatHistory> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<AiChatHistory>()
                .eq(AiChatHistory::getUserId, userId)
                .eq(StringUtils.hasText(chatType), AiChatHistory::getChatType, chatType)
                .eq(materialId != null, AiChatHistory::getMaterialId, materialId)
                .orderByDesc(AiChatHistory::getCreateTime);

        return chatHistoryMapper.selectPage(pageReq, wrapper);
    }

    @Override
    public Page<Map<String, Object>> listQuizBatches(Long materialId, int page, int size) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 先查询去重的 batchId 列表（带分页）
        // 使用子查询方式避免 only_full_group_by 问题
        Page<AiQuestionBank> batchPage = new Page<>(page, size);
        LambdaQueryWrapper<AiQuestionBank> batchWrapper = new LambdaQueryWrapper<AiQuestionBank>()
                .select(AiQuestionBank::getBatchId)
                .eq(AiQuestionBank::getUserId, userId)
                .eq(materialId != null, AiQuestionBank::getMaterialId, materialId)
                .groupBy(AiQuestionBank::getBatchId)
                .orderByDesc(AiQuestionBank::getBatchId);  // 使用 batchId 排序代替 create_time
        Page<AiQuestionBank> batchResult = questionBankMapper.selectPage(batchPage, batchWrapper);

        List<String> batchIds = batchResult.getRecords().stream()
                .map(AiQuestionBank::getBatchId)
                .toList();

        if (batchIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        // 2. 查询这些批次的聚合数据
        List<AiQuestionBank> questions = questionBankMapper.selectList(
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getUserId, userId)
                        .in(AiQuestionBank::getBatchId, batchIds)
                        .orderByDesc(AiQuestionBank::getCreateTime));

        Map<String, List<AiQuestionBank>> grouped = questions.stream()
                .collect(Collectors.groupingBy(AiQuestionBank::getBatchId, LinkedHashMap::new, Collectors.toList()));

        // 3. 批量预加载所有关联的材料，避免 N+1 查询
        Set<Long> materialIds = questions.stream()
                .map(AiQuestionBank::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, LearningMaterial> materialMap = materialIds.isEmpty()
                ? Map.of()
                : materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(LearningMaterial::getId, m -> m));

        // 4. 构建批次列表（保持分页顺序）
        List<Map<String, Object>> batches = new ArrayList<>();
        for (String batchId : batchIds) {
            List<AiQuestionBank> batchQuestions = grouped.get(batchId);
            if (batchQuestions == null || batchQuestions.isEmpty()) continue;

            AiQuestionBank first = batchQuestions.get(0);
            Map<String, Object> batch = new LinkedHashMap<>();
            batch.put("batchId", batchId);
            batch.put("materialId", first.getMaterialId());
            batch.put("questionCount", batchQuestions.size());
            batch.put("createTime", first.getCreateTime());

            LearningMaterial material = materialMap.get(first.getMaterialId());
            batch.put("materialName", material != null ? material.getOriginalName() : "未知资料");

            batches.add(batch);
        }

        Page<Map<String, Object>> result = new Page<>(page, size, batchResult.getTotal());
        result.setRecords(batches);
        return result;
    }

    @Override
    public List<AiQuestionBank> getQuizBatchDetail(String batchId) {
        Long userId = UserContext.getCurrentUserId();

        return questionBankMapper.selectList(
                new LambdaQueryWrapper<AiQuestionBank>()
                        .eq(AiQuestionBank::getBatchId, batchId)
                        .eq(AiQuestionBank::getUserId, userId)
                        .orderByAsc(AiQuestionBank::getId)
        );
    }
}
