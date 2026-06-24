/**
 * 在线资源 API
 */
import api from '@/api'

/** 搜索在线资源 */
export async function searchOnline(keyword, page = 1) {
  const res = await api.get('/resource/search', { params: { keyword, page } })
  return res.data
}

/** 导入在线资源 */
export async function importResource(url, title) {
  const res = await api.post('/resource/import', { url, title })
  return res.data
}

/** 查询导入状态 */
export async function getImportStatus(materialId) {
  const res = await api.get(`/resource/import/${materialId}/status`)
  return res.data
}
