import api from '@/api'

/**
 * 错题本 API 模块
 */

/**
 * 获取错题列表
 * @param {Object} params { materialId, isMastered, page, size }
 */
export async function getWrongQuestions(params) {
  const res = await api.get('/ai/quiz/wrong', { params })
  return res.data
}

/**
 * 标记错题已掌握
 * @param {number} id 错题记录ID
 */
export async function markWrongQuestionMastered(id) {
  const res = await api.put(`/ai/quiz/wrong/${id}/master`)
  return res.data
}
