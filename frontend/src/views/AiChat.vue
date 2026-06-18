<template>
  <div class="chat-page">
    <!-- Left Sidebar: History -->
    <aside class="chat-history-sidebar">
      <div class="history-header">
        <h3 class="history-title">对话历史</h3>
        <el-button text type="primary" :icon="Plus" size="small" @click="createNewChat">
          新对话
        </el-button>
      </div>
      <div class="history-list">
        <div
          v-for="session in chatSessions"
          :key="session.id"
          class="history-item"
          :class="{ active: currentSessionId === session.id }"
          @click="switchSession(session.id)"
        >
          <el-icon :size="16"><ChatDotRound /></el-icon>
          <span class="history-label truncate">{{ session.title }}</span>
        </div>
        <div v-if="chatSessions.length === 0" class="history-empty">
          暂无历史对话
        </div>
      </div>
    </aside>

    <!-- Main Chat Area -->
    <main class="chat-main">
      <!-- Header -->
      <div class="chat-header">
        <div class="chat-header-left">
          <el-select
            v-model="selectedMaterialId"
            placeholder="选择资料"
            filterable
            clearable
            style="width: 260px;"
          >
            <el-option
              v-for="item in materialList"
              :key="item.id"
              :label="item.originalName"
              :value="item.id"
            />
          </el-select>
        </div>
        <div class="chat-header-right">
          <el-button text :icon="Delete" @click="clearMessages">清空</el-button>
        </div>
      </div>

      <!-- Messages -->
      <div class="chat-messages" ref="messageListRef">
        <div v-if="messages.length === 0" class="chat-empty">
          <AppEmpty
            icon="ChatDotRound"
            title="开始你的学习问答"
            :description="selectedMaterialId ? '基于已选资料向 AI 提问' : '请先选择一份学习资料'"
          />
          <div v-if="selectedMaterial && suggestedQuestions.length > 0" class="suggested-questions">
            <div class="suggested-label">推荐问题</div>
            <div class="question-chips">
              <button
                v-for="q in suggestedQuestions"
                :key="q"
                class="question-chip"
                @click="sendQuestion(q)"
              >
                {{ q }}
              </button>
            </div>
          </div>
        </div>

        <template v-else>
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message"
            :class="msg.role"
          >
            <div class="message-avatar">
              <div class="avatar-circle" :class="msg.role">
                <el-icon v-if="msg.role === 'assistant'" :size="16"><MagicStick /></el-icon>
                <span v-else>{{ userInitial }}</span>
              </div>
            </div>
            <div class="message-body">
              <div class="message-content markdown-body" v-html="renderMarkdown(msg.content)" />
              <div v-if="msg.sources?.length > 0" class="message-sources">
                <div class="sources-title">
                  <el-icon :size="12"><Collection /></el-icon>
                  <span>参考来源</span>
                </div>
                <div class="sources-list">
                  <span v-for="(source, i) in msg.sources" :key="i" class="source-chip">
                    切片 {{ source.chunkIndex }} · {{ (source.score * 100).toFixed(0) }}%
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="message assistant">
            <div class="message-avatar">
              <div class="avatar-circle assistant">
                <el-icon :size="16"><MagicStick /></el-icon>
              </div>
            </div>
            <div class="message-body">
              <div class="loading-bubble">
                <span class="typing-dot" />
                <span class="typing-dot" />
                <span class="typing-dot" />
                <span>AI 思考中</span>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Input -->
      <div class="chat-input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入你的问题... 按 Enter 发送，Shift+Enter 换行"
            resize="none"
            @keydown.enter.exact.prevent="handleSend"
          />
          <div class="input-actions">
            <span class="input-hint">
              {{ selectedMaterial ? `基于：${selectedMaterial.originalName}` : '未选择资料' }}
            </span>
            <el-button type="primary" :loading="loading" :disabled="!canSend" @click="handleSend">
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </div>
    </main>

    <!-- Right Sidebar: Context -->
    <aside class="chat-context-sidebar">
      <div class="context-card">
        <div class="context-title">当前资料</div>
        <div v-if="selectedMaterial" class="context-material">
          <div class="context-material-icon">
            <el-icon :size="20"><Document /></el-icon>
          </div>
          <div class="context-material-info">
            <div class="context-material-name truncate">{{ selectedMaterial.originalName }}</div>
            <div class="context-material-size">{{ formatFileSize(selectedMaterial.fileSize) }}</div>
          </div>
        </div>
        <div v-else class="context-empty">
          未选择资料
        </div>
      </div>

      <div v-if="selectedMaterial && suggestedQuestions.length > 0" class="context-card">
        <div class="context-title">推荐问题</div>
        <div class="context-questions">
          <button
            v-for="q in suggestedQuestions"
            :key="q"
            class="context-question"
            @click="sendQuestion(q)"
          >
            {{ q }}
          </button>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete, MagicStick, Promotion, ChatDotRound, Collection, Document } from '@element-plus/icons-vue'
import { useMarkdown } from '@/composables/useMarkdown'
import { useUserStore } from '@/stores/user'
import { askQuestion } from '@/api/ai'
import { loadReadyMaterials } from '@/api/material'
import AppEmpty from '@/components/common/AppEmpty.vue'

const route = useRoute()
const { renderMarkdown } = useMarkdown()
const userStore = useUserStore()

const materialList = ref([])
const selectedMaterialId = ref('')
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messageListRef = ref(null)
const currentSessionId = ref('default')

const chatSessions = ref([
  { id: 'default', title: '当前对话' }
])

const suggestedQuestions = ref([
  '这份资料的核心观点是什么？',
  '请帮我梳理一下知识结构',
  '这个概念怎么理解？',
  '有哪些需要注意的关键点？'
])

const selectedMaterial = computed(() => {
  return materialList.value.find(item => item.id === selectedMaterialId.value) || null
})

const userInitial = computed(() => {
  return (userStore.userInfo?.nickname || '我')[0]
})

const canSend = computed(() => {
  return selectedMaterialId.value && inputMessage.value.trim() && !loading.value
})

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

async function handleSend() {
  if (!canSend.value) {
    if (!selectedMaterialId.value) ElMessage.warning('请先选择资料')
    return
  }

  const question = inputMessage.value.trim()
  messages.value.push({ role: 'user', content: question })
  inputMessage.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const data = await askQuestion({
      materialId: selectedMaterialId.value,
      question
    })
    messages.value.push({
      role: 'assistant',
      content: data.answer || data,
      sources: data.sources || []
    })
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，回答生成失败，请稍后重试。'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function sendQuestion(question) {
  inputMessage.value = question
  handleSend()
}

function clearMessages() {
  messages.value = []
}

function createNewChat() {
  currentSessionId.value = `session-${Date.now()}`
  chatSessions.value.unshift({
    id: currentSessionId.value,
    title: '新对话'
  })
  clearMessages()
}

function switchSession(id) {
  currentSessionId.value = id
}

async function loadMaterials() {
  materialList.value = await loadReadyMaterials()
  const queryId = route.query.materialId
  if (queryId) {
    selectedMaterialId.value = Number(queryId)
  }
}

onMounted(() => {
  loadMaterials()
})

watch(() => route.query.materialId, (newId) => {
  if (newId) {
    selectedMaterialId.value = Number(newId)
  }
})

watch(messages, () => {
  scrollToBottom()
}, { deep: true })
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 260px 1fr 260px;
  gap: var(--space-4);
  height: calc(100vh - var(--header-height) - var(--space-8));
  min-height: 480px;
}

/* Left Sidebar */
.chat-history-sidebar {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-4);
  border-bottom: 1px solid var(--outline-variant);
}

.history-title {
  font-size: var(--text-heading-4);
  font-weight: 600;
  color: var(--color-text-primary);
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2);
}

.history-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: var(--text-body);
  transition: background-color var(--duration-fast) var(--ease-default);
}

.history-item:hover,
.history-item.active {
  background: var(--surface-hover);
  color: var(--color-text-primary);
}

.history-label {
  flex: 1;
}

.history-empty {
  padding: var(--space-8) var(--space-4);
  text-align: center;
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

/* Main */
.chat-main {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--outline-variant);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-5);
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.chat-empty :deep(.app-empty) {
  padding: var(--space-12) 0 var(--space-8);
}

.suggested-questions {
  width: 100%;
  max-width: 520px;
}

.suggested-label {
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-3);
  text-align: center;
}

.question-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  justify-content: center;
}

.question-chip {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--outline);
  border-radius: var(--radius-full);
  background: var(--surface-card);
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.question-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--surface-active);
}

/* Messages */
.message {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-small);
  font-weight: 600;
}

.avatar-circle.user {
  background: var(--color-primary);
  color: #fff;
}

.avatar-circle.assistant {
  background: var(--surface-container);
  color: var(--color-primary);
}

.message-body {
  max-width: 70%;
}

.message-content {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  line-height: 1.7;
  font-size: var(--text-body);
}

.message.user .message-content {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: var(--radius-sm);
}

.message.assistant .message-content {
  background: var(--surface-container);
  color: var(--color-text-primary);
  border-bottom-left-radius: var(--radius-sm);
}

.message-sources {
  margin-top: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: var(--surface-container-low);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-md);
}

.sources-title {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-micro);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: var(--space-1);
}

.sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.source-chip {
  font-size: var(--text-micro);
  color: var(--color-text-secondary);
  background: var(--surface-card);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.loading-bubble {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: var(--surface-container);
  border-radius: var(--radius-lg);
  border-bottom-left-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: var(--text-body);
}

.typing-dot {
  width: 6px;
  height: 6px;
  background: currentColor;
  border-radius: 50%;
  animation: typingPulse 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) { animation-delay: -0.32s; }
.typing-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes typingPulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* Input */
.chat-input-area {
  padding: var(--space-4);
  border-top: 1px solid var(--outline-variant);
}

.input-wrapper {
  background: var(--surface-container-low);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  padding: var(--space-3);
  transition: border-color var(--duration-fast) var(--ease-default);
}

.input-wrapper:focus-within {
  border-color: var(--color-primary);
}

.input-wrapper :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  font-size: var(--text-body);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-2);
}

.input-hint {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

/* Right Sidebar */
.chat-context-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.context-card {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.context-title {
  font-size: var(--text-heading-4);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-3);
}

.context-material {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
}

.context-material-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--surface-container);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.context-material-info {
  min-width: 0;
}

.context-material-name {
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--color-text-primary);
}

.context-material-size {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: var(--space-1);
}

.context-empty {
  font-size: var(--text-body);
  color: var(--color-text-tertiary);
  padding: var(--space-4);
  text-align: center;
}

.context-questions {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.context-question {
  text-align: left;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  line-height: 1.5;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.context-question:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--surface-active);
}

@media (max-width: 1279px) {
  .chat-page {
    grid-template-columns: 200px 1fr;
  }

  .chat-context-sidebar {
    display: none;
  }
}

@media (max-width: 767px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .chat-history-sidebar {
    display: none;
  }

  .message-body {
    max-width: 85%;
  }
}
</style>
