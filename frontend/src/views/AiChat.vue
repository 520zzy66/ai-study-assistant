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
                <!-- 文件卡片 -->
                <div v-if="msg.material" class="message-file-card">
                  <div class="file-card-icon">
                    <el-icon :size="20"><Document /></el-icon>
                  </div>
                  <div class="file-card-info">
                    <div class="file-card-name">{{ msg.material.name }}</div>
                    <div class="file-card-meta">
                      <span class="file-card-type">{{ (msg.material.fileType || '').toUpperCase() }}</span>
                      <span v-if="msg.material.fileSize" class="file-card-size">{{ formatFileSize(msg.material.fileSize) }}</span>
                    </div>
                  </div>
                </div>
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
            <div :class="['material-tag', { processing: isMaterialProcessing }]">
              <el-icon :size="14"><Document /></el-icon>
              <span class="tag-name">{{ selectedMaterial.originalName }}</span>
              <span v-if="isMaterialProcessing" class="tag-status">
                <span class="dot-pulse"></span>
                处理中
              </span>
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
import { loadAvailableMaterials, uploadMaterial, getMaterialDetail } from '@/api/material'
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
const inputText = ref('')
const fileInput = ref(null)
const textareaRef = ref(null)
let pollTimer = null

const selectedMaterial = computed(() =>
  materialList.value.find(m => m.id === aiStore.selectedMaterialId) || null
)

const isMaterialProcessing = computed(() =>
  selectedMaterial.value && selectedMaterial.value.status === 'processing'
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
        // 如果消息关联了资料，附带文件信息
        let materialInfo = null
        if (msg.materialId) {
          const mat = materialList.value.find(m => m.id === msg.materialId)
          if (mat) {
            materialInfo = {
              id: mat.id,
              name: mat.originalName,
              fileType: mat.fileType,
              fileSize: mat.fileSize
            }
          }
        }
        aiStore.addUserMessage(msg.userMessage, materialInfo)
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
    aiStore.setSelectedMaterial(result.id)

    // 将新上传的资料添加到列表（避免等待异步处理）
    const newMaterial = {
      id: result.id,
      originalName: result.originalName || file.name,
      fileType: result.fileType || ext.replace('.', ''),
      fileSize: result.fileSize || file.size,
      status: 'processing'
    }
    // 避免重复添加
    if (!materialList.value.find(m => m.id === newMaterial.id)) {
      materialList.value.unshift(newMaterial)
    }

    // 开始轮询资料处理状态
    startPollingMaterial(result.id)
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
  // selectedMaterialId 已通过 v-model 直接绑定到 store，无需额外同步
}

function clearMaterial() {
  aiStore.setSelectedMaterial(null)
  stopPolling()
}

/**
 * 轮询资料处理状态
 * 上传后异步处理需要时间，每 3 秒检查一次直到 ready/failed
 * 最多重试 20 次（约 1 分钟），避免无限轮询
 */
function startPollingMaterial(materialId) {
  stopPolling()
  let retryCount = 0
  const MAX_RETRY = 20
  pollTimer = setInterval(async () => {
    try {
      const detail = await getMaterialDetail(materialId)
      retryCount = 0 // 请求成功重置计数
      const idx = materialList.value.findIndex(m => m.id === materialId)
      if (idx !== -1) {
        materialList.value[idx].status = detail.status
      }
      if (detail.status === 'ready' || detail.status === 'failed') {
        stopPolling()
        if (detail.status === 'ready') {
          ElMessage.success(`"${detail.originalName}" 处理完成，可以开始对话`)
        } else {
          ElMessage.error(`"${detail.originalName}" 处理失败: ${detail.errorMsg || '未知错误'}`)
        }
      }
    } catch (e) {
      retryCount++
      console.error('轮询资料状态失败:', e)
      if (retryCount >= MAX_RETRY) {
        stopPolling()
        ElMessage.error('资料处理状态查询超时，请手动刷新页面')
      }
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
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
  // 防止并发竞态：流式进行中禁止重复发送
  if (aiStore.isStreaming || aiStore.loading) return

  const question = text || inputText.value.trim()
  if (!question) return

  // 检查关联资料是否还在处理中
  if (isMaterialProcessing.value) {
    ElMessage.warning('文件正在处理中，请稍候再提问')
    return
  }

  inputText.value = ''

  // 捕获当前关联的文件信息，嵌入到消息中
  const materialInfo = selectedMaterial.value
    ? {
        id: selectedMaterial.value.id,
        name: selectedMaterial.value.originalName,
        fileType: selectedMaterial.value.fileType,
        fileSize: selectedMaterial.value.fileSize
      }
    : null

  aiStore.addUserMessage(question, materialInfo)

  // 保存 materialId，因为清除状态后 store 中的值会丢失
  const currentMaterialId = materialInfo?.id || null

  // 文件已随消息发出，清除输入框上方的文件标签
  if (materialInfo) {
    aiStore.setSelectedMaterial(null)
  }

  aiStore.setLoading(true)
  aiStore.setStreaming(true)
  aiStore.clearError()
  aiStore.clearInterrupted()

  await nextTick()
  scrollToBottom()

  try {
    const history = aiStore.getRecentHistory(10)

    const abortFn = askQuestionStream(
      {
        materialId: currentMaterialId,
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
  } catch (err) {
    console.error('发送消息失败:', err)
    aiStore.setError(err.message || '发送失败')
    aiStore.finishStream()
  }
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

/**
 * 格式化文件大小
 */
function formatFileSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
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
      aiStore.setSelectedMaterial(Number(queryId))
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
  stopPolling()
  aiStore.abortCurrent()
})

watch(() => route.query.materialId, newId => {
  if (newId) {
    aiStore.setSelectedMaterial(Number(newId))
  }
})
</script>

<style scoped>
.chat-container {
  --chat-sidebar-width: 280px;
  --chat-sidebar-collapsed-width: 60px;
  --chat-header-height: 56px;
  --chat-user-bubble: var(--color-primary);
  --chat-assistant-bubble: var(--slate-100);

  display: flex;
  height: calc(100vh - 120px);
  background: var(--surface-card);
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 1px solid var(--outline);
}

/* 左侧会话管理侧边栏 — 浅色系，与全局一致 */
.chat-sidebar {
  width: var(--chat-sidebar-width);
  background: var(--surface-page);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  flex-shrink: 0;
  z-index: 10;
  border-right: 1px solid var(--outline);
}

.chat-sidebar.collapsed {
  width: var(--chat-sidebar-collapsed-width);
}

/* 品牌区 */
.sidebar-brand {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--outline-variant);
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
  color: var(--color-primary);
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.collapse-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: none;
  background: var(--surface-hover);
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.collapse-btn:hover {
  background: var(--surface-container);
  color: var(--color-text-primary);
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
  border: 1px solid var(--outline);
  background: var(--surface-card);
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all var(--duration-fast) var(--ease-default);
}

.new-chat-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
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
  background: var(--outline);
  border-radius: 4px;
}

.conversation-group {
  margin-bottom: 16px;
}

.group-title {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-tertiary);
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
  transition: background var(--duration-fast) var(--ease-default);
  position: relative;
}

.conversation-item:hover {
  background: var(--surface-hover);
}

.conversation-item.active {
  background: var(--surface-active);
}

.conversation-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: var(--color-primary);
  border-radius: 0 4px 4px 0;
}

.conversation-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--surface-container);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-preview {
  font-size: 12px;
  color: var(--color-text-tertiary);
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
  color: var(--color-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.more-btn:hover {
  background: var(--surface-container);
  color: var(--color-text-primary);
}

.conversations-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--color-text-disabled);
}

.conversations-empty p {
  margin-top: 12px;
  font-size: 14px;
}

/* 底部用户/设置区 */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--outline-variant);
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
  background: var(--surface-active);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
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
  color: var(--color-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--surface-hover);
  color: var(--color-text-primary);
}

/* 右侧主对话工作区 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--surface-card);
}

/* 顶部对话标题栏 */
.chat-header {
  height: var(--chat-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid var(--outline);
  background: var(--surface-card);
}

.header-left {
  flex: 1;
  min-width: 0;
}

.chat-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-title:hover {
  color: var(--color-primary);
}

.title-input {
  width: 100%;
  max-width: 400px;
  height: 36px;
  padding: 0 12px;
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-sm);
  font-size: var(--text-heading-3);
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
  border: 1px solid var(--outline);
  background: var(--surface-card);
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.search-toggle:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.search-toggle.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-on-primary);
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
  background: var(--outline);
  border-radius: 6px;
}

.messages-area::-webkit-scrollbar-thumb:hover {
  background: var(--slate-300);
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
  background: var(--blue-50);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  margin-bottom: 24px;
}

.welcome-screen h2 {
  font-size: var(--text-heading-1);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 12px 0;
}

.welcome-desc {
  font-size: var(--text-body);
  color: var(--color-text-tertiary);
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
  border: 1px solid var(--outline);
  background: var(--surface-card);
  color: var(--color-text-secondary);
  font-size: var(--text-ui);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  text-align: left;
}

.question-card:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
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
  background: var(--chat-user-bubble);
  color: var(--color-on-primary);
  border-radius: var(--radius-lg) var(--radius-lg) 4px var(--radius-lg);
  font-size: var(--text-body);
  line-height: 1.6;
  word-break: break-word;
}

/* 消息内文件卡片 */
.message-file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--radius-sm);
  margin-bottom: 10px;
  backdrop-filter: blur(4px);
}

.file-card-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.file-card-info {
  flex: 1;
  min-width: 0;
}

.file-card-name {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.file-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.file-card-type {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.15);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.file-card-size {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.message-user .user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--blue-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
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
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-primary);
  flex-shrink: 0;
}

.message-assistant .assistant-bubble {
  flex: 1;
  min-width: 0;
}

.message-assistant .message-text {
  padding: 16px 18px;
  background: var(--chat-assistant-bubble);
  border-radius: 4px var(--radius-lg) var(--radius-lg) var(--radius-lg);
  font-size: var(--text-body);
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
  background: var(--surface-container);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: var(--text-ui);
}

.message-assistant .message-text :deep(pre) {
  background: var(--slate-800);
  color: var(--slate-100);
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
  color: var(--color-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-default);
}

.action-btn:hover {
  background: var(--surface-hover);
  color: var(--color-primary);
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 20px 18px;
  background: var(--chat-assistant-bubble);
  border-radius: 4px var(--radius-lg) var(--radius-lg) var(--radius-lg);
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-text-tertiary);
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
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning);
  color: var(--color-warning-on);
}

.system-banner.error {
  background: var(--color-error-bg);
  border: 1px solid var(--color-error);
  color: var(--color-error-on);
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
  background: var(--color-primary);
  color: var(--color-on-primary);
}

.banner-btn:not(.secondary):hover {
  background: var(--color-primary-hover);
}

.banner-btn.secondary {
  background: transparent;
  color: inherit;
}

.banner-btn.secondary:hover {
  background: var(--state-hover);
}

/* 标题栏资料选择器 */
.material-select-header {
  width: 180px;
}

/* 底部输入区域 */
.input-area {
  padding: 0 24px 24px;
  background: var(--surface-card);
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
  background: var(--color-primary-light);
  border: 1px solid var(--blue-200);
  border-radius: var(--radius-full);
  font-size: var(--text-small);
  color: var(--color-primary);
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
  color: var(--color-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.material-tag .tag-close:hover {
  background: var(--state-hover);
}

.material-tag.processing {
  background: var(--color-warning-bg);
  border-color: var(--color-warning);
  color: var(--color-warning-on);
}

.tag-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-micro);
  color: var(--color-warning-on);
}

.dot-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-warning);
  animation: dotPulse 1.4s infinite ease-in-out;
}

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* 输入框容器 */
.input-box {
  display: flex;
  align-items: flex-end;
  background: var(--surface-container-low);
  border: 1px solid var(--outline);
  border-radius: var(--radius-full);
  padding: 8px 8px 8px 16px;
  transition: all var(--duration-fast) var(--ease-default);
}

.input-box:focus-within {
  background: var(--surface-card);
  border-color: var(--slate-300);
  box-shadow: var(--shadow-sm);
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

/* Input area icon buttons */
.input-box .icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-default);
}

.input-box .icon-btn:hover {
  background: var(--surface-hover);
  color: var(--color-text-primary);
}

.input-textarea {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  resize: none;
  font-size: var(--text-body);
  line-height: 1.6;
  color: var(--color-text-primary);
  padding: 8px 12px;
  min-height: 24px;
  max-height: 200px;
  font-family: inherit;
}

.input-textarea::placeholder {
  color: var(--color-text-disabled);
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
  transition: all var(--duration-fast) var(--ease-default);
}

.send-btn {
  background: var(--color-text-primary);
  color: var(--surface-card);
}

.send-btn:hover:not(:disabled) {
  background: var(--slate-700);
  transform: scale(1.05);
}

.send-btn:disabled {
  background: var(--slate-200);
  color: var(--color-text-disabled);
  cursor: not-allowed;
}

.stop-btn {
  background: var(--color-error);
  color: #fff;
}

.stop-btn:hover {
  background: #b91c1c;
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
  font-size: var(--text-micro);
  color: var(--color-text-disabled);
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
