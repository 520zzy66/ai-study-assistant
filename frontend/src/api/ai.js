import api from '@/api'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/**
 * AI 模块 API
 * 封装所有 AI 相关接口调用
 */

/**
 * 获取 AI 文档总结
 * @param {number} materialId 资料ID
 * @param {boolean} force 是否强制重新生成
 */
export async function generateSummary(materialId, force = false) {
  const res = await api.post(`/ai/summary/${materialId}`, { force })
  return res.data
}

/**
 * RAG 文档问答
 * @param {Object} params { materialId, question, historyLimit }
 */
export async function askQuestion(params) {
  const res = await api.post('/ai/qa', params)
  return res.data
}

/**
 * 流式文档问答（SSE Token Streaming）
 * 使用 fetch + ReadableStream 实现逐 token 渲染
 * 支持多轮对话历史
 *
 * @param {Object} params { materialId, question, history: [{role, content}] }
 * @param {Object} callbacks { onToken(token, fullText), onComplete(fullText), onError(error) }
 * @returns {Function} abort() - 调用以取消请求
 */
export function askQuestionStream(params, callbacks) {
  const controller = new AbortController()
  const token = localStorage.getItem('token')

  // 构建请求体，支持历史对话
  const requestBody = {
    materialId: params.materialId,
    question: params.question,
    history: params.history || []
  }

  fetch('/api/ai/qa/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(requestBody),
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok) {
        // SSE 请求走原生 fetch，需手动处理 401（与 Axios 拦截器行为一致）
        if (response.status === 401) {
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
        }
        const errText = await response.text().catch(() => '')
        callbacks.onError?.(new Error(errText || `HTTP ${response.status}`))
        return
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let fullText = ''
      let streamDone = false

      while (!streamDone) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        // SSE 格式: "data: <token>\n\n"
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const token = line.slice(5).trim()
            if (token === '[DONE]') {
              streamDone = true
              break
            }
            fullText += token
            callbacks.onToken?.(token, fullText)
          }
        }
      }
      callbacks.onComplete?.(fullText)
    })
    .catch((err) => {
      if (err.name === 'AbortError') return
      callbacks.onError?.(err)
    })

  return () => controller.abort()
}

/**
 * 生成练习题
 * @param {number} materialId 资料ID
 * @param {Object} params { choiceCount, judgeCount, shortAnswerCount, difficulty }
 */
export async function generateQuiz(materialId, params) {
  const res = await api.post(`/ai/quiz/${materialId}`, params)
  return res.data
}

/**
 * 提交答案并判分
 * @param {string} batchId 批次ID
 * @param {Array} answers [{ questionId, answer }]
 */
export async function submitAnswers(batchId, answers) {
  const res = await api.post(`/ai/quiz/${batchId}/answer`, { answers })
  return res.data
}

/**
 * 生成学习计划
 * @param {Object} params { goal, examDate, dailyHours, materialIds }
 */
export async function generatePlan(params) {
  const res = await api.post('/ai/plan', params)
  return res.data
}

// ==================== 异步任务 API ====================

/**
 * 异步生成练习题（立即返回 taskId，后台执行）
 */
export async function generateQuizAsync(materialId, params) {
  const res = await api.post(`/ai/task/quiz/${materialId}`, params)
  return res.data
}

/**
 * 异步生成文档总结
 */
export async function generateSummaryAsync(materialId, force = false) {
  const res = await api.post(`/ai/task/summary/${materialId}`, { force })
  return res.data
}

/**
 * 异步生成学习计划
 */
export async function generatePlanAsync(params) {
  const res = await api.post('/ai/task/plan', params)
  return res.data
}

/**
 * 查询任务状态
 */
export async function getTask(taskId) {
  const res = await api.get(`/ai/task/${taskId}`)
  return res.data
}

/**
 * 获取活跃任务列表
 */
export async function getActiveTasks() {
  const res = await api.get('/ai/task')
  return res.data
}

/**
 * 取消任务
 * @param {string} taskId 任务ID
 */
export async function cancelTask(taskId) {
  const res = await api.delete(`/ai/task/${taskId}`)
  return res.data
}

// ==================== 学习计划进度 API ====================

/**
 * 更新学习计划某天的进度
 * @param {number} planId 计划ID
 * @param {number} dayIndex 第几天
 * @param {Object} data { completed, actualHours, note }
 */
export async function updatePlanProgress(planId, dayIndex, data) {
  const res = await api.put(`/ai/plan/${planId}/day/${dayIndex}`, data)
  return res.data
}

/**
 * 获取学习计划的所有进度
 * @param {number} planId 计划ID
 */
export async function getPlanProgressList(planId) {
  const res = await api.get(`/ai/plan/${planId}/progress`)
  return res.data
}

/**
 * 获取学习计划进度统计
 * @param {number} planId 计划ID
 */
export async function getPlanProgressStats(planId) {
  const res = await api.get(`/ai/plan/${planId}/stats`)
  return res.data
}
