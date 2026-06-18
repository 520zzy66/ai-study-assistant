package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.AiChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 对话历史 Mapper
 */
@Mapper
public interface AiChatHistoryMapper extends BaseMapper<AiChatHistory> {
}
