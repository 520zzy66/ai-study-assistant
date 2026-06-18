package com.study.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.common.Result;
import com.study.entity.AiChatHistory;
import com.study.entity.AiQuestionBank;
import com.study.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 历史记录控制器
 */
@Tag(name = "历史记录", description = "对话历史、出题记录查询接口")
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 对话历史列表
     */
    @Operation(summary = "对话历史列表", description = "分页查询当前用户的 AI 对话历史记录")
    @GetMapping("/chat")
    public Result<Page<AiChatHistory>> listChatHistory(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long materialId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AiChatHistory> result = historyService.listChatHistory(type, materialId, page, size);
        return Result.success(result);
    }

    /**
     * 出题记录列表（按批次聚合）
     */
    @Operation(summary = "出题记录列表", description = "分页查询当前用户的出题批次记录")
    @GetMapping("/quiz")
    public Result<Page<Map<String, Object>>> listQuizBatches(
            @RequestParam(required = false) Long materialId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Map<String, Object>> result = historyService.listQuizBatches(materialId, page, size);
        return Result.success(result);
    }

    /**
     * 出题批次详情
     */
    @Operation(summary = "出题批次详情", description = "查询指定批次的题目详情")
    @GetMapping("/quiz/{batchId}")
    public Result<List<AiQuestionBank>> getQuizBatchDetail(@PathVariable String batchId) {
        List<AiQuestionBank> questions = historyService.getQuizBatchDetail(batchId);
        return Result.success(questions);
    }
}
