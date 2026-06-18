import api from '@/api'

/**
 * 历史记录 API 模块
 */

/**
 * 获取对话历史列表
 * @param {Object} params { type, materialId, page, size }
 */
export async function getChatHistory(params) {
  const res = await api.get('/history/chat', { params })
  return res.data
}

/**
 * 获取出题记录列表
 * @param {Object} params { materialId, page, size }
 */
export async function getQuizHistory(params) {
  const res = await api.get('/history/quiz', { params })
  return res.data
}

/**
 * 获取出题批次详情
 * @param {string} batchId 批次ID
 */
export async function getQuizBatchDetail(batchId) {
  const res = await api.get(`/history/quiz/${batchId}`)
  return res.data
}
