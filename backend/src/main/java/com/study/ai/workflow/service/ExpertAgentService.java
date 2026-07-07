package com.study.ai.workflow.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import reactor.core.publisher.Flux;

/**
 * Expert agent service driven by Spring AI native tool calling.
 *
 * <p>Each implementation represents a domain-specific expert (Civil, Graduate, General)
 * that can autonomously call @Tool methods to retrieve knowledge and generate answers.
 */
public interface ExpertAgentService {

    /**
     * Returns the expert ID handled by this service.
     *
     * @return expert ID
     */
    String getExpertId();

    /**
     * Generates the expert answer.
     *
     * @param state global OverAllState (contains context injected by GeneralNode)
     * @return answer in Markdown
     */
    String answer(OverAllState state);

    /**
     * Generates the expert answer as a stream of tokens.
     *
     * @param state global OverAllState (contains context injected by GeneralNode)
     * @return Flux of token strings
     */
    Flux<String> answerStream(OverAllState state);
}
