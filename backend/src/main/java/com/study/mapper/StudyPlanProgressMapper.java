package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.StudyPlanProgress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习计划进度 Mapper
 */
@Mapper
public interface StudyPlanProgressMapper extends BaseMapper<StudyPlanProgress> {
}
