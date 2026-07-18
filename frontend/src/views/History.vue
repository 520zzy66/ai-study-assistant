<template>
  <div class="history-page">
    <BasePageHeader
      title="历史记录"
      description="查看你的 AI 对话和出题记录"
    />

    <BaseCard class="history-card" :padding="'none'">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="history-tabs">
        <el-tab-pane label="对话历史" name="chat">
          <el-table :data="chatHistory" v-loading="loading" class="history-table">
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <span class="type-tag">{{ getChatTypeLabel(row.chatType) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="资料" width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="material-name">{{ row.materialName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="问题" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="question-text">{{ row.userMessage || row.question }}</span>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="160">
              <template #default="{ row }">
                <span class="time-text">{{ row.createTime }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="handleViewChat(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!loading && chatHistory.length === 0" class="tab-empty">
            <AppEmpty
              icon="ChatDotRound"
              title="暂无对话记录"
              description="开始与 AI 对话，记录会在这里显示"
              compact
            />
          </div>

          <div v-if="chatTotal > 0" class="table-footer">
            <span class="total-text">共 {{ chatTotal }} 条记录</span>
            <el-pagination
              v-model:current-page="chatQuery.page"
              v-model:page-size="chatQuery.size"
              :total="chatTotal"
              layout="prev, pager, next"
              @current-change="loadChatHistory"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="出题记录" name="quiz">
          <el-table :data="quizHistory" v-loading="loading" class="history-table">
            <el-table-column label="资料" width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="material-name">{{ row.materialName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="题目数" width="100" align="center">
              <template #default="{ row }">
                <span class="count-text">{{ row.questionCount }} 道</span>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="160">
              <template #default="{ row }">
                <span class="time-text">{{ row.createTime }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="handleViewQuiz(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!loading && quizHistory.length === 0" class="tab-empty">
            <AppEmpty
              icon="EditPen"
              title="暂无出题记录"
              description="使用 AI 出题功能，记录会在这里显示"
              compact
            />
          </div>

          <div v-if="quizTotal > 0" class="table-footer">
            <span class="total-text">共 {{ quizTotal }} 条记录</span>
            <el-pagination
              v-model:current-page="quizQuery.page"
              v-model:page-size="quizQuery.size"
              :total="quizTotal"
              layout="prev, pager, next"
              @current-change="loadQuizHistory"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </BaseCard>

    <!-- Chat Detail Drawer -->
    <el-drawer v-model="showChatDetail" title="对话详情" size="520px">
      <div v-if="currentChat" class="detail-content">
        <div class="detail-meta">
          <span class="type-tag">{{ getChatTypeLabel(currentChat.chatType) }}</span>
          <span class="meta-text">{{ currentChat.materialName || '-' }}</span>
          <span class="meta-text">{{ currentChat.createTime }}</span>
        </div>
        <div class="detail-section">
          <div class="detail-label">用户提问</div>
          <div class="detail-value">{{ currentChat.userMessage || currentChat.question }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">AI 回答</div>
          <div class="ai-response-content markdown-body" v-html="renderMarkdown(currentChat.aiResponse || currentChat.answer)" />
        </div>
      </div>
    </el-drawer>

    <!-- Quiz Detail Drawer -->
    <el-drawer v-model="showQuizDetail" title="出题详情" size="420px">
      <div v-if="currentQuiz" class="detail-content">
        <div class="detail-row">
          <div class="detail-section">
            <div class="detail-label">资料</div>
            <div class="detail-value">{{ currentQuiz.materialName || '-' }}</div>
          </div>
          <div class="detail-section">
            <div class="detail-label">题目数</div>
            <div class="detail-value">{{ currentQuiz.questionCount }} 道</div>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-label">生成时间</div>
          <div class="detail-value">{{ currentQuiz.createTime }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useMarkdown } from '@/composables/useMarkdown'
import { getChatHistory, getQuizHistory } from '@/api/history'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

const { renderMarkdown } = useMarkdown()

const activeTab = ref('chat')
const loading = ref(false)

const chatHistory = ref([])
const chatTotal = ref(0)
const chatQuery = reactive({ page: 1, size: 10 })

const quizHistory = ref([])
const quizTotal = ref(0)
const quizQuery = reactive({ page: 1, size: 10 })

const showChatDetail = ref(false)
const currentChat = ref(null)
const showQuizDetail = ref(false)
const currentQuiz = ref(null)

function getChatTypeLabel(type) {
  const map = {
    summary: '总结',
    qa: '问答',
    quiz: '出题',
    plan: '计划',
    chat: '问答',
    workflow: '工作流',
    resource_package: '资源包'
  }
  return map[type] || type
}

async function loadChatHistory() {
  loading.value = true
  try {
    const data = await getChatHistory(chatQuery)
    chatHistory.value = data.records || []
    chatTotal.value = data.total || 0
  } catch (error) {
    chatHistory.value = []
  } finally {
    loading.value = false
  }
}

async function loadQuizHistory() {
  loading.value = true
  try {
    const data = await getQuizHistory(quizQuery)
    quizHistory.value = data.records || []
    quizTotal.value = data.total || 0
  } catch (error) {
    quizHistory.value = []
  } finally {
    loading.value = false
  }
}

function handleTabChange(tab) {
  if (tab === 'chat') {
    loadChatHistory()
  } else {
    loadQuizHistory()
  }
}

function handleViewChat(row) {
  currentChat.value = row
  showChatDetail.value = true
}

function handleViewQuiz(row) {
  currentQuiz.value = row
  showQuizDetail.value = true
}

onMounted(() => {
  loadChatHistory()
})
</script>

<style scoped>
.history-page {
  width: 100%;
}

.history-card {
  border-radius: var(--radius-lg);
}

.history-card :deep(.card-body) {
  padding: var(--space-5) var(--space-6);
}

.history-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-5);
}

.history-table {
  margin-bottom: var(--space-4);
}

.type-tag {
  display: inline-block;
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--surface-container);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.material-name {
  font-size: var(--text-body);
  color: var(--color-text-primary);
}

.question-text {
  font-size: var(--text-body);
  color: var(--color-text-primary);
}

.time-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.count-text {
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--color-text-primary);
}

.tab-empty {
  border-top: 1px solid var(--outline-variant);
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--outline-variant);
}

.total-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

/* Detail */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--outline-variant);
}

.meta-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.detail-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.detail-label {
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-value {
  font-size: var(--text-body);
  color: var(--color-text-primary);
  line-height: 1.6;
}

.ai-response-content {
  font-size: var(--text-body);
  line-height: 1.7;
}

@media (max-width: 767px) {
  .table-footer {
    flex-direction: column;
    gap: var(--space-3);
    align-items: flex-start;
  }
}
</style>
