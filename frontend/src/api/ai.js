import api from '@/api'

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
 * 流式文档问答
 * @param {Object} params { materialId, question }
 */
export async function askQuestionStream(params) {
  return api.post('/ai/qa/stream', params, {
    responseType: 'stream',
    headers: { Accept: 'text/event-stream' }
  })
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
