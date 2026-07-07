package com.study.ai.workflow.service;

import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.agent.tool.KnowledgeTools;
import org.springframework.stereotype.Service;

/**
 * Civil service exam expert agent.
 */
@Service
public class CivilExpertAgentService extends AbstractExpertAgentService {

    /**
     * Creates a civil expert service.
     *
     * @param agentClientFactory Agent ChatClient factory
     * @param knowledgeTools     native Spring AI tools
     */
    public CivilExpertAgentService(AgentClientFactory agentClientFactory, KnowledgeTools knowledgeTools) {
        super(agentClientFactory, knowledgeTools, "civil", "CIVIL_EXPERT");
    }
}
