package com.study.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.dto.request.MaterialListRequest;
import com.study.vo.MaterialUploadVO;
import com.study.vo.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 学习资料服务接口
 */
public interface MaterialService {

    /**
     * 上传文件
     *
     * @param file     文件
     * @param category 分类（可选）
     * @return 上传结果
     */
    MaterialUploadVO upload(MultipartFile file, String category);

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
}
