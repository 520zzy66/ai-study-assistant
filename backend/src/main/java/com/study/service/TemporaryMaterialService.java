package com.study.service;

import com.study.dto.request.TemporaryMaterialPromoteRequest;
import com.study.entity.TemporaryMaterial;
import com.study.entity.TemporaryMaterialChunk;
import com.study.vo.TemporaryMaterialVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 会话临时资料服务。
 */
public interface TemporaryMaterialService {

    /** 上传一份会话临时资料。 */
    TemporaryMaterialVO upload(MultipartFile file, String conversationId);

    /** 查询当前用户尚未过期的临时资料。 */
    List<TemporaryMaterialVO> list();

    /** 查询临时资料详情。 */
    TemporaryMaterialVO getDetail(String uploadToken);

    /** 删除临时资料及其索引和文件。 */
    void delete(String uploadToken);

    /** 将临时资料添加到“我的资料”。 */
    Long promote(String uploadToken, TemporaryMaterialPromoteRequest request);

    /** 获取工作流可用且归属匹配的临时资料。 */
    TemporaryMaterial requireReady(Long userId, String conversationId, String uploadToken);

    /** 获取临时资料切片。 */
    List<TemporaryMaterialChunk> getChunks(Long temporaryMaterialId);

    /** 缓存 MultimodalNode 生成的摘要。 */
    void saveSummary(Long temporaryMaterialId, String summary);
}
