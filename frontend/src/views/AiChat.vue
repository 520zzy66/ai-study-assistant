<template>
  <div class="chat-container">
    <!-- 左侧会话管理侧边栏 -->
    <aside class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- 顶部品牌区 -->
      <div class="sidebar-brand">
        <div class="brand-content" v-if="!sidebarCollapsed">
          <div class="brand-logo">
            <el-icon :size="24"><ChatDotRound /></el-icon>
            <span class="brand-name">AI 学习助手</span>
          </div>
        </div>
        <button class="collapse-btn" @click="toggleSidebar">
          <el-icon :size="18">
            <component :is="sidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
        </button>
      </div>

      <!-- 新建对话按钮 -->
      <div class="sidebar-new-chat" v-if="!sidebarCollapsed">
        <button class="new-chat-btn" @click="createNewChat">
          <el-icon :size="16"><Plus /></el-icon>
          <span>新建对话</span>
        </button>
      </div>
      <div class="sidebar-new-chat collapsed" v-else>
        <button class="new-chat-btn icon-only" @click="createNewChat">
          <el-icon :size="18"><Plus /></el-icon>
        </button>
      </div>

      <!-- 会话列表区 -->
      <div class="sidebar-conversations" v-if="!sidebarCollapsed">
        <!-- 今天 -->
        <div class="conversation-group" v-if="todayConversations.length > 0">
          <div class="group-title">今天</div>
          <div
            v-for="conv in todayConversations"
            :key="conv.id"
            :class="['conversation-item', { active: conv.id === activeConversationId }]"
            @click="switchConversation(conv.id)"
          >
            <div class="conversation-icon">
              <el-icon :size="16"><ChatLineRound /></el-icon>
            </div>
            <div class="conversation-info">
              <div class="conversation-title">{{ conv.title }}</div>
              <div class="conversation-preview">{{ conv.preview }}</div>
            </div>
            <div class="conversation-actions" @click.stop>
              <el-dropdown trigger="click" @command="(cmd) => handleConversationAction(cmd, conv)">
                <button class="more-btn">
                  <el-icon :size="14"><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">
                      <el-icon><Edit /></el-icon>
                      重命名
                    </el-dropdown-item>
                    <el-dropdown-item command="pin">
                      <el-icon><Star /></el-icon>
                      {{ conv.pinned ? '取消固定' : '固定' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>

        <!-- 过去7天 -->
        <div class="conversation-group" v-if="weekConversations.length > 0">
          <div class="group-title">过去7天</div>
          <div
            v-for="conv in weekConversations"
            :key="conv.id"
            :class="['conversation-item', { active: conv.id === activeConversationId }]"
            @click="switchConversation(conv.id)"
          >
            <div class="conversation-icon">
              <el-icon :size="16"><ChatLineRound /></el-icon>
            </div>
            <div class="conversation-info">
              <div class="conversation-title">{{ conv.title }}</div>
              <div class="conversation-preview">{{ conv.preview }}</div>
            </div>
            <div class="conversation-actions" @click.stop>
              <el-dropdown trigger="click" @command="(cmd) => handleConversationAction(cmd, conv)">
                <button class="more-btn">
                  <el-icon :size="14"><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">
                      <el-icon><Edit /></el-icon>
                      重命名
                    </el-dropdown-item>
                    <el-dropdown-item command="pin">
                      <el-icon><Star /></el-icon>
                      {{ conv.pinned ? '取消固定' : '固定' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>

        <!-- 更早 -->
        <div class="conversation-group" v-if="olderConversations.length > 0">
          <div class="group-title">更早</div>
          <div
            v-for="conv in olderConversations"
            :key="conv.id"
            :class="['conversation-item', { active: conv.id === activeConversationId }]"
            @click="switchConversation(conv.id)"
          >
            <div class="conversation-icon">
              <el-icon :size="16"><ChatLineRound /></el-icon>
            </div>
            <div class="conversation-info">
              <div class="conversation-title">{{ conv.title }}</div>
              <div class="conversation-preview">{{ conv.preview }}</div>
            </div>
            <div class="conversation-actions" @click.stop>
              <el-dropdown trigger="click" @command="(cmd) => handleConversationAction(cmd, conv)">
                <button class="more-btn">
                  <el-icon :size="14"><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">
                      <el-icon><Edit /></el-icon>
                      重命名
                    </el-dropdown-item>
                    <el-dropdown-item command="pin">
                      <el-icon><Star /></el-icon>
                      {{ conv.pinned ? '取消固定' : '固定' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="conversations.length === 0" class="conversations-empty">
          <el-icon :size="32"><ChatDotRound /></el-icon>
          <p>暂无对话记录</p>
        </div>
      </div>

      <!-- 底部用户/设置区 -->
      <div class="sidebar-footer">
        <div class="user-info" v-if="!sidebarCollapsed">
          <div class="user-avatar">
            <el-icon :size="20"><User /></el-icon>
          </div>
          <div class="user-name">{{ userName }}</div>
        </div>
        <div class="footer-actions">
          <button class="icon-btn" @click="toggleTheme" :title="isDarkMode ? '切换到浅色模式' : '切换到深色模式'">
            <el-icon :size="18">
              <component :is="isDarkMode ? 'Sunny' : 'Moon'" />
            </el-icon>
          </button>
          <button class="icon-btn" @click="openSettings" title="设置">
            <el-icon :size="18"><Setting /></el-icon>
          </button>
        </div>
      </div>
    </aside>

    <!-- 右侧主对话工作区 -->
    <main class="chat-main">
      <!-- 顶部对话标题栏 -->
      <header class="chat-header">
        <div class="header-left">
          <h1 class="chat-title" v-if="!isEditingTitle" @dblclick="startEditTitle">
            {{ currentConversationTitle }}
          </h1>
          <input
            v-else
            ref="titleInput"
            v-model="editedTitle"
            class="title-input"
            @blur="saveTitle"
            @keydown.enter="saveTitle"
            @keydown.escape="cancelEditTitle"
          />
        </div>
        <div class="header-right">
          <!-- 模型选择器 -->
          <div class="model-selector">
            <el-select v-model="selectedModel" size="small" class="model-select">
              <el-option label="DeepSeek-V3" value="deepseek-chat" />
              <el-option label="DeepSeek-R1" value="deepseek-reasoner" />
            </el-select>
          </div>
          <!-- 联网搜索开关 -->
          <button
            :class="['search-toggle', { active: webSearchEnabled }]"
            @click="webSearchEnabled = !webSearchEnabled"
            title="联网搜索"
          >
            <el-icon :size="16"><Search /></el-icon>
            <span>搜索</span>
          </button>
        </div>
      </header>

      <!-- 中央消息流区域 -->
      <div class="messages-area" ref="messagesContainer">
        <!-- 空状态 -->
        <div v-if="!aiStore.hasMessages && !aiStore.isStreaming" class="welcome-screen">
          <div class="welcome-icon">
            <el-icon :size="48"><ChatDotRound /></el-icon>
          </div>
          <h2>AI 学习助手</h2>
          <p class="welcome-desc">我可以帮你解答学习问题、总结知识点、制定学习计划</p>

          <!-- 建议问题 -->
          <div class="suggested-questions">
            <button
              v-for="q in suggestedQuestions"
              :key="q"
              class="question-card"
              @click="sendMessage(q)"
            >
              <el-icon :size="16"><MagicStick /></el-icon>
              <span>{{ q }}</span>
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-else class="messages-list">
          <div
            v-for="(msg, index) in aiStore.messages"
            :key="index"
            :class="['message-item', msg.role]"
          >
            <!-- 用户消息 -->
            <div v-if="msg.role === 'user'" class="message-user">
              <div class="message-bubble user-bubble">
                <div class="message-text">{{ msg.content }}</div>
              </div>
              <div class="message-avatar user-avatar">
                <el-icon :size="18"><User /></el-icon>
              </div>
            </div>

            <!-- AI消息 -->
            <div v-else class="message-assistant">
              <div class="message-avatar assistant-avatar">
                <el-icon :size="18"><MagicStick /></el-icon>
              </div>
              <div class="message-bubble assistant-bubble">
                <div class="message-text markdown-body" v-html="renderMarkdown(msg.content)" />
                <div class="message-actions">
                  <button class="action-btn" @click="copyMessage(msg.content)" title="复制">
                    <el-icon :size="14"><CopyDocument /></el-icon>
                  </button>
                  <button
                    v-if="isLastAssistantMessage(index)"
                    class="action-btn"
                    @click="regenerateLast"
                    title="重新生成"
                  >
                    <el-icon :size="14"><Refresh /></el-icon>
                  </button>
                  <button class="action-btn" @click="likeMessage(index)" title="点赞">
                    <el-icon :size="14"><Pointer /></el-icon>
                  </button>
                  <button class="action-btn" @click="dislikeMessage(index)" title="点踩">
                    <el-icon :size="14"><Pointer style="transform: rotate(180deg)" /></el-icon>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 流式输出中 -->
          <div v-if="aiStore.isStreaming" class="message-item assistant">
            <div class="message-assistant">
              <div class="message-avatar assistant-avatar">
                <el-icon :size="18"><MagicStick /></el-icon>
              </div>
              <div class="message-bubble assistant-bubble">
                <div v-if="aiStore.currentAnswer" class="message-text markdown-body" v-html="renderMarkdown(aiStore.currentAnswer)" />
                <div v-else class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 中断提示 -->
          <div v-if="aiStore.interruptedInfo && !aiStore.isStreaming" class="system-banner interrupted">
            <el-icon :size="16"><WarningFilled /></el-icon>
            <span>{{ aiStore.interruptedInfo.reason }}</span>
            <button class="banner-btn" @click="regenerateLast">重新生成</button>
            <button class="banner-btn secondary" @click="aiStore.clearInterrupted">忽略</button>
          </div>

          <!-- 错误提示 -->
          <div v-if="aiStore.error && !aiStore.isStreaming" class="system-banner error">
            <el-icon :size="16"><CircleCloseFilled /></el-icon>
            <span>{{ aiStore.error }}</span>
            <button class="banner-btn" @click="regenerateLast">重试</button>
            <button class="banner-btn secondary" @click="aiStore.clearError">关闭</button>
          </div>
        </div>
      </div>

      <!-- 底部输入区域 -->
      <div class="input-area">
        <div class="input-wrapper">
          <!-- 关联资料标签 -->
          <div v-if="selectedMaterial" class="material-tag-bar">
            <div class="material-tag">
              <el-icon :size="14"><Document /></el-icon>
              <span class="tag-name">{{ selectedMaterial.originalName }}</span>
              <button class="tag-close" @click="clearMaterial">
                <el-icon :size="12"><Close /></el-icon>
              </button>
            </div>
          </div>

          <!-- 输入框 -->
          <div class="input-box">
            <div class="input-left">
              <button class="icon-btn" @click="triggerFileUpload" title="上传附件">
                <el-icon :size="20"><Paperclip /></el-icon>
              </button>
              <input
                type="file"
                ref="fileInput"
                style="display: none"
                @change="handleFileUpload"
                accept=".pdf,.doc,.docx,.md,.txt"
              />
            </div>
            <textarea
              ref="textareaRef"
              v-model="inputText"
              class="input-textarea"
              placeholder="给 AI 学习助手发送消息"
              :disabled="aiStore.loading"
              @keydown.enter.exact.prevent="handleSend"
              @input="autoResize"
              rows="1"
            />
            <div class="input-right">
              <button
                v-if="!aiStore.isStreaming"
                class="send-btn"
                :disabled="!canSend"
                @click="handleSend"
              >
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                  <path d="M3 10L17 3L10 17L9 11L3 10Z" fill="currentColor"/>
                </svg>
              </button>
              <button
                v-else
                class="stop-btn"
                @click="stopStream"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <rect x="3" y="3" width="10" height="10" rx="2" fill="currentColor"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 底部信息 -->
          <div class="input-footer">
            <span class="disclaimer">内容由 AI 生成，仅供参考</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound, ChatLineRound, Promotion, Refresh, CopyDocument,
  User, MagicStick, Delete, WarningFilled,
  CircleCloseFilled, VideoPause, Plus, MoreFilled,
  Edit, Star, Setting, Moon, Sunny, Search, Paperclip, Pointer,
  Expand, Fold, Close, Document
} from '@element-plus/icons-vue'
import { useMarkdown } from '@/composables/useMarkdown'
import { useAiStore } from '@/stores/ai'
import { askQuestionStream } from '@/api/ai'
import { loadAvailableMaterials, uploadMaterial } from '@/api/material'
import { getChatHistory } from '@/api/history'

const route = useRoute()
const { renderMarkdown } = useMarkdown()
const aiStore = useAiStore()

// 侧边栏状态
const sidebarCollapsed = ref(false)
const activeConversationId = ref(null)

// 会话列表数据
const conversations = ref([])

// 按时间分组的会话
const todayConversations = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return conversations.value.filter(c => {
    const convDate = new Date(c.createdAt)
    convDate.setHours(0, 0, 0, 0)
    return convDate.getTime() === today.getTime()
  })
})

const weekConversations = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const weekAgo = new Date(today)
  weekAgo.setDate(weekAgo.getDate() - 7)
  return conversations.value.filter(c => {
    const convDate = new Date(c.createdAt)
    convDate.setHours(0, 0, 0, 0)
    return convDate.getTime() < today.getTime() && convDate.getTime() >= weekAgo.getTime()
  })
})

const olderConversations = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const weekAgo = new Date(today)
  weekAgo.setDate(weekAgo.getDate() - 7)
  return conversations.value.filter(c => {
    const convDate = new Date(c.createdAt)
    convDate.setHours(0, 0, 0, 0)
    return convDate.getTime() < weekAgo.getTime()
  })
})

// 标题编辑
const isEditingTitle = ref(false)
const editedTitle = ref('')
const titleInput = ref(null)

// 模型选择
const selectedModel = ref('deepseek-chat')
const webSearchEnabled = ref(false)

// 主题
const isDarkMode = ref(false)

// 用户信息
const userName = ref('学习者')

// 消息相关
const messagesContainer = ref(null)
const materialList = ref([])
const selectedMaterialId = ref(null)
const inputText = ref('')
const fileInput = ref(null)
const textareaRef = ref(null)

const selectedMaterial = computed(() =>
  materialList.value.find(m => m.id === selectedMaterialId.value) || null
)

const canSend = computed(() =>
  inputText.value.trim() && !aiStore.loading && !aiStore.isStreaming
)

const currentConversationTitle = computed(() => {
  if (activeConversationId.value) {
    const conv = conversations.value.find(c => c.id === activeConversationId.value)
    return conv ? conv.title : '新建对话'
  }
  return '新建对话'
})

const suggestedQuestions = computed(() => {
  if (selectedMaterial.value) {
    return [
      '这份资料的核心观点是什么？',
      '请帮我梳理一下知识结构',
      '这个概念怎么理解？',
      '有哪些需要注意的关键点？'
    ]
  }
  return [
    '如何高效学习一门新学科？',
    '什么是费曼学习法？',
    '如何制定合理的学习计划？',
    '怎样提高记忆效率？'
  ]
})

// 加载会话列表
async function loadConversations() {
  try {
    const result = await getChatHistory({ type: 'qa', page: 1, size: 100 })
    // 按 conversationId 聚合
    const grouped = {}
    for (const record of result.records) {
      const cid = record.conversationId
      if (!grouped[cid]) {
        grouped[cid] = {
          id: cid,
          title: record.userMessage?.substring(0, 30) || '新对话',
          preview: record.aiResponse?.substring(0, 50) || '',
          createdAt: new Date(record.createTime),
          pinned: false,
          materialId: record.materialId
        }
      }
    }
    conversations.value = Object.values(grouped).sort(
      (a, b) => b.createdAt - a.createdAt
    )
  } catch (e) {
    console.error('加载会话列表失败:', e)
  }
}

// 侧边栏操作
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function createNewChat() {
  activeConversationId.value = null
  aiStore.clearMessages()
}

async function switchConversation(conversationId) {
  activeConversationId.value = conversationId
  aiStore.clearMessages()

  try {
    const result = await getChatHistory({ page: 1, size: 100 })
    // 筛选出该 conversationId 的消息，按时间正序
    const msgs = result.records
      .filter(r => r.conversationId === conversationId)
      .sort((a, b) => new Date(a.createTime) - new Date(b.createTime))

    for (const msg of msgs) {
      if (msg.userMessage) {
        aiStore.addUserMessage(msg.userMessage)
      }
      if (msg.aiResponse) {
        aiStore.addAssistantMessage(msg.aiResponse)
      }
    }

    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.error('加载会话消息失败:', e)
    ElMessage.error('加载会话历史失败')
  }
}

function handleConversationAction(command, conv) {
  switch (command) {
    case 'rename':
      // 实现重命名逻辑
      ElMessage.info('重命名功能开发中')
      break
    case 'pin':
      conv.pinned = !conv.pinned
      break
    case 'delete':
      ElMessageBox.confirm('确定删除该对话？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const index = conversations.value.findIndex(c => c.id === conv.id)
        if (index > -1) {
          conversations.value.splice(index, 1)
          if (activeConversationId.value === conv.id) {
            activeConversationId.value = null
            aiStore.clearMessages()
          }
          ElMessage.success('已删除')
        }
      }).catch(() => {})
      break
  }
}

// 标题编辑
function startEditTitle() {
  editedTitle.value = currentConversationTitle.value
  isEditingTitle.value = true
  nextTick(() => titleInput.value?.focus())
}

function saveTitle() {
  if (editedTitle.value.trim()) {
    const conv = conversations.value.find(c => c.id === activeConversationId.value)
    if (conv) {
      conv.title = editedTitle.value.trim()
    }
  }
  isEditingTitle.value = false
}

function cancelEditTitle() {
  isEditingTitle.value = false
}

// 主题切换
function toggleTheme() {
  isDarkMode.value = !isDarkMode.value
  document.documentElement.classList.toggle('dark', isDarkMode.value)
}

function openSettings() {
  ElMessage.info('设置功能开发中')
}

// 文件上传
function triggerFileUpload() {
  fileInput.value?.click()
}

async function handleFileUpload(event) {
  const file = event.target.files[0]
  if (!file) return

  // 校验文件类型
  const allowedTypes = ['.pdf', '.doc', '.docx', '.md', '.txt']
  const ext = '.' + file.name.split('.').pop().toLowerCase()
  if (!allowedTypes.includes(ext)) {
    ElMessage.error('不支持的文件类型，仅支持 PDF/DOC/DOCX/MD/TXT')
    return
  }

  // 校验文件大小（50MB）
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 50MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    ElMessage.info('正在上传文件...')
    const result = await uploadMaterial(formData)
    ElMessage.success(`文件 "${file.name}" 上传成功，正在处理中`)

    // 上传成功后自动关联该资料到当前对话
    selectedMaterialId.value = result.id
    aiStore.setSelectedMaterial(result.id)

    // 刷新资料列表
    await loadMaterials()
  } catch (err) {
    console.error('文件上传失败:', err)
    ElMessage.error('文件上传失败: ' + (err.message || '未知错误'))
  } finally {
    // 清空 file input，允许重复上传同一文件
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

// 消息操作
function isLastAssistantMessage(index) {
  for (let i = aiStore.messages.length - 1; i >= 0; i--) {
    if (aiStore.messages[i].role === 'assistant') {
      return i === index
    }
  }
  return false
}

function isFailedMessage(content) {
  return content.includes('[回答被中断]') || content.includes('[回答异常终止]')
}

function onMaterialChange() {
  aiStore.setSelectedMaterial(selectedMaterialId.value)
}

function clearMaterial() {
  selectedMaterialId.value = null
  aiStore.setSelectedMaterial(null)
}

// 自动调整文本框高度
function autoResize() {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
  }
}

async function handleSend() {
  if (!canSend.value) return
  await sendMessage(inputText.value.trim())
}

async function sendMessage(text) {
  const question = text || inputText.value.trim()
  if (!question) return

  inputText.value = ''

  aiStore.addUserMessage(question)
  aiStore.setLoading(true)
  aiStore.setStreaming(true)
  aiStore.clearError()
  aiStore.clearInterrupted()

  await nextTick()
  scrollToBottom()

  const history = aiStore.getRecentHistory(10)

  const abortFn = askQuestionStream(
    {
      materialId: selectedMaterialId.value || null,
      question,
      history
    },
    {
      onToken(_token, fullText) {
        aiStore.updateCurrentAnswer(fullText)
        nextTick(() => scrollToBottom())
      },
      onComplete(fullText) {
        aiStore.finishStream()
        aiStore.setAbortFn(null)
        // 刷新会话列表
        loadConversations()
        nextTick(() => scrollToBottom())
      },
      onError(err) {
        console.error('AI 流式调用失败:', err)
        aiStore.setError(err)
        aiStore.setAbortFn(null)
        nextTick(() => scrollToBottom())
      }
    }
  )

  aiStore.setAbortFn(abortFn)
}

function stopStream() {
  aiStore.abortCurrent()
}

async function regenerateLast() {
  const lastQ = aiStore.lastQuestion
  if (!lastQ) return

  const lastIdx = aiStore.messages.length - 1
  if (lastIdx >= 0 && aiStore.messages[lastIdx].role === 'assistant') {
    const content = aiStore.messages[lastIdx].content
    if (isFailedMessage(content)) {
      aiStore.deleteMessage(lastIdx)
    }
  }

  aiStore.clearInterrupted()
  aiStore.clearError()

  await sendMessage(lastQ)
}

function copyMessage(content) {
  const plainText = content.replace(/[#*`_\[\]()]/g, '')
  navigator.clipboard.writeText(plainText).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

function likeMessage(index) {
  ElMessage.success('已点赞')
}

function dislikeMessage(index) {
  ElMessage.success('已点踩')
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

async function loadMaterials() {
  try {
    materialList.value = await loadAvailableMaterials()
    const queryId = route.query.materialId
    if (queryId) {
      selectedMaterialId.value = Number(queryId)
      aiStore.setSelectedMaterial(selectedMaterialId.value)
    }
  } catch (e) {
    console.error('加载资料列表失败:', e)
  }
}

onMounted(() => {
  loadMaterials()
  loadConversations()
  nextTick(() => scrollToBottom())
})

onBeforeUnmount(() => {
  // 清理工作
})

watch(() => route.query.materialId, newId => {
  if (newId) {
    selectedMaterialId.value = Number(newId)
    aiStore.setSelectedMaterial(selectedMaterialId.value)
  }
})
</script>

<style scoped>
/* 设计系统变量 - 使用 :global 确保 CSS 变量应用到 :root */
:global(:root) {
  --sidebar-width: 280px;
  --sidebar-collapsed-width: 60px;
  --header-height: 56px;
  --input-min-height: 120px;
  --color-bg-primary: #ffffff;
  --color-bg-secondary: #f8fafc;
  --color-bg-sidebar: #1e293b;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-sidebar: #e2e8f0;
  --color-border: #e5e7eb;
  --color-border-light: #f1f5f9;
  --color-accent: #2563eb;
  --color-accent-hover: #1d4ed8;
  --color-user-bubble: #2563eb;
  --color-assistant-bubble: #f1f5f9;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-full: 9999px;
  --shadow-sm: 0 2px 6px rgba(0,0,0,0.06);
  --shadow-md: 0 8px 24px rgba(0,0,0,0.08);
}

.chat-container {
  display: flex;
  height: calc(100vh - 120px);
  background: var(--color-bg-primary);
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

/* 左侧会话管理侧边栏 */
.chat-sidebar {
  width: var(--sidebar-width);
  background: var(--color-bg-sidebar);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  flex-shrink: 0;
  z-index: 10;
}

.chat-sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

/* 品牌区 */
.sidebar-brand {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.brand-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: none;
  background: rgba(255,255,255,0.1);
  color: var(--color-text-sidebar);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.collapse-btn:hover {
  background: rgba(255,255,255,0.2);
}

/* 新建对话按钮 */
.sidebar-new-chat {
  padding: 16px;
}

.sidebar-new-chat.collapsed {
  padding: 16px 12px;
}

.new-chat-btn {
  width: 100%;
  height: 44px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(255,255,255,0.2);
  background: transparent;
  color: var(--color-text-sidebar);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.3);
}

.new-chat-btn.icon-only {
  width: 36px;
  height: 36px;
  margin: 0 auto;
}

/* 会话列表区 */
.sidebar-conversations {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.sidebar-conversations::-webkit-scrollbar {
  width: 4px;
}

.sidebar-conversations::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-conversations::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.2);
  border-radius: 4px;
}

.conversation-group {
  margin-bottom: 16px;
}

.group-title {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255,255,255,0.5);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.conversation-item:hover {
  background: rgba(255,255,255,0.1);
}

.conversation-item.active {
  background: rgba(255,255,255,0.15);
}

.conversation-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: var(--color-accent);
  border-radius: 0 4px 4px 0;
}

.conversation-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-sidebar);
  flex-shrink: 0;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-preview {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.conversation-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.conversation-item:hover .conversation-actions {
  opacity: 1;
}

.more-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  border: none;
  background: transparent;
  color: rgba(255,255,255,0.6);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.more-btn:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

.conversations-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: rgba(255,255,255,0.4);
}

.conversations-empty p {
  margin-top: 12px;
  font-size: 14px;
}

/* 底部用户/设置区 */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-sidebar);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
}

.footer-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  border: none;
  background: transparent;
  color: rgba(255,255,255,0.6);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

/* 右侧主对话工作区 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-bg-primary);
}

/* 顶部对话标题栏 */
.chat-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-primary);
}

.header-left {
  flex: 1;
  min-width: 0;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-title:hover {
  color: var(--color-accent);
}

.title-input {
  width: 100%;
  max-width: 400px;
  height: 36px;
  padding: 0 12px;
  border: 2px solid var(--color-accent);
  border-radius: var(--radius-sm);
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  outline: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.model-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-select {
  width: 140px;
}

.search-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  background: var(--color-bg-primary);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.search-toggle:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.search-toggle.active {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: #fff;
}

/* 中央消息流区域 */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

.messages-area::-webkit-scrollbar {
  width: 6px;
}

.messages-area::-webkit-scrollbar-track {
  background: transparent;
}

.messages-area::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 6px;
}

.messages-area::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-secondary);
}

/* 欢迎屏幕 */
.welcome-screen {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-accent);
  margin-bottom: 24px;
}

.welcome-screen h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 12px 0;
}

.welcome-desc {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0 0 40px 0;
  text-align: center;
}

.suggested-questions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 600px;
  width: 100%;
}

.question-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-bg-primary);
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.question-card:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: #f0f7ff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.question-card el-icon {
  color: var(--color-accent);
}

/* 消息列表 */
.messages-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.message-item {
  display: flex;
  flex-direction: column;
  animation: messageIn 0.3s ease;
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 用户消息 */
.message-user {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.message-user .user-bubble {
  max-width: 70%;
  padding: 14px 18px;
  background: var(--color-user-bubble);
  color: #fff;
  border-radius: var(--radius-lg) var(--radius-lg) 4px var(--radius-lg);
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
}

.message-user .user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #dbeafe;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-accent);
  flex-shrink: 0;
}

/* AI消息 */
.message-assistant {
  display: flex;
  gap: 12px;
}

.message-assistant .assistant-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.message-assistant .assistant-bubble {
  flex: 1;
  min-width: 0;
}

.message-assistant .message-text {
  padding: 16px 18px;
  background: var(--color-assistant-bubble);
  border-radius: 4px var(--radius-lg) var(--radius-lg) var(--radius-lg);
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-primary);
}

.message-assistant .message-text :deep(p) {
  margin: 0 0 12px 0;
}

.message-assistant .message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-assistant .message-text :deep(code) {
  background: rgba(0,0,0,0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 14px;
}

.message-assistant .message-text :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 16px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 12px 0;
}

.message-assistant .message-text :deep(pre code) {
  background: transparent;
  padding: 0;
}

/* 消息操作按钮 */
.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-item:hover .message-actions {
  opacity: 1;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.action-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-accent);
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 20px 18px;
  background: var(--color-assistant-bubble);
  border-radius: 4px var(--radius-lg) var(--radius-lg) var(--radius-lg);
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-text-secondary);
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* 系统提示 */
.system-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
}

.system-banner.interrupted {
  background: #fef3c7;
  border: 1px solid #fde68a;
  color: #92400e;
}

.system-banner.error {
  background: #fee2e2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.banner-btn {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.banner-btn:not(.secondary) {
  background: var(--color-accent);
  color: #fff;
}

.banner-btn:not(.secondary):hover {
  background: var(--color-accent-hover);
}

.banner-btn.secondary {
  background: transparent;
  color: inherit;
}

.banner-btn.secondary:hover {
  background: rgba(0,0,0,0.05);
}

/* 标题栏资料选择器 */
.material-select-header {
  width: 180px;
}

/* 底部输入区域 - DeepSeek 风格 */
.input-area {
  padding: 0 24px 24px;
  background: var(--color-bg-primary);
}

.input-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

/* 关联资料标签 */
.material-tag-bar {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.material-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #e8f4fd;
  border: 1px solid #b8dff7;
  border-radius: 20px;
  font-size: 13px;
  color: #1a73e8;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.material-tag .tag-name {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-tag .tag-close {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #1a73e8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.material-tag .tag-close:hover {
  background: rgba(26, 115, 232, 0.15);
}

/* 输入框容器 */
.input-box {
  display: flex;
  align-items: flex-end;
  background: #f4f4f4;
  border: 1px solid #e5e5e5;
  border-radius: 24px;
  padding: 8px 8px 8px 16px;
  transition: all 0.2s;
}

.input-box:focus-within {
  background: #fff;
  border-color: #d0d0d0;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.input-left,
.input-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.input-left {
  padding-bottom: 4px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #8e8e8e;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #e8e8e8;
  color: #333;
}

.input-textarea {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  resize: none;
  font-size: 15px;
  line-height: 1.6;
  color: #1a1a1a;
  padding: 8px 12px;
  min-height: 24px;
  max-height: 200px;
  font-family: inherit;
}

.input-textarea::placeholder {
  color: #9e9e9e;
}

.input-textarea:disabled {
  opacity: 0.6;
}

.send-btn,
.stop-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn {
  background: #1a1a1a;
  color: #fff;
}

.send-btn:hover:not(:disabled) {
  background: #333;
  transform: scale(1.05);
}

.send-btn:disabled {
  background: #d0d0d0;
  cursor: not-allowed;
}

.stop-btn {
  background: #ef4444;
  color: #fff;
}

.stop-btn:hover {
  background: #dc2626;
  transform: scale(1.05);
}

/* 底部信息 */
.input-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0 0;
}

.disclaimer {
  font-size: 12px;
  color: #9e9e9e;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .chat-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .chat-sidebar:not(.collapsed) {
    transform: translateX(0);
  }

  .chat-header {
    padding: 0 16px;
  }

  .messages-area {
    padding: 16px;
  }

  .input-area {
    padding: 12px 16px 20px;
  }
}

@media (max-width: 768px) {
  .suggested-questions {
    grid-template-columns: 1fr;
  }

  .message-user .user-bubble {
    max-width: 85%;
  }

  .model-selector {
    display: none;
  }
}
</style>
