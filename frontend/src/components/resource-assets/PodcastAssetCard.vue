<template>
  <div class="podcast-asset-card" :class="`is-${asset.status || 'pending'}`">
    <div class="card-header">
      <div class="card-title-row">
        <el-icon :size="18" class="card-icon"><Headset /></el-icon>
        <div class="card-title-block">
          <h4 class="card-title">播客音频解说</h4>
          <p v-if="asset.podcastStyle || asset.provider" class="card-meta">
            <span v-if="asset.podcastStyle">风格：{{ formatPodcastStyle(asset.podcastStyle) }}</span>
            <span v-if="asset.provider">·</span>
            <span v-if="asset.provider">{{ formatProvider(asset.provider) }}</span>
          </p>
        </div>
      </div>
      <AssetStatusBadge :status="asset.status || 'pending'" />
    </div>

    <!-- 成功：音频播放器 + 下载 -->
    <div v-if="asset.status === 'success' && asset.previewUrl" class="audio-block">
      <audio
        ref="audioEl"
        :src="asset.previewUrl"
        controls
        preload="metadata"
        class="audio-player"
      />
      <div class="audio-meta">
        <span v-if="asset.sizeBytes" class="meta-chip">{{ formatSize(asset.sizeBytes) }}</span>
        <span v-if="asset.durationSeconds" class="meta-chip">{{ formatDuration(asset.durationSeconds) }}</span>
        <span class="meta-chip meta-ai">AI 合成语音</span>
      </div>
      <el-button
        :icon="Download"
        size="small"
        plain
        @click="handleDownload"
      >
        下载 MP3
      </el-button>
    </div>

    <!-- 生成中 / 等待中 -->
    <div v-else-if="asset.status === 'generating' || asset.status === 'pending'" class="state-block">
      <p class="state-text">正在合成音频解说，请稍候…</p>
    </div>

    <!-- 失败 -->
    <div v-else-if="asset.status === 'failed'" class="state-block state-failed">
      <p class="state-text">
        <span v-if="asset.errorMessage">{{ asset.errorMessage }}</span>
        <span v-else>音频生成失败</span>
      </p>
      <p v-if="asset.errorCode" class="state-subtext">错误码：{{ asset.errorCode }}</p>
      <el-button
        type="primary"
        size="small"
        :loading="retrying"
        :icon="RefreshLeft"
        @click="handleRetry"
      >
        重新生成
      </el-button>
    </div>

    <!-- 已取消 / 未知状态 -->
    <div v-else class="state-block">
      <p class="state-text">当前资产状态：{{ asset.status || '未知' }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Headset, RefreshLeft } from '@element-plus/icons-vue'
import AssetStatusBadge from './AssetStatusBadge.vue'
import { retryResourceAsset } from '@/api/ai'

const props = defineProps({
  /**
   * 资产 VO（audio 类型）
   */
  asset: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['retry-success', 'retry-failed'])

const audioEl = ref(null)
const retrying = ref(false)

const PODCAST_STYLE_MAP = {
  teacher: '教师讲解',
  review: '复习串讲',
  story: '故事化叙述'
}

const PROVIDER_MAP = {
  'xfyun-tts': '讯飞在线语音合成'
}

function formatPodcastStyle(style) {
  return PODCAST_STYLE_MAP[style] || style
}

function formatProvider(provider) {
  return PROVIDER_MAP[provider] || provider
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

function formatDuration(seconds) {
  if (!seconds) return ''
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${sec.toString().padStart(2, '0')}`
}

function handleDownload() {
  if (!props.asset.downloadUrl) {
    ElMessage.warning('下载链接不存在')
    return
  }
  const link = document.createElement('a')
  link.href = props.asset.downloadUrl
  link.download = props.asset.fileName || `podcast-${props.asset.assetId}.mp3`
  // download 属性对跨源不生效，但同源 /api 走 inline 响应仍可触发下载
  link.target = '_blank'
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  link.remove()
}

async function handleRetry() {
  if (!props.asset.assetId) return
  retrying.value = true
  try {
    const updated = await retryResourceAsset(props.asset.assetId)
    ElMessage.success('已重新生成音频')
    emit('retry-success', updated)
  } catch (err) {
    ElMessage.error(err?.message || '重试失败，请稍后再试')
    emit('retry-failed', err)
  } finally {
    retrying.value = false
  }
}
</script>

<style scoped>
.podcast-asset-card {
  padding: var(--space-4);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.podcast-asset-card.is-failed {
  border-color: var(--color-error-bg);
  background: var(--color-error-bg);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.card-icon {
  color: var(--color-primary);
}

.card-title {
  margin: 0;
  font-size: var(--text-ui);
  font-weight: 600;
  color: var(--color-text-primary);
}

.card-meta {
  margin: 2px 0 0;
  display: flex;
  gap: 4px;
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.audio-block {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.audio-player {
  width: 100%;
  height: 40px;
}

.audio-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.meta-chip {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--surface-hover);
  color: var(--color-text-secondary);
  font-size: var(--text-micro);
}

.meta-chip.meta-ai {
  background: var(--bg-tag-green);
  color: var(--color-primary);
}

.state-block {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
}

.state-failed {
  background: var(--bg-card);
  border: 1px dashed var(--color-error);
}

.state-text {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--text-ui);
  line-height: 1.6;
}

.state-failed .state-text {
  color: var(--color-error-on);
}

.state-subtext {
  margin: 0;
  color: var(--color-text-tertiary);
  font-size: var(--text-micro);
}

@media (max-width: 767px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
