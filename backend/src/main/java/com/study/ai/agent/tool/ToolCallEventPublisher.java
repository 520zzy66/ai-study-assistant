package com.study.ai.agent.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Publishes real tool call events from Spring AI {@code @Tool} methods.
 *
 * <p>The listener is stored in a ThreadLocal because Spring AI invokes the tool
 * methods inside the same request execution path. When no listener is bound,
 * tool methods run normally without event overhead.</p>
 */
public final class ToolCallEventPublisher {

    private static final ThreadLocal<Consumer<Map<String, Object>>> LISTENER = new ThreadLocal<>();

    private ToolCallEventPublisher() {
    }

    /**
     * Binds a listener for the current workflow execution.
     *
     * @param listener event listener
     * @return scope that clears the listener on close
     */
    public static Scope open(Consumer<Map<String, Object>> listener) {
        LISTENER.set(listener);
        return new Scope();
    }

    /**
     * Emits a tool_call event.
     *
     * @param toolName tool name
     * @param params   tool parameters
     */
    public static void toolCall(String toolName, Map<String, Object> params) {
        Map<String, Object> event = baseEvent("tool_call");
        event.put("tool", toolName);
        event.put("params", params);
        emit(event);
    }

    /**
     * Emits a tool_result event.
     *
     * @param toolName    tool name
     * @param resultCount matched result count
     * @param durationMs  tool duration in milliseconds
     * @param error       whether the tool failed internally
     */
    public static void toolResult(String toolName, int resultCount, long durationMs, boolean error) {
        Map<String, Object> event = baseEvent("tool_result");
        event.put("tool", toolName);
        event.put("resultCount", resultCount);
        event.put("durationMs", durationMs);
        event.put("error", error);
        emit(event);
    }

    private static Map<String, Object> baseEvent(String type) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("timestamp", System.currentTimeMillis());
        return event;
    }

    private static void emit(Map<String, Object> event) {
        Consumer<Map<String, Object>> listener = LISTENER.get();
        if (listener != null) {
            try {
                listener.accept(event);
            } catch (RuntimeException ignored) {
                // Tool execution must not fail because an optional SSE listener is unavailable.
            }
        }
    }

    /**
     * Bound listener scope.
     */
    public static final class Scope implements AutoCloseable {

        private Scope() {
        }

        @Override
        public void close() {
            LISTENER.remove();
        }
    }
}
