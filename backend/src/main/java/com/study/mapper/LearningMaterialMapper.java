package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.LearningMaterial;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习资料 Mapper
 */
@Mapper
public interface LearningMaterialMapper extends BaseMapper<LearningMaterial> {
}
