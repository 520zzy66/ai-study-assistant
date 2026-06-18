package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.UserWrongQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错题本 Mapper
 */
@Mapper
public interface UserWrongQuestionMapper extends BaseMapper<UserWrongQuestion> {
}
