/**
 * 格式化工具函数
 * 统一管理项目中的格式化逻辑，避免重复代码
 */

/**
 * 格式化文件大小
 * @param {number} bytes 字节数
 * @returns {string} 格式化后的文件大小
 */
export function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * 获取资料状态标签
 * @param {string} status 状态值
 * @returns {string} 状态标签
 */
export function getStatusLabel(status) {
  return { processing: '处理中', parsing: '解析中', ready: '可用', failed: '失败' }[status] || status
}

/**
 * 获取资料状态类型（Element Plus Tag 类型）
 * @param {string} status 状态值
 * @returns {string} Tag 类型
 */
export function getStatusType(status) {
  return { processing: 'warning', parsing: 'warning', ready: 'success', failed: 'danger' }[status] || 'info'
}
