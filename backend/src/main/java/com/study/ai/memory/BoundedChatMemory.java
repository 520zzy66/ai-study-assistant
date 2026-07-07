package com.study.ai.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 有界对话记忆实现
 * 委托给底层 ChatMemory，同时限制最大 conversationId 数量，防止 OOM
 */
@Slf4j
public class BoundedChatMemory implements ChatMemory {

    /** 最大并发会话数，超出后 LRU 驱逐旧会话 */
    static final int MAX_CONVERSATIONS = 1000;

    private final ChatMemory delegate;

    /**
     * LRU 记录 conversationId 访问顺序。
     * 使用 synchronizedMap 保证线程安全，但注意 put/get 之间的复合操作（如
     * add() 中 lru.put + delegate.add）仍是非原子的 — 这在当前场景可接受。
     *
     * <p><b>并发风险说明：</b>
     * <ul>
     *   <li>add() 中 delegate.add() 和 lru.put() 之间存在小窗口竞态，
     *       极端情况下可能导致会话被意外驱逐或短暂不可读。</li>
     *   <li>get() 中 delegate.get() 和 lru.get() 之间也有类似窗口。</li>
     *   <li>在当前 AI 对话场景下，这种低概率的竞态不会导致数据丢失或业务错误，
     *       仅可能偶尔多驱逐一个会话，用户重新提问即可恢复。</li>
     *   <li>如需更高一致性保证，应考虑使用分布式锁或 Redis 事务。</li>
     * </ul>
     */
    private final Map<String, Boolean> lru = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    if (size() > MAX_CONVERSATIONS) {
                        try {
                            delegate.clear(eldest.getKey());
                        } catch (Exception ignored) {
                            // delegate.clear 异常时仍需驱逐，防止 LRU 无限增长
                            log.warn("驱逐旧会话时 delegate.clear 失败: {}", eldest.getKey(), ignored);
                        }
                        return true;
                    }
                    return false;
                }
            });

    public BoundedChatMemory(ChatMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 先写入 delegate，再标记 LRU — 避免 delegate 异常留下幽灵 LRU 条目
        delegate.add(conversationId, messages);
        lru.put(conversationId, Boolean.TRUE);
    }

    @Override
    public List<Message> get(String conversationId) {
        // 先读取 delegate，再 touch LRU — 降低中间被驱逐的 TOCTOU 窗口
        List<Message> messages = delegate.get(conversationId);
        if (messages != null && !messages.isEmpty()) {
            lru.get(conversationId); // 仅在读取到数据时标记访问
        }
        return messages != null ? messages : new ArrayList<>();
    }

    /**
     * 兼容旧 API：获取最近 N 条消息。
     * Spring AI 1.1.2 的 ChatMemory.get() 只接受 conversationId，
     * 此方法用于需要限制消息数量的场景。
     */
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messages = get(conversationId);
        if (messages.size() <= lastN) {
            return messages;
        }
        return messages.subList(messages.size() - lastN, messages.size());
    }

    @Override
    public void clear(String conversationId) {
        lru.remove(conversationId);
        delegate.clear(conversationId);
    }
}
