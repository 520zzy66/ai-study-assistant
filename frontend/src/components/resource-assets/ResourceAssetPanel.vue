<template>
  <div class="resource-asset-panel">
    <div v-if="!hasAnyAsset" class="panel-empty">
      <el-icon :size="24"><Collection /></el-icon>
      <p>本次资源包未生成多模态资产</p>
    </div>

    <template v-else>
      <!-- 播客音频区 -->
      <section v-if="audioAssets.length > 0" class="panel-section">
        <header class="section-header">
          <h3 class="section-title">
            <el-icon :size="16"><Headset /></el-icon>
            播客音频
          </h3>
          <span class="section-count">{{ audioAssets.length }} 个</span>
        </header>
        <div class="audio-list">
          <PodcastAssetCard
            v-for="asset in audioAssets"
            :key="asset.assetId"
            :asset="asset"
            @retry-success="onRetrySuccess"
            @retry-failed="onRetryFailed"
          />
        </div>
      </section>

      <!-- 知识配图区 -->
      <section v-if="imageAssets.length > 0" class="panel-section">
        <header class="section-header">
          <h3 class="section-title">
            <el-icon :size="16"><PictureFilled /></el-icon>
            知识配图
          </h3>
          <span class="section-count">{{ imageAssets.length }} 个</span>
        </header>
        <ImageAssetGallery
          :assets="imageAssets"
          @retry-success="onRetrySuccess"
          @retry-failed="onRetryFailed"
        />
      </section>

      <!-- 多模态失败汇总提示（spec §8.4 + §11.2） -->
      <p v-if="hasFailedAsset" class="panel-footnote">
        部分多模态资产生成失败，可点击"重新生成"重试；
        若持续失败，请检查讯飞配置或内容审核策略。
      </p>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Collection, Headset, PictureFilled } from '@element-plus/icons-vue'
import PodcastAssetCard from './PodcastAssetCard.vue'
import ImageAssetGallery from './ImageAssetGallery.vue'

const props = defineProps({
  /**
   * 资产 VO 列表（同时包含 audio 和 image 类型）
   */
  assets: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['retry-success', 'retry-failed'])

/**
 * 给资产的 previewUrl/downloadUrl 追加 access_token query 参数。
 * 浏览器 <img>/<audio>/<a download> 标签无法携带 Authorization 头，
 * 后端 JwtAuthenticationFilter 仅对 GET 请求允许 access_token query 参数。
 */
function appendAccessToken(asset) {
  if (!asset || (!asset.previewUrl && !asset.downloadUrl)) return asset
  const token = localStorage.getItem('token')
  if (!token) return asset
  const sep = (url) => (url.includes('?') ? '&' : '?')
  const next = { ...asset }
  if (asset.previewUrl) {
    next.previewUrl = asset.previewUrl + sep(asset.previewUrl) + 'access_token=' + encodeURIComponent(token)
  }
  if (asset.downloadUrl) {
    next.downloadUrl = asset.downloadUrl + sep(asset.downloadUrl) + 'access_token=' + encodeURIComponent(token)
  }
  return next
}

const audioAssets = computed(() => {
  if (!props.assets) return []
  return props.assets.filter(a => a.assetType === 'audio').map(appendAccessToken)
})

const imageAssets = computed(() => {
  if (!props.assets) return []
  return props.assets.filter(a => a.assetType === 'image').map(appendAccessToken)
})

const hasAnyAsset = computed(() => audioAssets.value.length > 0 || imageAssets.value.length > 0)

const hasFailedAsset = computed(() => {
  return props.assets?.some(a => a.status === 'failed')
})

function onRetrySuccess(updated) {
  emit('retry-success', updated)
}

function onRetryFailed(err) {
  emit('retry-failed', err)
}
</script>

<style scoped>
.resource-asset-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.panel-empty {
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-tertiary);
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
}

.panel-empty p {
  margin: 0;
  font-size: var(--text-ui);
}

.panel-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.section-title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin: 0;
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.section-count {
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.audio-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.panel-footnote {
  margin: 0;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-warning-bg);
  color: var(--color-warning-on);
  font-size: var(--text-small);
  line-height: 1.6;
}

@media (max-width: 767px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
