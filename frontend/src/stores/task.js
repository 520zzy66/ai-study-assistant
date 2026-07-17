/**
 * 异步任务全局状态管理
 * 管理后台 AI 生成任务（出题/总结/计划），支持跨页面保持进度
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

const POLL_INTERVAL = 2000 // 2秒轮询
const MAX_RETRY_COUNT = 5  // 最大重试次数

export const useTaskStore = defineStore('task', () => {
  /** @type {import('vue').Ref<Map<string, { timer: number, task: object, callbacks: object, retryCount: number }>>} */
  const taskMap = ref(new Map())

  /**
   * 创建异步任务并开始轮询
   * @param {string} taskId 任务ID
   * @param {string} type 任务类型 (quiz/summary/plan)
   * @param {object} callbacks 回调 { onProgress(progress, message), onSuccess(result), onError(error) }
   */
  function watchTask(taskId, type, callbacks = {}) {
    // 如果已存在，先停止旧轮询
    if (taskMap.value.has(taskId)) {
      stopWatching(taskId)
    }

    const task = { type, callbacks, retryCount: 0 }
    const timer = setInterval(async () => {
      try {
        const res = await api.get(`/ai/task/${taskId}`)
        const data = res.data
        // 成功获取数据，重置重试计数
        task.retryCount = 0
        taskMap.value.set(taskId, { ...task, timer, task: data })

        callbacks.onProgress?.(data.progress, data.message)

        if (data.status === 'success') {
          stopWatching(taskId)
          let result = data.result
          if (typeof result === 'string') {
            try { result = JSON.parse(result) } catch { /* keep as string */ }
          }
          callbacks.onSuccess?.(result)
        } else if (data.status === 'failed' || data.status === 'cancelled') {
          stopWatching(taskId)
          callbacks.onError?.(data.errorMsg || '任务执行失败')
        }
      } catch (err) {
        // 网络错误时增加重试计数
        task.retryCount++
        console.warn(`[TaskStore] 轮询任务 ${taskId} 失败 (${task.retryCount}/${MAX_RETRY_COUNT}):`, err.message)

        if (task.retryCount >= MAX_RETRY_COUNT) {
          // 超过最大重试次数，停止轮询并通知调用方
          stopWatching(taskId)
          callbacks.onError?.('网络连接失败，请检查网络后重试')
        }
      }
    }, POLL_INTERVAL)

    taskMap.value.set(taskId, { ...task, timer, task: { taskId, type, status: 'pending' } })
  }

  /** 停止监听某个任务 */
  function stopWatching(taskId) {
    const entry = taskMap.value.get(taskId)
    if (entry?.timer) {
      clearInterval(entry.timer)
    }
    taskMap.value.delete(taskId)
  }

  /** 获取活跃任务列表 */
  function getActiveTasks() {
    const active = []
    taskMap.value.forEach((entry, taskId) => {
      if (entry.task?.status === 'pending' || entry.task?.status === 'running') {
        active.push({ taskId, ...entry.task })
      }
    })
    return active
  }

  /** 是否有指定类型的活跃任务 */
  function hasActiveTaskOfType(type) {
    return getActiveTasks().some(t => t.type === type)
  }

  /** 获取指定类型的首个活跃任务 */
  function getFirstActiveOfType(type) {
    return getActiveTasks().find(t => t.type === type) || null
  }

  /**
   * 在 App.vue mount 时恢复所有未完成任务
   */
  async function resumePending() {
    try {
      const res = await api.get('/ai/task')
      const tasks = res.data || []
      for (const task of tasks) {
        if (task.status === 'pending' || task.status === 'running') {
          // 恢复轮询（不带回调，因为组件可能未挂载）
          let retryCount = 0
          const timer = setInterval(async () => {
            try {
              const r = await api.get(`/ai/task/${task.taskId}`)
              const data = r.data
              retryCount = 0
              taskMap.value.set(task.taskId, { type: task.type, timer, task: data, retryCount })
              if (data.status === 'success' || data.status === 'failed' || data.status === 'cancelled') {
                stopWatching(task.taskId)
              }
            } catch (err) {
              retryCount++
              console.warn(`[TaskStore] 恢复任务 ${task.taskId} 轮询失败 (${retryCount}/${MAX_RETRY_COUNT})`)
              if (retryCount >= MAX_RETRY_COUNT) {
                stopWatching(task.taskId)
              }
            }
          }, POLL_INTERVAL)
          taskMap.value.set(task.taskId, { type: task.type, timer, task, retryCount: 0 })
        }
      }
    } catch (err) {
      console.warn('[TaskStore] 恢复任务列表失败:', err.message)
    }
  }

  return {
    taskMap,
    watchTask,
    stopWatching,
    getActiveTasks,
    hasActiveTaskOfType,
    getFirstActiveOfType,
    resumePending
  }
})
