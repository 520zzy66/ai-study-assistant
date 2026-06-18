package com.study.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.entity.AiChatHistory;
import com.study.entity.AiQuestionBank;

import java.util.List;
import java.util.Map;

/**
 * 历史记录服务接口
 */
public interface HistoryService {

    /**
     * 分页查询对话历史
     *
     * @param chatType   对话类型（可选）
     * @param materialId 资料ID（可选）
     * @param page       页码
     * @param size       每页条数
     * @return 分页结果
     */
    Page<AiChatHistory> listChatHistory(String chatType, Long materialId, int page, int size);

    /**
     * 查询出题批次列表
     *
     * @param materialId 资料ID（可选）
     * @param page       页码
     * @param size       每页条数
     * @return 分页结果，每项包含 batchId/materialName/questionCount/createTime
     */
    Page<Map<String, Object>> listQuizBatches(Long materialId, int page, int size);

    /**
     * 查询批次题目详情
     *
     * @param batchId 批次ID
     * @return 题目列表
     */
    List<AiQuestionBank> getQuizBatchDetail(String batchId);
}
