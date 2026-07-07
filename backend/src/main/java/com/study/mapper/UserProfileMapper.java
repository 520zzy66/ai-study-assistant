package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户画像 Mapper
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
