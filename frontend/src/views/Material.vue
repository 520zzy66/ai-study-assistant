<template>
  <div class="material-page">
    <BasePageHeader
      title="学习资料"
      description="管理你的学习资料，支持 AI 总结、问答和出题"
    />

    <!-- Tabs -->
    <div class="material-tabs">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'mine' }"
        @click="activeTab = 'mine'"
      >
        <el-icon><Document /></el-icon>
        我的资料
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'library' }"
        @click="activeTab = 'library'; loadLibrary()"
      >
        <el-icon><Collection /></el-icon>
        资料库
      </button>
    </div>

    <!-- ========== 我的资料 Tab ========== -->
    <template v-if="activeTab === 'mine'">
      <!-- Toolbar -->
      <div class="material-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索资料名称..."
            clearable
            style="width:240px;"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width:130px;" @change="handleSearch">
            <el-option label="处理中" value="processing" />
            <el-option label="可用" value="ready" />
            <el-option label="失败" value="failed" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="large" @click="showUpload = true">
            <el-icon><Upload /></el-icon> 上传资料
          </el-button>
        </div>
      </div>

      <!-- Content Card -->
      <BaseCard :padding="'none'">
        <!-- Skeleton Loading -->
        <div v-if="loading" class="table-loading">
          <el-skeleton :rows="8" animated />
        </div>

        <!-- Empty -->
        <div v-else-if="materialList.length === 0" style="padding:40px 0;">
          <AppEmpty icon="Document" title="暂无学习资料" description="上传你的第一份资料，开始学习之旅">
            <template #action>
              <el-button type="primary" @click="showUpload = true">上传资料</el-button>
            </template>
          </AppEmpty>
        </div>

        <!-- Table -->
        <template v-else>
          <el-table :data="materialList" class="material-table" @row-click="handleView">
            <el-table-column label="文件名" min-width="240">
              <template #default="{ row }">
                <div class="file-cell">
                  <div class="file-icon" :style="{ background: getFileIconBg(row.fileType), color: getFileIconColor(row.fileType) }">
                    <el-icon :size="18"><Document /></el-icon>
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
                <span class="file-type-badge" :style="{ background: getFileIconBg(row.fileType), color: getFileIconColor(row.fileType) }">
                  {{ row.fileType?.toUpperCase() || '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="170">
              <template #default="{ row }">
                <span class="time-text">{{ row.createTime }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small" effect="light">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button v-if="row.status === 'ready'" size="small" text type="primary" @click.stop="goToSummary(row)">总结</el-button>
                  <el-button v-if="row.status === 'ready'" size="small" text type="primary" @click.stop="goToChat(row)">问答</el-button>
                  <el-button size="small" text @click.stop="handleView(row)">查看</el-button>
                  <el-button size="small" text type="danger" @click.stop="handleDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- Pagination -->
          <div v-if="total > 0" class="table-footer">
            <span class="total-text">共 {{ total }} 份资料</span>
            <el-pagination
              v-model:current-page="queryParams.page"
              v-model:page-size="queryParams.size"
              :page-sizes="[10, 20, 50]"
              :total="total"
              layout="sizes, prev, pager, next"
              @size-change="handleSearch"
              @current-change="handleSearch"
            />
          </div>
        </template>
      </BaseCard>
    </template>

    <!-- ========== 资料库 Tab ========== -->
    <template v-if="activeTab === 'library'">
      <!-- Toolbar -->
      <div class="material-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="libraryQuery.keyword"
            placeholder="搜索资料名称..."
            clearable
            style="width:240px;"
            @keyup.enter="loadLibrary"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="libraryQuery.category" placeholder="全部分类" clearable style="width:140px;" @change="loadLibrary">
            <el-option label="学习方法" value="学习方法" />
            <el-option label="计算机基础" value="计算机基础" />
            <el-option label="英语学习" value="英语学习" />
          </el-select>
        </div>
      </div>

      <!-- Content Card -->
      <BaseCard :padding="'none'">
        <div v-if="libraryLoading" class="table-loading">
          <el-skeleton :rows="8" animated />
        </div>

        <div v-else-if="libraryList.length === 0" style="padding:40px 0;">
          <AppEmpty icon="Collection" title="暂无资料" description="资料库正在建设中，敬请期待" />
        </div>

        <template v-else>
          <el-table :data="libraryList" class="material-table" @row-click="handleLibraryView">
            <el-table-column label="资料名称" min-width="260">
              <template #default="{ row }">
                <div class="file-cell">
                  <div class="file-icon" :style="{ background: getFileIconBg(row.fileType), color: getFileIconColor(row.fileType) }">
                    <el-icon :size="18"><Document /></el-icon>
                  </div>
                  <div class="file-info">
                    <div class="file-name truncate">{{ row.originalName }}</div>
                    <div class="file-desc">{{ row.summary ? row.summary.substring(0, 60) + '...' : row.category }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="120">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.category || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="切片数" width="80" align="center">
              <template #default="{ row }">
                <span class="time-text">{{ row.chunkCount || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button size="small" text type="success" @click.stop="handleAddToMine(row)">
                    <el-icon><Plus /></el-icon> 添加到我的资料
                  </el-button>
                  <el-button size="small" text type="primary" @click.stop="goToSummary(row)">总结</el-button>
                  <el-button size="small" text type="primary" @click.stop="goToChat(row)">问答</el-button>
                  <el-button size="small" text type="primary" @click.stop="goToQuiz(row)">出题</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="libraryTotal > 0" class="table-footer">
            <span class="total-text">共 {{ libraryTotal }} 份资料</span>
            <el-pagination
              v-model:current-page="libraryQuery.page"
              v-model:page-size="libraryQuery.size"
              :page-sizes="[10, 20, 50]"
              :total="libraryTotal"
              layout="sizes, prev, pager, next"
              @size-change="loadLibrary"
              @current-change="loadLibrary"
            />
          </div>
        </template>
      </BaseCard>
    </template>

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
            <el-icon class="upload-icon" :size="36"><UploadFilled /></el-icon>
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
    <el-drawer v-model="detailVisible" size="420px">
      <template #header>
        <span class="drawer-title">资料详情</span>
      </template>
      <div v-if="selectedMaterial" class="detail-content">
        <div class="detail-row">
          <span class="detail-label">文件名</span>
          <span class="detail-value">{{ selectedMaterial.originalName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <span class="detail-value">{{ selectedMaterial.fileType?.toUpperCase() || '-' }}</span>
        </div>
        <div v-if="selectedMaterial.fileSize" class="detail-row">
          <span class="detail-label">大小</span>
          <span class="detail-value">{{ formatFileSize(selectedMaterial.fileSize) }}</span>
        </div>
        <div v-if="selectedMaterial.category" class="detail-row">
          <span class="detail-label">分类</span>
          <span class="detail-value">{{ selectedMaterial.category }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <el-tag :type="getStatusType(selectedMaterial.status)" size="small" effect="light">
            {{ getStatusLabel(selectedMaterial.status) }}
          </el-tag>
        </div>
        <div v-if="selectedMaterial.createTime" class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ selectedMaterial.createTime }}</span>
        </div>
        <div v-if="selectedMaterial.chunkCount" class="detail-row">
          <span class="detail-label">切片数</span>
          <span class="detail-value">{{ selectedMaterial.chunkCount }}</span>
        </div>
        <div v-if="selectedMaterial.summary" class="detail-row">
          <span class="detail-label">摘要</span>
          <span class="detail-value detail-summary">{{ selectedMaterial.summary }}</span>
        </div>
        <div class="detail-actions">
          <el-button v-if="selectedMaterial.status === 'ready'" type="primary" @click="goToSummary(selectedMaterial)">AI 总结</el-button>
          <el-button v-if="selectedMaterial.status === 'ready'" @click="goToChat(selectedMaterial)">AI 问答</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMaterialList, uploadMaterial, deleteMaterial, getMaterialDetail, getMaterialLibrary, copyToMyLibrary } from '@/api/material'
import { Upload, UploadFilled, Search, Document, Collection, Plus } from '@element-plus/icons-vue'
import { formatFileSize, getStatusLabel, getStatusType } from '@/utils/format'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

const router = useRouter()
const activeTab = ref('mine')

// ---- 我的资料 ----
const loading = ref(false)
const uploading = ref(false)
const showUpload = ref(false)
const detailVisible = ref(false)
const materialList = ref([])
const total = ref(0)
const uploadRef = ref(null)
const selectedMaterial = ref(null)

const queryParams = reactive({ keyword: '', status: '', page: 1, size: 10 })
const uploadForm = reactive({ file: null, category: '' })

// ---- 资料库 ----
const libraryLoading = ref(false)
const libraryList = ref([])
const libraryTotal = ref(0)
const libraryQuery = reactive({ keyword: '', category: '', page: 1, size: 10 })


function getFileIconBg(type) {
  const map = { pdf: '#fef2f2', doc: '#eff6ff', docx: '#eff6ff', epub: '#fffbeb', md: '#f3f4f6', txt: '#f3f4f6' }
  return map[(type || '').toLowerCase()] || '#f3f4f6'
}

function getFileIconColor(type) {
  const map = { pdf: '#dc2626', doc: '#2563eb', docx: '#2563eb', epub: '#d97706', md: '#6b7280', txt: '#6b7280' }
  return map[(type || '').toLowerCase()] || '#6b7280'
}


// ---- 我的资料 ----
async function handleSearch() {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (params.keyword) { params.fileName = params.keyword; delete params.keyword }
    const data = await getMaterialList(params)
    materialList.value = data.records || []
    total.value = data.total || 0
  } catch { materialList.value = [] }
  finally { loading.value = false }
}

function handleFileChange(file) { uploadForm.file = file.raw }

async function handleUpload() {
  if (!uploadForm.file) { ElMessage.warning('请选择文件'); return }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    if (uploadForm.category) formData.append('category', uploadForm.category)
    await uploadMaterial(formData)
    ElMessage.success('上传成功')
    showUpload.value = false
    uploadForm.file = null; uploadForm.category = ''
    uploadRef.value?.clearFiles()
    handleSearch()
  } finally { uploading.value = false }
}

function handleView(row) { selectedMaterial.value = row; detailVisible.value = true }

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该资料？删除后不可恢复。', '确认删除', { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' })
  try { await deleteMaterial(row.id); ElMessage.success('删除成功'); handleSearch() } catch { /* handled */ }
}

function goToSummary(row) { router.push(`/ai/summary?materialId=${row.id}`) }
function goToChat(row) { router.push(`/ai/chat?materialId=${row.id}`) }
function goToQuiz(row) { router.push(`/ai/quiz?materialId=${row.id}`) }

async function handleAddToMine(row) {
  try {
    await copyToMyLibrary(row.id)
    ElMessage.success('已添加到我的资料库')
    handleSearch() // 刷新我的资料列表
  } catch { /* handled by interceptor */ }
}

// ---- 资料库 ----
async function loadLibrary() {
  libraryLoading.value = true
  try {
    const data = await getMaterialLibrary(libraryQuery)
    libraryList.value = data.records || []
    libraryTotal.value = data.total || 0
  } catch { libraryList.value = [] }
  finally { libraryLoading.value = false }
}

async function handleLibraryView(row) {
  try {
    const data = await getMaterialDetail(row.id)
    selectedMaterial.value = data
    detailVisible.value = true
  } catch { /* handled */ }
}

onMounted(() => handleSearch())
</script>

<style scoped>
.material-page { width: 100%; }

/* Tabs */
.material-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background: var(--surface-container);
  border-radius: var(--radius-lg);
  padding: 4px;
  width: fit-content;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.tab-btn:hover {
  color: var(--color-text-primary);
}

.tab-btn.active {
  background: var(--surface-card);
  color: var(--color-primary);
  box-shadow: var(--shadow-xs);
}

/* Toolbar */
.material-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-loading { padding: 32px 24px; }

.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-name {
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-primary);
}

.file-size {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.file-desc {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: 2px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: var(--text-micro);
  font-weight: 600;
}

.time-text {
  font-size: var(--text-ui);
  color: var(--color-text-secondary);
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 2px;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid var(--outline-variant);
}

.total-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.upload-icon { color: var(--color-text-tertiary); margin-bottom: 8px; }
.upload-text { font-size: var(--text-body); color: var(--color-text-secondary); }
.upload-text em { color: var(--color-primary); font-style: normal; }
.upload-tip { font-size: var(--text-small); color: var(--color-text-tertiary); margin-top: 8px; }

.drawer-title {
  font-size: var(--text-heading-2);
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-content { display: flex; flex-direction: column; gap: 20px; }
.detail-row { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: var(--text-small); color: var(--color-text-tertiary); font-weight: 500; }
.detail-value { font-size: var(--text-body); color: var(--color-text-primary); }
.detail-summary {
  font-size: var(--text-small);
  line-height: 1.6;
  color: var(--color-text-secondary);
  background: var(--surface-container);
  padding: 12px;
  border-radius: var(--radius-md);
}
.detail-actions { display: flex; gap: 12px; margin-top: 8px; }

.material-table :deep(.el-table__body tr) { cursor: pointer; }

@media (max-width: 767px) {
  .material-toolbar { flex-direction: column; align-items: stretch; }
  .toolbar-left { flex-wrap: wrap; }
  .table-footer { flex-direction: column; gap: 12px; align-items: flex-start; }
  .material-tabs { width: 100%; }
  .tab-btn { flex: 1; justify-content: center; }
}
</style>
