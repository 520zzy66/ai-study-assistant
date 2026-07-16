package com.study.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.dto.request.MaterialListRequest;
import com.study.vo.MaterialUploadVO;
import com.study.vo.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学习资料服务接口
 */
public interface MaterialService {

    /**
     * 上传文件
     *
     * @param file     文件
     * @param category 分类（可选）
     * @param folderId 目标文件夹ID（可选，null表示未分类）
     * @return 上传结果
     */
    MaterialUploadVO upload(MultipartFile file, String category, Long folderId);

    /**
     * 分页查询当前用户的资料列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    Page<MaterialVO> list(MaterialListRequest request);

    /**
     * 获取资料详情（含摘要）
     *
     * @param id 资料ID
     * @return 资料详情
     */
    MaterialVO getDetail(Long id);

    /**
     * 预览或下载资料文件
     *
     * @param id 资料ID
     * @return Resource 文件资源
     */
    org.springframework.core.io.Resource preview(Long id);

    /**
     * 删除资料（逻辑删除）
     *
     * @param id 资料ID
     */
    void delete(Long id);

    /**
     * 重新处理失败的资料
     *
     * @param id 资料ID
     */
    void retryProcess(Long id);

    /**
     * 查询系统资料库（公共资料）
     *
     * @param keyword  搜索关键词（可选）
     * @param category 分类筛选（可选）
     * @param page     页码
     * @param size     每页大小
     * @return 分页结果
     */
    Page<MaterialVO> listLibrary(String keyword, String category, int page, int size);

    /**
     * 将系统资料库的资料复制到当前用户的资料库
     *
     * @param libraryId 系统资料ID
     * @return 复制后的新资料ID
     */
    Long copyToMyLibrary(Long libraryId);

    /**
     * 获取所有可用资料（用户自己的 + 系统资料库），用于 AI 功能下拉选择
     *
     * @return 资料列表
     */
    List<MaterialVO> listAvailable();

    /**
     * 批量移动资料到文件夹
     *
     * @param userId      当前用户ID
     * @param materialIds 资料ID列表
     * @param folderId    目标文件夹ID（null表示移出文件夹）
     */
    void moveMaterials(Long userId, List<Long> materialIds, Long folderId);
}
