<template>
  <div class="material-page">
    <div class="page-header">
      <h2>学习资料</h2>
      <p>管理你的学习资料，支持 AI 总结、问答和出题</p>
    </div>

    <el-card shadow="never" class="material-card">
      <!-- Toolbar -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索资料名称"
            clearable
            style="width: 240px;"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px;" @change="handleSearch">
            <el-option label="处理中" value="processing" />
            <el-option label="可用" value="ready" />
            <el-option label="失败" value="failed" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="showUpload = true">
            <el-icon><Upload /></el-icon>
            上传资料
          </el-button>
        </div>
      </div>

      <!-- Table -->
      <el-table :data="materialList" v-loading="loading" class="material-table">
        <el-table-column label="文件名" min-width="240">
          <template #default="{ row }">
            <div class="file-cell">
              <div class="file-icon">
                <el-icon :size="20"><Document /></el-icon>
              </div>
              <div class="file-info">
                <div class="file-name truncate">{{ row.originalName }}</div>
                <div class="file-size">{{ formatFileSize(row.fileSize) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <span class="file-type">{{ row.fileType?.toUpperCase() || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button
                v-if="row.status === 'ready'"
                size="small"
                text
                type="primary"
                @click="goToSummary(row)"
              >
                总结
              </el-button>
              <el-button
                v-if="row.status === 'ready'"
                size="small"
                text
                type="primary"
                @click="goToChat(row)"
              >
                问答
              </el-button>
              <el-button size="small" text @click="handleView(row)">查看</el-button>
              <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Empty State -->
      <div v-if="!loading && materialList.length === 0" class="table-empty">
        <AppEmpty
          icon="Document"
          title="暂无学习资料"
          description="上传你的第一份资料，开始学习之旅"
          compact
        >
          <template #action>
            <el-button type="primary" @click="showUpload = true">上传资料</el-button>
          </template>
        </AppEmpty>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="table-footer">
        <span class="total-text">共 {{ total }} 份资料</span>
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- Upload Dialog -->
    <el-dialog v-model="showUpload" title="上传资料" width="520px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-position="top">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.doc,.docx,.txt,.md,.mp3,.wav,.mp4"
            :on-change="handleFileChange"
            drag
          >
            <el-icon class="upload-icon" :size="32"><Upload /></el-icon>
            <div class="upload-text">拖拽文件到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="upload-tip">支持 PDF、Word、TXT、Markdown、音视频格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="分类（可选）">
          <el-input v-model="uploadForm.category" placeholder="输入分类便于管理" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="detailVisible" title="资料详情" size="420px">
      <div v-if="selectedMaterial" class="detail-content">
        <div class="detail-section">
          <div class="detail-label">文件名</div>
          <div class="detail-value">{{ selectedMaterial.originalName }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">类型</div>
          <div class="detail-value">{{ selectedMaterial.fileType?.toUpperCase() }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">大小</div>
          <div class="detail-value">{{ formatFileSize(selectedMaterial.fileSize) }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">状态</div>
          <div class="detail-value">
            <el-tag :type="getStatusType(selectedMaterial.status)" size="small">
              {{ getStatusLabel(selectedMaterial.status) }}
            </el-tag>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-label">上传时间</div>
          <div class="detail-value">{{ selectedMaterial.createTime }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">切片数</div>
          <div class="detail-value">{{ selectedMaterial.chunkCount || 0 }}</div>
        </div>
        <div class="detail-actions">
          <el-button v-if="selectedMaterial.status === 'ready'" type="primary" @click="goToSummary(selectedMaterial)">
            AI 总结
          </el-button>
          <el-button v-if="selectedMaterial.status === 'ready'" @click="goToChat(selectedMaterial)">
            AI 问答
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMaterialList, uploadMaterial, deleteMaterial } from '@/api/material'
import AppEmpty from '@/components/common/AppEmpty.vue'

const router = useRouter()

const loading = ref(false)
const uploading = ref(false)
const showUpload = ref(false)
const detailVisible = ref(false)
const materialList = ref([])
const total = ref(0)
const uploadRef = ref(null)
const selectedMaterial = ref(null)

const queryParams = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10
})

const uploadForm = reactive({
  file: null,
  category: ''
})

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function getStatusLabel(status) {
  const map = { processing: '处理中', ready: '可用', failed: '失败' }
  return map[status] || status
}

function getStatusType(status) {
  const map = { processing: 'warning', ready: 'success', failed: 'danger' }
  return map[status] || 'info'
}

async function handleSearch() {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (params.keyword) {
      params.fileName = params.keyword
      delete params.keyword
    }
    const data = await getMaterialList(params)
    materialList.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    materialList.value = []
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.page = 1
  handleSearch()
}

function handleFileChange(file) {
  uploadForm.file = file.raw
}

async function handleUpload() {
  if (!uploadForm.file) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    if (uploadForm.category) {
      formData.append('category', uploadForm.category)
    }
    await uploadMaterial(formData)
    ElMessage.success('上传成功')
    showUpload.value = false
    uploadForm.file = null
    uploadForm.category = ''
    uploadRef.value?.clearFiles()
    handleSearch()
  } finally {
    uploading.value = false
  }
}

function handleView(row) {
  selectedMaterial.value = row
  detailVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该资料？删除后不可恢复。', '确认删除', {
    type: 'warning',
    confirmButtonText: '删除',
    confirmButtonClass: 'el-button--danger'
  })
  try {
    await deleteMaterial(row.id)
    ElMessage.success('删除成功')
    handleSearch()
  } catch (error) {
    // handled by interceptor
  }
}

function goToSummary(row) {
  router.push(`/ai/summary?materialId=${row.id}`)
}

function goToChat(row) {
  router.push(`/ai/chat?materialId=${row.id}`)
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.material-page {
  width: 100%;
}

.material-card {
  border-radius: var(--radius-lg);
}

.material-card :deep(.el-card__body) {
  padding: var(--space-5);
}

.material-table {
  margin-top: var(--space-4);
}

.file-cell {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.file-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--surface-container);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-info {
  min-width: 0;
}

.file-name {
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--color-text-primary);
}

.file-size {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: var(--space-1);
}

.file-type {
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--surface-container);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.time-text {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.action-btns {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.table-empty {
  border-top: 1px solid var(--outline-variant);
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--outline-variant);
}

.total-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.upload-icon {
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-2);
}

.upload-text {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.upload-text em {
  color: var(--color-primary);
  font-style: normal;
}

.upload-tip {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: var(--space-2);
}

/* Detail Drawer */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.detail-label {
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--color-text-tertiary);
}

.detail-value {
  font-size: var(--text-body);
  color: var(--color-text-primary);
}

.detail-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

@media (max-width: 767px) {
  .table-footer {
    flex-direction: column;
    gap: var(--space-3);
    align-items: flex-start;
  }
}
</style>
