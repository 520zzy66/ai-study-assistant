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
 * 流式生成文档总结（SSE Token Streaming）
 * 使用 fetch + ReadableStream 实现逐 token 渲染（打字机效果）
 *
 * @param {number} materialId 资料ID
 * @param {Object} callbacks { onToken(token, fullText), onComplete(fullText), onError(error) }
 * @returns {Function} abort() - 调用以取消请求
 */
export function generateSummaryStream(materialId, callbacks) {
  const controller = new AbortController()
  const token = localStorage.getItem('token')

  fetch(`/api/ai/summary/stream/${materialId}`, {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token}`
    },
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok) {
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

        // SSE 事件以 \n\n 分隔
        const events = buffer.split('\n\n')
        buffer = events.pop() || ''

        for (const event of events) {
          if (streamDone) break
          const dataLines = event.split('\n')
            .filter(line => line.startsWith('data:'))
            .map(line => line.slice(5).trim())

          if (dataLines.length === 0) continue

          const chunk = dataLines.join('\n')

          if (chunk === '[DONE]') {
            streamDone = true
            break
          }

          fullText += chunk
          callbacks.onToken?.(chunk, fullText)
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
 * 生成思维导图
 * @param {number} materialId 资料ID
 */
export async function generateMindMap(materialId) {
  const res = await api.post(`/ai/summary/mindmap/${materialId}`)
  return res.data
}

/**
 * 获取已生成的思维导图
 * @param {number} materialId 资料ID
 */
export async function getMindMap(materialId) {
  const res = await api.get(`/ai/summary/mindmap/${materialId}`)
  return res.data
}

/**
 * 保存用户编辑后的思维导图
 * @param {number} materialId 资料ID
 * @param {string} mindMapJson 思维导图 JSON 字符串
 */
export async function updateMindMap(materialId, mindMapJson) {
  const res = await api.put(`/ai/summary/mindmap/${materialId}`, { mindMap: mindMapJson })
  return res.data
}

/**
 * 生成文件夹思维导图
 * @param {number} folderId 文件夹ID
 */
export async function generateFolderMindMap(folderId) {
  const res = await api.post(`/ai/summary/folder/mindmap/${folderId}`)
  return res.data
}

/**
 * 获取已生成的文件夹思维导图
 * @param {number} folderId 文件夹ID
 */
export async function getFolderMindMap(folderId) {
  const res = await api.get(`/ai/summary/folder/mindmap/${folderId}`)
  return res.data
}

/**
 * 保存用户编辑后的文件夹思维导图
 * @param {number} folderId 文件夹ID
 * @param {string} mindMapJson 思维导图 JSON 字符串
 */
export async function updateFolderMindMap(folderId, mindMapJson) {
  const res = await api.put(`/ai/summary/folder/mindmap/${folderId}`, { mindMap: mindMapJson })
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
    folderId: params.folderId || null,
    temporaryMaterialToken: params.temporaryMaterialToken || null,
    question: params.question,
    history: params.history || [],
    conversationId: params.conversationId || null
  }

  fetch('/api/ai/workflow/ask/stream', {
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

        // SSE 事件以 \n\n 分隔
        const events = buffer.split('\n\n')
        buffer = events.pop() || ''  // 最后一个可能不完整，留到下次处理

        for (const event of events) {
          if (streamDone) break
          // 提取所有 data: 行并拼接（SSE 规范：多行 data 合并）
          const dataLines = event.split('\n')
            .filter(line => line.startsWith('data:'))
            .map(line => line.slice(5).trim())

          if (dataLines.length === 0) continue

          // 合并同一事件的多行 data
          const token = dataLines.join('\n')

          if (token === '[DONE]') {
            streamDone = true
            break
          }

          // 尝试解析 JSON（错误响应 / 路由元数据）
          try {
            const jsonData = JSON.parse(token)
            if (jsonData.type === 'error') {
              callbacks.onError?.(new Error(jsonData.message || '工作流执行失败'))
              streamDone = true
              break
            }
            // 过滤路由元数据 JSON（含 domain/confidence/intent 字段）
            if ('domain' in jsonData && 'confidence' in jsonData) {
              continue
            }
          } catch {
            // 普通 token 流式响应，忽略解析错误
          }
          fullText += token
          callbacks.onToken?.(token, fullText)
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

/**
 * 获取学习计划列表
 * @returns {Promise<Array>} 当前用户的学习计划，按创建时间倒序
 */
export async function listPlans() {
  const res = await api.get('/ai/plan')
  return res.data
}

// ==================== 异步任务 API ====================

/**
 * 异步生成练习题（立即返回 taskId，后台执行）
 * @param {number|null} materialId 资料ID（与 folderId 二选一）
 * @param {Object} params { choiceCount, judgeCount, shortAnswerCount, difficulty, folderId }
 */
export async function generateQuizAsync(materialId, params) {
  // 如果 params 中包含 folderId，使用文件夹级别的接口
  if (params.folderId) {
    const res = await api.post(`/ai/task/quiz/folder/${params.folderId}`, params)
    return res.data
  }
  const res = await api.post(`/ai/task/quiz/${materialId}`, params)
  return res.data
}

/**
 * 异步生成文档总结
 * @param {number|null} materialId 资料ID（与 folderId 二选一）
 * @param {Object} options { force, folderId }
 */
export async function generateSummaryAsync(materialId, options = {}) {
  const force = options.force || false
  // 如果 options 中包含 folderId，使用文件夹级别的接口
  if (options.folderId) {
    const res = await api.post(`/ai/task/summary/folder/${options.folderId}`, { force })
    return res.data
  }
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
 * 异步生成个性化资源包
 * @param {Object} params { materialId, goal, examDate, dailyHours, difficulty, includeSummary, includeMindMap, includeQuiz, includePlan, includeMultimodalScript }
 */
export async function generateResourcePackageAsync(params) {
  const res = await api.post('/ai/task/resource-package', params)
  return res.data
}

/**
 * 获取最近生成的资源包任务
 */
export async function listResourcePackageTasks(size = 10) {
  const res = await api.get('/ai/task/resource-package', { params: { size } })
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

// ==================== 多模态资源资产 API（spec §10.1） ====================

/**
 * 查询多模态资产能力（发音人列表、图片风格、开关状态等）。
 * 前端据此控制表单拓展开关的可用性和默认值。
 * @returns {Promise<Object>} { ttsEnabled, imageEnabled, voices, imageStyles, maxImageCount, defaultVoice, defaultImageStyle }
 */
export async function getResourceAssetCapabilities() {
  const res = await api.get('/ai/resource-assets/capabilities')
  return res.data
}

/**
 * 查询资源包下的多模态资产列表（音频 + 图片）。
 * 仅返回当前用户拥有的资产。
 * @param {string} packageId 资源包 ID
 * @returns {Promise<Array>} 资产 VO 列表
 */
export async function listResourcePackageAssets(packageId) {
  const res = await api.get(`/ai/resource-packages/${packageId}/assets`)
  return res.data
}

/**
 * 重试 failed 状态的资产，复用原始脚本/提示词重新生成。
 * @param {string} assetId 资产 UUID
 * @returns {Promise<Object>} 更新后的资产 VO
 */
export async function retryResourceAsset(assetId) {
  const res = await api.post(`/ai/resource-assets/${assetId}/retry`)
  return res.data
}
