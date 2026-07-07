package com.study.ai.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.study.ai.workflow.graph.RouteKeys;
import com.study.ai.workflow.service.ExpertAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 专家 Agent 节点 — 一个节点，内部路由到三个专家 Agent Service。
 *
 * <p>根据 state 中的 routedExpert 字段选择对应的 ExpertAgentService：
 * <ul>
 *   <li>CIVIL_EXPERT → CivilExpertAgentService</li>
 *   <li>GRADUATE_EXPERT → GraduateExpertAgentService</li>
 *   <li>GENERAL_EXPERT → GeneralQaAgentService</li>
 * </ul>
 *
 * <p>ExpertAgentService 自主调用 @Tool（KnowledgeTools）进行 RAG 检索，
 * Spring AI 自动 ReAct 循环完成最终回答。
 */
@Slf4j
@Component
public class ExpertAgentNode implements NodeAction {

    private static final String FALLBACK_ANSWER = "专业分析暂时不可用，请稍后重试。你也可以补充资料或缩小问题范围后再次提问。";

    private final Map<String, ExpertAgentService> expertServices;

    public ExpertAgentNode(List<ExpertAgentService> expertServices) {
        this.expertServices = expertServices.stream()
                .collect(Collectors.toMap(ExpertAgentService::getExpertId, s -> s));
        log.info("[ExpertAgentNode] 已注册专家服务：{}", expertServices.stream()
                .map(ExpertAgentService::getExpertId).collect(Collectors.joining(", ")));
    }

    /**
     * 获取指定专家服务实例。
     *
     * @param expertId 专家 ID
     * @return 专家服务实例，如果不存在则返回 null
     */
    public ExpertAgentService getExpertService(String expertId) {
        return expertServices.get(expertId);
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String routedExpert = state.value(RouteKeys.ROUTED_EXPERT, "GENERAL_EXPERT");
        ExpertAgentService service = expertServices.get(routedExpert);

        if (service == null) {
            log.warn("[ExpertAgentNode] 未找到专家服务：{}", routedExpert);
            return Map.of(
                    RouteKeys.ANSWER, FALLBACK_ANSWER,
                    RouteKeys.FALLBACK, true,
                    RouteKeys.ERROR_MESSAGE, "未找到专家服务：" + routedExpert
            );
        }

        try {
            log.info("[ExpertAgentNode] 调用专家服务：{}", routedExpert);
            String answer = service.answer(state);
            return Map.of(RouteKeys.ANSWER, answer);
        } catch (Exception e) {
            log.warn("[ExpertAgentNode] 专家执行失败：expert={}, error={}", routedExpert, e.getMessage());
            return Map.of(
                    RouteKeys.ANSWER, FALLBACK_ANSWER,
                    RouteKeys.FALLBACK, true,
                    RouteKeys.ERROR_MESSAGE, e.getMessage()
            );
        }
    }
}
