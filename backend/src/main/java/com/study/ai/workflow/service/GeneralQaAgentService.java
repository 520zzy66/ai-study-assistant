package com.study.ai.workflow.service;

import com.study.ai.MaterialContentReader;
import com.study.ai.agent.config.AgentClientFactory;
import com.study.ai.agent.tool.KnowledgeTools;
import org.springframework.stereotype.Service;

/**
 * General QA agent used as the workflow fallback expert.
 */
@Service
public class GeneralQaAgentService extends AbstractExpertAgentService {

    /**
     * Creates a general QA expert service.
     *
     * @param agentClientFactory Agent ChatClient factory
     * @param knowledgeTools     native Spring AI tools
     */
    public GeneralQaAgentService(AgentClientFactory agentClientFactory,
                                 KnowledgeTools knowledgeTools,
                                 MaterialContentReader materialContentReader) {
        super(agentClientFactory, knowledgeTools, materialContentReader, "general-qa", "GENERAL_EXPERT");
    }
}
