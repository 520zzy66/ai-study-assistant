package com.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料文件夹实体
 *
 * <p>用户可创建树形文件夹结构来分类管理学习资料。
 * parent_id 为 NULL 表示根文件夹；支持无限层级嵌套。
 */
@Data
@TableName("material_folder")
public class MaterialFolder {

    /** 文件夹 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 文件夹名称 */
    private String name;

    /** 父文件夹 ID（NULL 表示根层级） */
    private Long parentId;

    /** 同级排序（越小越靠前） */
    private Integer sortOrder;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 子文件夹（非 DB 字段，Service 查询后装配） */
    @TableField(exist = false)
    private java.util.List<MaterialFolder> children;
}
