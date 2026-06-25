import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * AI 对话状态管理
 * 集中管理对话历史，支持跨页面保持
 */
export const useAiStore = defineStore('ai', () => {
  // ==================== 对话状态 ====================

  /** 消息列表：{ role: 'user'|'assistant', content: string, timestamp: number, material?: {id, name, fileType, fileSize} } */
  const messages = ref([])

  /** 当前正在流式接收的回答 */
  const currentAnswer = ref('')

  /** 是否正在流式接收 */
  const isStreaming = ref(false)

  /** 是否正在加载（等待响应） */
  const loading = ref(false)

  /** 当前关联的资料 ID */
  const selectedMaterialId = ref(null)

  /** 上一次中断的信息 */
  const interruptedInfo = ref(null)

  /** 最后一个问题（用于重新生成） */
  const lastQuestion = ref('')

  /** 错误信息 */
  const error = ref(null)

  /** 中断函数 */
  let abortFn = null

  // ==================== 计算属性 ====================

  /** 是否有对话历史 */
  const hasMessages = computed(() => messages.value.length > 0)

  /** 获取最近 N 轮对话历史（用于发送给后端） */
  const getRecentHistory = (limit = 10) => {
    const history = []
    const recentMessages = messages.value.slice(-(limit * 2))
    for (const msg of recentMessages) {
      history.push({
        role: msg.role,
        content: msg.content
      })
    }
    return history
  }

  // ==================== 操作方法 ====================

  /**
   * 添加用户消息
   * @param {string} content 消息内容
   * @param {Object} [material] 附带的文件信息 { id, name, fileType, fileSize }
   */
  function addUserMessage(content, material) {
    const msg = {
      role: 'user',
      content,
      timestamp: Date.now()
    }
    if (material) {
      msg.material = material
    }
    messages.value.push(msg)
    lastQuestion.value = content
  }

  /**
   * 添加助手消息
   */
  function addAssistantMessage(content) {
    messages.value.push({
      role: 'assistant',
      content,
      timestamp: Date.now()
    })
  }

  /**
   * 设置流式状态
   */
  function setStreaming(streaming) {
    isStreaming.value = streaming
  }

  /**
   * 设置加载状态
   */
  function setLoading(val) {
    loading.value = val
  }

  /**
   * 更新当前流式回答
   */
  function updateCurrentAnswer(text) {
    currentAnswer.value = text
  }

  /**
   * 完成流式回答，将当前回答加入消息列表
   */
  function finishStream() {
    if (currentAnswer.value) {
      addAssistantMessage(currentAnswer.value)
      currentAnswer.value = ''
    }
    isStreaming.value = false
    loading.value = false
    error.value = null
    interruptedInfo.value = null
  }

  /**
   * 设置中断函数
   */
  function setAbortFn(fn) {
    abortFn = fn
  }

  /**
   * 中断当前对话
   * 仅在有活跃流或加载状态时才标记为中断，已完成的回答不会被误判
   */
  function abortCurrent() {
    const wasActive = isStreaming.value || loading.value

    if (abortFn) {
      abortFn()
      abortFn = null
    }

    if (wasActive) {
      // 如果有正在流式接收的内容，保存为中断状态
      if (currentAnswer.value) {
        addAssistantMessage(currentAnswer.value + '\n\n[回答被中断]')
        currentAnswer.value = ''
      }
      isStreaming.value = false
      loading.value = false
      interruptedInfo.value = {
        question: lastQuestion.value,
        time: new Date().toLocaleTimeString(),
        reason: '用户中断了回答'
      }
    } else {
      // 回答已完成，仅做清理，不标记中断
      isStreaming.value = false
      loading.value = false
      currentAnswer.value = ''
    }
  }

  /**
   * 设置错误
   */
  function setError(err) {
    error.value = typeof err === 'string' ? err : err?.message || '请求失败'
    isStreaming.value = false
    loading.value = false
    // 如果有部分内容，也保存下来
    if (currentAnswer.value) {
      addAssistantMessage(currentAnswer.value + '\n\n[回答异常终止]')
      currentAnswer.value = ''
    }
    interruptedInfo.value = {
      question: lastQuestion.value,
      time: new Date().toLocaleTimeString(),
      reason: error.value
    }
  }

  /**
   * 清除错误
   */
  function clearError() {
    error.value = null
  }

  /**
   * 清除中断信息
   */
  function clearInterrupted() {
    interruptedInfo.value = null
  }

  /**
   * 清空所有对话
   */
  function clearMessages() {
    messages.value = []
    currentAnswer.value = ''
    isStreaming.value = false
    loading.value = false
    error.value = null
    interruptedInfo.value = null
    lastQuestion.value = ''
    if (abortFn) {
      abortFn()
      abortFn = null
    }
  }

  /**
   * 删除单条消息（及其对应的回复）
   */
  function deleteMessage(index) {
    if (index >= 0 && index < messages.value.length) {
      messages.value.splice(index, 1)
    }
  }

  /**
   * 设置选中的资料
   */
  function setSelectedMaterial(id) {
    selectedMaterialId.value = id
  }

  return {
    // 状态
    messages,
    currentAnswer,
    isStreaming,
    loading,
    selectedMaterialId,
    interruptedInfo,
    lastQuestion,
    error,
    // 计算属性
    hasMessages,
    // 方法
    getRecentHistory,
    addUserMessage,
    addAssistantMessage,
    setStreaming,
    setLoading,
    updateCurrentAnswer,
    finishStream,
    setAbortFn,
    abortCurrent,
    setError,
    clearError,
    clearInterrupted,
    clearMessages,
    deleteMessage,
    setSelectedMaterial
  }
})
