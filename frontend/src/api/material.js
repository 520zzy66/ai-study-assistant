import api from '@/api'

/**
 * 资料 API 模块
 * 封装资料相关的 API 调用，避免重复代码
 */

/**
 * 获取可用资料列表（状态为 ready）
 * @returns 资料列表
 */
export async function loadReadyMaterials() {
  const res = await api.get('/material/list', { params: { status: 'ready', size: 100 } })
  return res.data.records
}

/**
 * 获取资料列表（带查询参数）
 * @param {Object} queryParams 查询参数
 * @returns 分页结果
 */
export async function getMaterialList(queryParams) {
  const res = await api.get('/material/list', { params: queryParams })
  return res.data
}

/**
 * 上传资料
 * @param {FormData} formData 文件数据
 * @returns 上传结果
 */
export async function uploadMaterial(formData) {
  const res = await api.post('/material/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

/**
 * 删除资料
 * @param {number} id 资料 ID
 * @returns 操作结果
 */
export async function deleteMaterial(id) {
  const res = await api.delete(`/material/${id}`)
  return res.data
}

/**
 * 获取资料详情
 * @param {number} id 资料 ID
 * @returns 资料详情
 */
export async function getMaterialDetail(id) {
  const res = await api.get(`/material/${id}`)
  return res.data
}
