package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.StudyPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习计划 Mapper
 */
@Mapper
public interface StudyPlanMapper extends BaseMapper<StudyPlan> {
}
