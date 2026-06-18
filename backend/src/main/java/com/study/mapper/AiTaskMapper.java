package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.AiTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 异步任务 Mapper
 */
@Mapper
public interface AiTaskMapper extends BaseMapper<AiTask> {
}
