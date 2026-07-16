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

/**
 * 重做错题 — 随机抽取未掌握的错题
 * @param {number} count 抽取数量（默认10）
 */
export async function getRepracticeQuestions(count = 10) {
  const res = await api.get('/ai/quiz/wrong/repractice', { params: { count } })
  return res.data
}

/**
 * 错题统计
 * @param {Object} params { startDate, endDate }
 */
export async function getWrongQuestionStats(params = {}) {
  const res = await api.get('/ai/quiz/wrong/stats', { params })
  return res.data
}

/**
 * 导出错题为 PDF
 * @param {boolean|null} mastered 是否已掌握（null=全部）
 */
export async function exportWrongQuestionsPdf(mastered = null) {
  const params = {}
  if (mastered !== null) params.mastered = mastered
  const res = await api.get('/ai/quiz/wrong/export', {
    params,
    responseType: 'blob'
  })
  return res.data
}
