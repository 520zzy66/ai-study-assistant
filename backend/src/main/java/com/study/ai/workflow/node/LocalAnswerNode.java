package com.study.ai.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.study.ai.workflow.graph.RouteKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 本地回答节点 — 直接返回 GeneralNode 生成的 Level 0/1 回答。
 *
 * <p>当 GeneralNode 判断为简单问题（Level 0 关键词匹配或 Level 1 简单常识）时，
 * 由该节点将 localAnswer 写入最终的 answer 字段。
 */
@Slf4j
@Component
public class LocalAnswerNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String localAnswer = state.value(RouteKeys.LOCAL_ANSWER, "");
        if (localAnswer == null || localAnswer.isBlank()) {
            localAnswer = "抱歉，我无法理解您的问题，请重新描述。";
        }

        log.info("[LocalAnswerNode] 返回本地回答：answer={}",
                localAnswer.length() > 60 ? localAnswer.substring(0, 60) + "..." : localAnswer);

        return Map.of(RouteKeys.ANSWER, localAnswer);
    }
}
