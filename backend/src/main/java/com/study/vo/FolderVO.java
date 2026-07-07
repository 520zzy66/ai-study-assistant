package com.study.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资料文件夹 VO（树形结构）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderVO {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private List<FolderVO> children;
}
