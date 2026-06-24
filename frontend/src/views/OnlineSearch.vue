<template>
  <div class="os-page">
    <div class="page-header">
      <h2>在线资源</h2>
      <p>搜索互联网学习资料并导入到你的资料库</p>
    </div>

    <!-- Search Bar -->
    <div class="os-search-bar">
      <el-input
        v-model="keyword"
        size="large"
        placeholder="输入关键词搜索学习资源，如「操作系统概念」"
        clearable
        @keydown.enter="handleSearch"
      >
        <template #prefix><el-icon :size="18"><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" size="large" :icon="Search" :loading="searching" @click="handleSearch">
        搜索
      </el-button>
    </div>

    <!-- Results -->
    <div v-loading="searching" class="os-results">
      <template v-if="results.length > 0">
        <div class="os-result-count">找到 {{ results.length }} 条结果</div>
        <div v-for="item in results" :key="item.id" class="os-result-card">
          <div class="os-result-header">
            <div class="os-result-title">
              <el-icon :size="16"><Link /></el-icon>
              <a :href="item.url" target="_blank" rel="noopener">{{ item.title }}</a>
            </div>
            <el-tag size="small" type="info">{{ item.source }}</el-tag>
          </div>
          <p class="os-result-snippet">{{ item.snippet }}</p>
          <div class="os-result-actions">
            <el-button size="small" :icon="Download" :loading="importingId === item.id" @click="handleImport(item)">
              导入到我的资料
            </el-button>
          </div>
        </div>
      </template>
      <AppEmpty v-else-if="!searching && searched" icon="Search" title="未找到结果" description="试试其他关键词" />
      <AppEmpty v-else-if="!searching && !searched" icon="Search" title="搜索在线资源" description="输入关键词搜索互联网学习资料" />
    </div>

    <!-- Import Progress -->
    <el-dialog v-model="importDialogVisible" title="导入进度" width="420px" :close-on-click-modal="false">
      <div v-if="importStatus === 'processing'" style="text-align:center; padding:20px;">
        <el-icon :size="40" class="spinning" color="var(--color-primary)"><Loading /></el-icon>
        <p style="margin-top:16px;">正在抓取并处理网页内容...</p>
        <p style="font-size:var(--text-small);color:var(--color-text-tertiary);">完成后资料将出现在你的资料库中</p>
      </div>
      <div v-else-if="importStatus === 'ready'" style="text-align:center; padding:20px;">
        <el-icon :size="40" color="var(--color-success)"><CircleCheck /></el-icon>
        <p style="margin-top:16px;font-weight:600;">导入成功！</p>
        <el-button type="primary" style="margin-top:12px" @click="goToMaterial">查看资料</el-button>
      </div>
      <div v-else-if="importStatus === 'failed'" style="text-align:center; padding:20px;">
        <el-icon :size="40" color="var(--color-error)"><CircleClose /></el-icon>
        <p style="margin-top:16px;">导入失败</p>
        <p style="font-size:var(--text-small);color:var(--color-text-tertiary);">{{ importErrorMsg }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Link, Download, Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { searchOnline, importResource, getImportStatus } from '@/api/resource'
import AppEmpty from '@/components/common/AppEmpty.vue'

const router = useRouter()
const keyword = ref('')
const searching = ref(false)
const searched = ref(false)
const results = ref([])
const importingId = ref(null)

const importDialogVisible = ref(false)
const importStatus = ref('')
const importErrorMsg = ref('')
const importedMaterialId = ref(null)
let importTimer = null

async function handleSearch() {
  if (!keyword.value.trim()) { ElMessage.warning('请输入搜索关键词'); return }
  searching.value = true
  searched.value = true
  try {
    const data = await searchOnline(keyword.value.trim())
    results.value = data.results || []
  } catch { results.value = [] }
  finally { searching.value = false }
}

async function handleImport(item) {
  importingId.value = item.id
  try {
    const data = await importResource(item.url, item.title)
    importedMaterialId.value = data.materialId
    importStatus.value = 'processing'
    importDialogVisible.value = true
    // 轮询导入状态
    importTimer = setInterval(async () => {
      try {
        const status = await getImportStatus(data.materialId)
        if (status.status === 'ready') {
          importStatus.value = 'ready'
          clearInterval(importTimer)
          importTimer = null
        } else if (status.status === 'failed') {
          importStatus.value = 'failed'
          importErrorMsg.value = status.errorMsg || '未知错误'
          clearInterval(importTimer)
          importTimer = null
        }
      } catch { /* 继续轮询 */ }
    }, 3000)
  } catch { ElMessage.error('导入失败') }
  finally { importingId.value = null }
}

function goToMaterial() {
  importDialogVisible.value = false
  router.push('/material')
}

// 组件卸载时清理轮询定时器
onUnmounted(() => {
  if (importTimer) {
    clearInterval(importTimer)
    importTimer = null
  }
})
</script>

<style scoped>
.os-page { width: 100%; }
.os-search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  max-width: 680px;
}
.os-results { min-height: 300px; }
.os-result-count {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-bottom: 16px;
}
.os-result-card {
  padding: 20px;
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  margin-bottom: 12px;
}
.os-result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}
.os-result-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-body);
  font-weight: 600;
}
.os-result-title a {
  color: var(--color-primary);
  text-decoration: none;
}
.os-result-title a:hover { text-decoration: underline; }
.os-result-snippet {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0 0 12px 0;
}
.os-result-actions { display: flex; justify-content: flex-end; }
.spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
