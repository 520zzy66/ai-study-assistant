<template>
  <div class="wrong-question-page">
    <div class="page-header">
      <h2>错题本</h2>
      <p>回顾错题，针对性复习，巩固薄弱知识点</p>
    </div>

    <el-card shadow="never" class="wrong-question-card">
      <!-- Toolbar -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="queryParams.materialId" placeholder="全部资料" clearable style="width: 220px;" @change="handleSearch">
            <el-option
              v-for="item in materialList"
              :key="item.id"
              :label="item.originalName"
              :value="item.id"
            />
          </el-select>
          <el-select v-model="queryParams.isMastered" placeholder="全部状态" clearable style="width: 140px;" @change="handleSearch">
            <el-option label="未掌握" :value="false" />
            <el-option label="已掌握" :value="true" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button text @click="handleReset">重置</el-button>
        </div>
      </div>

      <!-- Table -->
      <el-table :data="wrongList" v-loading="loading" class="wrong-table">
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <span class="type-tag">{{ getTypeLabel(row.questionType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="题目" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="question-text">{{ row.question }}</span>
          </template>
        </el-table-column>
        <el-table-column label="你的答案" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="answer-wrong">{{ row.userAnswer }}</span>
          </template>
        </el-table-column>
        <el-table-column label="正确答案" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="answer-correct">{{ row.correctAnswer }}</span>
          </template>
        </el-table-column>
        <el-table-column label="错误次数" width="100" align="center">
          <template #default="{ row }">
            <span class="wrong-count">{{ row.wrongCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="source-text">{{ row.materialName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button size="small" text type="primary" @click="handleView(row)">详情</el-button>
              <el-button
                v-if="!row.isMastered"
                size="small"
                text
                type="success"
                @click="handleMaster(row)"
              >
                标记掌握
              </el-button>
              <span v-else class="mastered-badge">已掌握</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Empty -->
      <div v-if="!loading && wrongList.length === 0" class="table-empty">
        <AppEmpty
          icon="Notebook"
          title="暂无错题"
          description="完成 AI 出题练习，答错的题目会自动收录"
          compact
        />
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="table-footer">
        <span class="total-text">共 {{ total }} 道错题</span>
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

    <!-- Detail Drawer -->
    <el-drawer v-model="showDetail" title="错题详情" size="480px">
      <div v-if="currentQuestion" class="detail-content">
        <div class="detail-section">
          <div class="detail-label">题目</div>
          <div class="detail-value">{{ currentQuestion.question }}</div>
        </div>
        <div class="detail-row">
          <div class="detail-section">
            <div class="detail-label">你的答案</div>
            <div class="detail-value answer-wrong">{{ currentQuestion.userAnswer }}</div>
          </div>
          <div class="detail-section">
            <div class="detail-label">正确答案</div>
            <div class="detail-value answer-correct">{{ currentQuestion.correctAnswer }}</div>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-label">题型</div>
          <div class="detail-value">{{ getTypeLabel(currentQuestion.questionType) }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">错误次数</div>
          <div class="detail-value">{{ currentQuestion.wrongCount }}</div>
        </div>
        <div v-if="currentQuestion.explanation" class="detail-section">
          <div class="detail-label">解析</div>
          <div class="detail-value">{{ currentQuestion.explanation }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getWrongQuestions, markWrongQuestionMastered } from '@/api/quiz'
import { loadReadyMaterials } from '@/api/material'
import AppEmpty from '@/components/common/AppEmpty.vue'

const loading = ref(false)
const wrongList = ref([])
const total = ref(0)
const materialList = ref([])
const showDetail = ref(false)
const currentQuestion = ref(null)

const queryParams = reactive({
  materialId: '',
  isMastered: '',
  page: 1,
  size: 10
})

function getTypeLabel(type) {
  const map = { choice: '单选', judge: '判断', short_answer: '简答' }
  return map[type] || type
}

async function loadMaterials() {
  try {
    materialList.value = await loadReadyMaterials()
  } catch (error) {
    materialList.value = []
  }
}

async function handleSearch() {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (params.isMastered === '') delete params.isMastered
    const data = await getWrongQuestions(params)
    // 兼容数组和分页格式
    if (Array.isArray(data)) {
      wrongList.value = data
      total.value = data.length
    } else {
      wrongList.value = data.records || []
      total.value = data.total || 0
    }
  } catch (error) {
    wrongList.value = []
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryParams.materialId = ''
  queryParams.isMastered = ''
  queryParams.page = 1
  handleSearch()
}

function handleView(row) {
  currentQuestion.value = row
  showDetail.value = true
}

async function handleMaster(row) {
  try {
    await markWrongQuestionMastered(row.id)
    ElMessage.success('已标记为掌握')
    handleSearch()
  } catch (error) {
    // handled by interceptor
  }
}

onMounted(() => {
  loadMaterials()
  handleSearch()
})
</script>

<style scoped>
.wrong-question-page {
  width: 100%;
}

.wrong-question-card {
  border-radius: var(--radius-lg);
}

.wrong-question-card :deep(.el-card__body) {
  padding: var(--space-5);
}

.wrong-table {
  margin-top: var(--space-4);
}

.type-tag {
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--surface-container);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.question-text {
  font-size: var(--text-body);
  color: var(--color-text-primary);
}

.answer-wrong {
  color: var(--color-error);
  font-weight: 500;
}

.answer-correct {
  color: var(--color-success);
  font-weight: 500;
}

.wrong-count {
  font-weight: 600;
  color: var(--color-error);
}

.source-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.mastered-badge {
  font-size: var(--text-small);
  color: var(--color-success);
  font-weight: 500;
  padding: 0 var(--space-2);
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

/* Detail */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.detail-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.detail-label {
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-value {
  font-size: var(--text-body);
  color: var(--color-text-primary);
  line-height: 1.6;
}

@media (max-width: 767px) {
  .table-footer {
    flex-direction: column;
    gap: var(--space-3);
    align-items: flex-start;
  }
}
</style>
