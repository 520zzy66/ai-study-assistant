import api from '@/api'

/**
 * 资料文件夹 API
 */

/**
 * 获取当前用户的文件夹树
 * @returns {Promise<Array<FolderVO>>}
 */
export async function getFolderTree() {
  const res = await api.get('/folders/tree')
  return res.data?.data || res.data || []
}

/**
 * 创建文件夹
 * @param {Object} data { name, parentId? }
 * @returns {Promise<FolderVO>}
 */
export async function createFolder(data) {
  const res = await api.post('/folders', data)
  return res.data?.data || res.data
}

/**
 * 更新文件夹（重命名 / 移动）
 * @param {number} id 文件夹 ID
 * @param {Object} data { name, parentId? }
 * @returns {Promise<FolderVO>}
 */
export async function updateFolder(id, data) {
  const res = await api.put(`/folders/${id}`, data)
  return res.data?.data || res.data
}

/**
 * 删除文件夹
 * @param {number} id 文件夹 ID
 * @returns {Promise<void>}
 */
export async function deleteFolder(id) {
  await api.delete(`/folders/${id}`)
}

/**
 * 移动资料到文件夹
 * @param {Array<number>} materialIds 资料 ID 列表
 * @param {number|null} folderId 目标文件夹 ID（null 表示移出文件夹）
 * @returns {Promise<void>}
 */
export async function moveMaterialsToFolder(materialIds, folderId) {
  await api.put('/material/move', { materialIds, folderId })
}
