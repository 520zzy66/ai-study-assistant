package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.SystemKnowledgeImportLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统知识库导入日志 Mapper
 *
 * @author AI Study Assistant
 */
@Mapper
public interface SystemKnowledgeImportLogMapper extends BaseMapper<SystemKnowledgeImportLog> {

    /**
     * 根据文件路径查询导入记录
     *
     * @param filePath 相对路径（如 civil/xingce/逻辑判断.pdf）
     * @return 导入记录，未找到返回 null
     */
    @Select("SELECT * FROM system_knowledge_import_log " +
            "WHERE file_path = #{filePath} AND deleted = 0 LIMIT 1")
    SystemKnowledgeImportLog selectByFilePath(@Param("filePath") String filePath);

    /**
     * 统计已入库的文件数量
     *
     * @return 已入库文件数
     */
    @Select("SELECT COUNT(*) FROM system_knowledge_import_log WHERE deleted = 0")
    int countImportedFiles();

    /**
     * 按知识库根目录统计已入库文件数
     *
     * @param knowledgeRoot 知识库根目录
     * @return 文件数
     */
    @Select("SELECT COUNT(*) FROM system_knowledge_import_log " +
            "WHERE knowledge_root = #{knowledgeRoot} AND deleted = 0")
    int countByKnowledgeRoot(@Param("knowledgeRoot") String knowledgeRoot);
}
