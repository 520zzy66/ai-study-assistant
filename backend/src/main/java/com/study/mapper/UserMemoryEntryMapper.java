package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.UserMemoryEntry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户记忆条目 Mapper
 */
@Mapper
public interface UserMemoryEntryMapper extends BaseMapper<UserMemoryEntry> {
}
