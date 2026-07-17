package com.study.ai.agent.core;

/**
 * 统一定义 Agent 机器名、展示名和动作常量
 */
public interface AgentNames {
    
    // ResourceOrchestratorAgent
    String RESOURCE_ORCHESTRATOR = "ResourceOrchestratorAgent";
    String RESOURCE_ORCHESTRATOR_DISPLAY = "资源编排 Agent";
    String ACTION_ORCHESTRATE = "orchestrate_resource_package";

    // ProfileAgent
    String PROFILE_AGENT = "ProfileAgent";
    String PROFILE_AGENT_DISPLAY = "画像分析 Agent";
    String ACTION_ANALYZE_PROFILE = "analyze_profile";

    // SummaryAgent
    String SUMMARY_AGENT = "SummaryAgent";
    String SUMMARY_AGENT_DISPLAY = "课程理解 Agent";
    String ACTION_GENERATE_SUMMARY = "generate_summary";
    String ACTION_GENERATE_MIND_MAP = "generate_mind_map";

    // QuizAgent
    String QUIZ_AGENT = "QuizAgent";
    String QUIZ_AGENT_DISPLAY = "题库生成 Agent";
    String ACTION_GENERATE_QUIZ = "generate_quiz";

    // PlanAgent
    String PLAN_AGENT = "PlanAgent";
    String PLAN_AGENT_DISPLAY = "路径规划 Agent";
    String ACTION_GENERATE_STUDY_PATH = "generate_study_path";

    // MultimodalAgent
    String MULTIMODAL_AGENT = "MultimodalAgent";
    String MULTIMODAL_AGENT_DISPLAY = "多模态脚本 Agent";
    String ACTION_GENERATE_MULTIMODAL_SCRIPT = "generate_multimodal_script";

    // SafetyAgent
    String SAFETY_AGENT = "SafetyAgent";
    String SAFETY_AGENT_DISPLAY = "质量安全 Agent";
    String ACTION_VALIDATE_PACKAGE = "validate_package";
}
