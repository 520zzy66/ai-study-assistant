package com.study.config;

import com.study.service.UserMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 记忆条目定时清理任务
 *
 * <p>每天凌晨 3:00 执行一次，清理过期的低重要度记忆条目。
 * 清理规则：90 天前 且 importance < 0.9 → 删除。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MemoryCleanupScheduler {

    private final UserMemoryService userMemoryService;

    /**
     * 每天凌晨 3:00 清理过期记忆条目
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredMemories() {
        try {
            int deleted = userMemoryService.cleanupExpiredMemories();
            if (deleted > 0) {
                log.info("记忆清理任务完成，删除 {} 条过期记忆", deleted);
            }
        } catch (Exception e) {
            log.warn("记忆清理任务异常（不影响业务）: {}", e.getMessage());
        }
    }
}
