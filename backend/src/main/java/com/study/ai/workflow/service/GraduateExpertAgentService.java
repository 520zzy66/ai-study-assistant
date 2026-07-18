package com.study.ai.workflow.service;

import com.study.ai.MaterialContentReader;
import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.agent.tool.KnowledgeTools;
import org.springframework.stereotype.Service;

/**
 * Graduate entrance exam expert agent.
 */
@Service
public class GraduateExpertAgentService extends AbstractExpertAgentService {

    /**
     * Creates a graduate expert service.
     *
     * @param agentClientFactory Agent ChatClient factory
     * @param knowledgeTools     native Spring AI tools
     */
    public GraduateExpertAgentService(AgentClientFactory agentClientFactory,
                                      KnowledgeTools knowledgeTools,
                                      MaterialContentReader materialContentReader) {
        super(agentClientFactory, knowledgeTools, materialContentReader, "graduate", "GRADUATE_EXPERT");
    }
}
