<template>
  <div class="image-asset-gallery">
    <div v-if="!assets || assets.length === 0" class="gallery-empty">
      暂无知识配图
    </div>

    <div v-else class="gallery-grid">
      <div
        v-for="asset in assets"
        :key="asset.assetId"
        class="gallery-item"
        :class="`is-${asset.status || 'pending'}`"
      >
        <!-- 成功：图片预览 -->
        <div v-if="asset.status === 'success' && asset.previewUrl" class="image-frame">
          <el-image
            :src="asset.previewUrl"
            :preview-src-list="successPreviewUrls"
            :initial-index="getInitialIndex(asset.assetId)"
            fit="cover"
            loading="lazy"
            class="image-content"
            hide-on-click-modal
            :preview-teleported="true"
          >
            <template #placeholder>
              <div class="image-loading">加载中…</div>
            </template>
            <template #error>
              <div class="image-error">图片加载失败</div>
            </template>
          </el-image>
          <span class="ai-badge">AI 生成</span>
        </div>

        <!-- 生成中 -->
        <div v-else-if="asset.status === 'generating' || asset.status === 'pending'" class="image-frame image-placeholder">
          <el-icon :size="28" class="is-loading"><Loading /></el-icon>
          <span>正在生成配图…</span>
        </div>

        <!-- 失败 -->
        <div v-else-if="asset.status === 'failed'" class="image-frame image-failed">
          <el-icon :size="24"><PictureFilled /></el-icon>
          <span class="failed-title">配图生成失败</span>
          <span v-if="asset.errorMessage" class="failed-reason">{{ asset.errorMessage }}</span>
          <el-button
            type="primary"
            size="small"
            :loading="retryingId === asset.assetId"
            :icon="RefreshLeft"
            plain
            @click="handleRetry(asset)"
          >
            重新生成
          </el-button>
        </div>

        <!-- 其他状态 -->
        <div v-else class="image-frame image-placeholder">
          <el-icon :size="24"><PictureFilled /></el-icon>
          <span>状态：{{ asset.status || '未知' }}</span>
        </div>

        <!-- 文字说明区 -->
        <div class="item-meta">
          <div class="item-meta-row">
            <span class="item-role">{{ formatRole(asset.assetRole) }}</span>
            <AssetStatusBadge :status="asset.status || 'pending'" />
          </div>
          <p v-if="asset.assetRole === 'cover'" class="item-title">资源包封面</p>
          <p v-else-if="asset.assetRole === 'explanation' && asset.promptSummary" class="item-title">
            {{ asset.promptSummary }}
          </p>
          <div v-if="asset.status === 'success'" class="item-actions">
            <span v-if="asset.sizeBytes" class="item-chip">{{ formatSize(asset.sizeBytes) }}</span>
            <el-button
              :icon="Download"
              size="small"
              text
              @click="handleDownload(asset)"
            >
              下载
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Loading, PictureFilled, RefreshLeft } from '@element-plus/icons-vue'
import AssetStatusBadge from './AssetStatusBadge.vue'
import { retryResourceAsset } from '@/api/ai'

const props = defineProps({
  /**
   * 图片资产 VO 列表
   */
  assets: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['retry-success', 'retry-failed'])

const retryingId = ref(null)

const ROLE_MAP = {
  cover: '封面',
  explanation: '解析图'
}

function formatRole(role) {
  return ROLE_MAP[role] || role || '配图'
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const successPreviewUrls = computed(() => {
  if (!props.assets) return []
  return props.assets
    .filter(a => a.status === 'success' && a.previewUrl)
    .map(a => a.previewUrl)
})

function getInitialIndex(assetId) {
  const idx = successPreviewUrls.value.findIndex(
    url => props.assets.find(a => a.assetId === assetId)?.previewUrl === url
  )
  return idx >= 0 ? idx : 0
}

function handleDownload(asset) {
  if (!asset.downloadUrl) {
    ElMessage.warning('下载链接不存在')
    return
  }
  const link = document.createElement('a')
  link.href = asset.downloadUrl
  link.download = asset.fileName || `image-${asset.assetId}`
  link.target = '_blank'
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  link.remove()
}

async function handleRetry(asset) {
  if (!asset.assetId) return
  retryingId.value = asset.assetId
  try {
    const updated = await retryResourceAsset(asset.assetId)
    ElMessage.success('已重新生成配图')
    emit('retry-success', updated)
  } catch (err) {
    ElMessage.error(err?.message || '重试失败，请稍后再试')
    emit('retry-failed', err)
  } finally {
    retryingId.value = null
  }
}
</script>

<style scoped>
.image-asset-gallery {
  width: 100%;
}

.gallery-empty {
  padding: var(--space-5);
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: var(--text-ui);
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-3);
}

.gallery-item {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.gallery-item:hover {
  border-color: var(--border-color-hover);
  box-shadow: var(--shadow-sm);
}

.gallery-item.is-failed {
  border-color: var(--color-error-bg);
}

.image-frame {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: var(--surface-container-low);
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-content {
  width: 100%;
  height: 100%;
}

.image-placeholder,
.image-failed {
  flex-direction: column;
  gap: var(--space-2);
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
  padding: var(--space-3);
  text-align: center;
}

.image-failed {
  color: var(--color-error-on);
  background: var(--color-error-bg);
}

.failed-title {
  font-weight: 600;
  color: var(--color-error-on);
}

.failed-reason {
  font-size: var(--text-micro);
  color: var(--color-text-secondary);
  line-height: 1.5;
  max-height: 40px;
  overflow: hidden;
}

.image-loading,
.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
  background: var(--surface-container-low);
}

.ai-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.55);
  color: #ffffff;
  font-size: var(--text-micro);
  font-weight: 500;
  backdrop-filter: blur(4px);
}

.item-meta {
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.item-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.item-role {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--bg-tag-blue);
  color: var(--color-tag-blue);
  font-size: var(--text-micro);
  font-weight: 500;
}

.item-title {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.item-chip {
  color: var(--color-text-tertiary);
  font-size: var(--text-micro);
}

@media (max-width: 767px) {
  .gallery-grid {
    grid-template-columns: 1fr;
  }
}
</style>
