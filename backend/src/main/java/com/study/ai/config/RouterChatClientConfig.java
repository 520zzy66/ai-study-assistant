package com.study.ai.config;

import com.study.ai.agent.config.AgentClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Exposes the router ChatClient (agent-general) as a named bean
 * so that components like UserProfileCompressor can inject it
 * via {@code @Qualifier("routerChatClient")}.
 */
@Configuration
public class RouterChatClientConfig {

    @Bean
    public ChatClient routerChatClient(AgentClientFactory factory) {
        return factory.getOrCreate("general");
    }
}
