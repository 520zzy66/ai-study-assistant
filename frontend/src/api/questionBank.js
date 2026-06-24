/**
 * 题库 API
 */
import api from '@/api'

/** 获取批次列表 */
export async function listBatches(params = {}) {
  const res = await api.get('/ai/quiz/bank/batches', { params })
  return res.data
}

/** 获取批次题目 */
export async function getBatchQuestions(batchId) {
  const res = await api.get(`/ai/quiz/bank/batches/${batchId}`)
  return res.data
}

/** 重命名批次 */
export async function renameBatch(batchId, name) {
  await api.put(`/ai/quiz/bank/batches/${batchId}/name`, { name })
}

/** 删除批次 */
export async function deleteBatch(batchId) {
  await api.delete(`/ai/quiz/bank/batches/${batchId}`)
}

/** 切换收藏 */
export async function toggleFavorite(questionId) {
  const res = await api.put(`/ai/quiz/bank/question/${questionId}/favorite`)
  return res.data
}

/** 收藏列表 */
export async function getFavorites(params = {}) {
  const res = await api.get('/ai/quiz/favorites', { params })
  return res.data
}

/** 单题重新作答 */
export async function reAnswer(questionId, answer) {
  const res = await api.post(`/ai/quiz/re-answer/${questionId}`, { answer })
  return res.data
}
