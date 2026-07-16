import api from './index'

/**
 * 用户画像 API
 */

/**
 * 获取当前用户画像
 * @returns {Promise} 用户画像数据
 */
export function getUserProfile() {
  return api.get('/user/learning-profile')
}

/**
 * 更新用户画像
 * @param {Object} data - 画像数据（空字段不覆盖已有值）
 * @param {number} data.age - 年龄
 * @param {string} data.education - 学籍
 * @param {string} data.targetExam - 目标考试
 * @param {string} data.studySubject - 学习科目
 * @param {number} data.dailyStudyGoalMinutes - 每日学习目标（分钟）
 * @param {string} data.preferredDifficulty - 难度偏好 easy/normal/hard
 * @param {string} data.learningStyle - 学习风格 visual/auditory/practice
 * @param {string[]} data.weakPoints - 薄弱知识点
 * @param {string[]} data.strongPoints - 擅长知识点
 * @returns {Promise}
 */
export function updateUserProfile(data) {
  return api.put('/user/learning-profile', data)
}
