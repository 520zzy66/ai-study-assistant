package com.study.ai.agent.resource;

import lombok.Data;

/**
 * 资源包资源清单项
 */
@Data
public class ResourceManifestItem {
    private String key;       // summary/mindMap/quiz/plan/multimodalScript
    private String title;
    private String type;      // summary/mind_map/quiz/study_path/multimodal_script
    private Boolean requested;
    private Boolean generated;
    private String status;    // success/failed/skipped
    private String errorCode;
    private String message;
}
