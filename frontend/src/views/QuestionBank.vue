<template>
  <div class="qb-page">
    <BasePageHeader title="我的题库" description="管理生成的练习题，查看、收藏和重新作答" />

    <!-- Toolbar -->
    <div class="qb-toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索批次名称..."
        clearable
        style="width: 280px"
        @clear="fetchBatches"
        @keydown.enter="fetchBatches"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <div class="qb-toolbar-right">
        <el-button :icon="Search" @click="fetchBatches">搜索</el-button>
        <el-button type="primary" :icon="Plus" @click="$router.push('/ai/quiz')">生成新题</el-button>
      </div>
    </div>

    <!-- Batch List -->
    <div v-loading="loading" class="qb-batch-list">
      <template v-if="batches.length > 0">
        <div v-for="batch in batches" :key="batch.batchId" class="batch-card">
          <div
            class="batch-header"
            role="button"
            tabindex="0"
            :aria-expanded="expandedBatch === batch.batchId"
            @click="toggleBatch(batch.batchId)"
            @keydown.enter.space.prevent="toggleBatch(batch.batchId)"
          >
            <div class="batch-info">
              <div class="batch-name">
                <el-icon :size="18"><Collection /></el-icon>
                <span>{{ batch.batchName }}</span>
              </div>
              <div class="batch-meta">
                <el-tag size="small" type="info">{{ batch.questionCount }} 题</el-tag>
                <el-tag size="small" :type="batch.difficulty === 'easy' ? 'success' : batch.difficulty === 'hard' ? 'danger' : 'warning'">
                  {{ { easy: '简单', medium: '中等', hard: '困难' }[batch.difficulty] || batch.difficulty }}
                </el-tag>
                <span class="batch-material">{{ batch.materialName }}</span>
              </div>
            </div>
            <div class="batch-actions" @click.stop>
              <el-button
                text
                size="small"
                :icon="Download"
                :loading="exportingBatchId === batch.batchId"
                @click="handleExportBatch(batch)"
              >导出 PDF</el-button>
              <el-button text size="small" :icon="Edit" @click="startRename(batch)">重命名</el-button>
              <el-button text size="small" type="danger" :icon="Delete" @click="handleDelete(batch)">删除</el-button>
              <el-icon :size="16" class="expand-icon" :class="{ expanded: expandedBatch === batch.batchId }">
                <ArrowDown />
              </el-icon>
            </div>
          </div>

          <!-- Expanded Questions -->
          <div v-if="expandedBatch === batch.batchId" class="batch-questions">
            <div v-for="q in expandedQuestions" :key="q.id" class="bank-question-item">
              <div class="bq-header">
                <el-tag size="small" :type="q.questionType === 'choice' ? '' : q.questionType === 'judge' ? 'success' : 'warning'">
                  {{ { choice: '单选', judge: '判断', short_answer: '简答' }[q.questionType] || q.questionType }}
                </el-tag>
                <el-tag size="small" :type="q.difficulty === 'easy' ? 'success' : q.difficulty === 'hard' ? 'danger' : 'warning'">
                  {{ { easy: '简单', medium: '中等', hard: '困难' }[q.difficulty] || q.difficulty }}
                </el-tag>
                <div class="bq-actions-right">
                  <el-button
                    text
                    size="small"
                    :type="q.isFavorite === 1 ? 'warning' : ''"
                    :icon="q.isFavorite === 1 ? StarFilled : Star"
                    @click="toggleFav(q)"
                  ></el-button>
                  <el-button text size="small" :icon="EditPen" @click="startReAnswer(q)">作答</el-button>
                </div>
              </div>
              <div class="bq-question">{{ q.question }}</div>

              <!-- Options -->
              <div v-if="q.questionType === 'choice' && q.options" class="bq-options">
                <div v-for="(val, key) in parseOptions(q.options)" :key="key" class="bq-option">
                  <span class="bq-opt-key">{{ key }}</span>
                  <span class="bq-opt-val">{{ val }}</span>
                </div>
              </div>

              <!-- Answer (expandable) -->
              <el-collapse class="bq-answer-collapse">
                <el-collapse-item title="查看答案与解析">
                  <div class="bq-answer"><strong>正确答案：</strong>{{ q.answer }}</div>
                  <div v-if="q.explanation" class="bq-explanation"><strong>解析：</strong>{{ q.explanation }}</div>
                </el-collapse-item>
              </el-collapse>

              <!-- Re-answer inline -->
              <div v-if="reAnsweringId === q.id" class="bq-reanswer">
                <el-input
                  v-if="q.questionType === 'short_answer'"
                  v-model="reAnswerText"
                  type="textarea"
                  :rows="2"
                  placeholder="输入你的答案..."
                />
                <div v-else class="bq-reanswer-options">
                  <button
                    v-for="(val, key) in parseOptions(q.options)"
                    :key="key"
                    class="bq-reanswer-opt"
                    type="button"
                    :class="{ selected: reAnswerText === key }"
                    @click="reAnswerText = key"
                  >{{ key }}. {{ val }}</button>
                </div>
                <div class="bq-reanswer-actions">
                  <el-button size="small" type="primary" :loading="reAnswering" @click="submitReAnswer(q)">提交</el-button>
                  <el-button size="small" @click="reAnsweringId = null">取消</el-button>
                </div>
                <div v-if="reAnswerResult" class="bq-reanswer-result" :class="{ correct: reAnswerResult.isCorrect }">
                  <el-icon :size="16"><component :is="reAnswerResult.isCorrect ? CircleCheck : CircleClose" /></el-icon>
                  <span>{{ reAnswerResult.isCorrect ? '回答正确！' : '回答错误' }}</span>
                  <span v-if="reAnswerResult.score < 1">得分：{{ Math.round(reAnswerResult.score * 100) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
      <AppEmpty v-else-if="!loading" icon="Collection" title="暂无题库" description="去 AI 出题生成练习题，题目会自动加入题库" />
    </div>

    <!-- Pagination -->
    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetchBatches"
      style="margin-top: 20px; justify-content: center"
    />

    <!-- Rename Dialog -->
    <el-dialog v-model="renameVisible" title="重命名批次" width="400px">
      <el-input v-model="renameText" placeholder="输入新名称" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Collection, Edit, Delete, ArrowDown, Star, StarFilled, EditPen, CircleCheck, CircleClose, Download } from '@element-plus/icons-vue'
import { listBatches, getBatchQuestions, exportBatchQuestionsPdf, renameBatch, deleteBatch, toggleFavorite, reAnswer } from '@/api/questionBank'
import AppEmpty from '@/components/common/AppEmpty.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const batches = ref([])
const exportingBatchId = ref('')

const expandedBatch = ref('')
const expandedQuestions = ref([])

// Rename
const renameVisible = ref(false)
const renameText = ref('')
const renameBatchId = ref('')

// Re-answer
const reAnsweringId = ref(null)
const reAnswerText = ref('')
const reAnswering = ref(false)
const reAnswerResult = ref(null)

function parseOptions(opts) {
  if (!opts) return {}
  if (typeof opts === 'string') {
    try { return JSON.parse(opts) } catch { return {} }
  }
  return opts
}

async function fetchBatches() {
  loading.value = true
  try {
    const data = await listBatches({ page: page.value, size: size.value, keyword: keyword.value })
    batches.value = data.records || []
    total.value = data.total || 0
  } catch { batches.value = [] }
  finally { loading.value = false }
}

async function toggleBatch(batchId) {
  if (expandedBatch.value === batchId) {
    expandedBatch.value = ''
    expandedQuestions.value = []
    return
  }
  expandedBatch.value = batchId
  try {
    expandedQuestions.value = await getBatchQuestions(batchId)
  } catch { expandedQuestions.value = [] }
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function safeFileName(name) {
  return String(name || '题库试卷')
    .replace(/[\\/:*?"<>|]/g, '_')
    .slice(0, 60)
}

async function handleExportBatch(batch) {
  exportingBatchId.value = batch.batchId
  try {
    const blob = await exportBatchQuestionsPdf(batch.batchId)
    const date = new Date().toISOString().split('T')[0]
    downloadBlob(blob, `题库-${safeFileName(batch.batchName || batch.batchId)}-${date}.pdf`)
    ElMessage.success('PDF 导出成功')
  } catch (error) {
    if (!error?.message) ElMessage.error('PDF 导出失败')
  } finally {
    exportingBatchId.value = ''
  }
}

function startRename(batch) {
  renameBatchId.value = batch.batchId
  renameText.value = batch.batchName
  renameVisible.value = true
}

async function confirmRename() {
  if (!renameText.value.trim()) return
  await renameBatch(renameBatchId.value, renameText.value.trim())
  ElMessage.success('已重命名')
  renameVisible.value = false
  fetchBatches()
}

async function handleDelete(batch) {
  try {
    await ElMessageBox.confirm(`确定删除批次"${batch.batchName}"吗？此操作不可恢复。`, '确认删除', { type: 'warning' })
  } catch { return }
  await deleteBatch(batch.batchId)
  ElMessage.success('已删除')
  if (expandedBatch.value === batch.batchId) { expandedBatch.value = ''; expandedQuestions.value = [] }
  fetchBatches()
}

async function toggleFav(q) {
  const res = await toggleFavorite(q.id)
  q.isFavorite = res.isFavorite ? 1 : 0
  ElMessage.success(res.isFavorite ? '已收藏' : '已取消收藏')
}

function startReAnswer(q) {
  reAnsweringId.value = q.id
  reAnswerText.value = ''
  reAnswerResult.value = null
}

async function submitReAnswer(q) {
  if (!reAnswerText.value.trim()) { ElMessage.warning('请输入答案'); return }
  reAnswering.value = true
  try {
    reAnswerResult.value = await reAnswer(q.id, reAnswerText.value.trim())
    ElMessage.success(reAnswerResult.value.isCorrect ? '回答正确！' : '回答错误，已加入错题本')
  } catch { ElMessage.error('提交失败') }
  finally { reAnswering.value = false }
}

onMounted(() => fetchBatches())
</script>

<style scoped>
.qb-page { width: 100%; }

.qb-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  gap: 12px;
}
.qb-toolbar-right { display: flex; gap: 8px; }

.batch-card {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-3);
  overflow: hidden;
}
.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  cursor: pointer;
  transition: background-color var(--duration-fast);
}
.batch-header:hover { background: var(--surface-hover); }
.batch-header:focus-visible { outline-offset: -3px; }
.batch-info { display: flex; flex-direction: column; gap: 8px; }
.batch-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-body);
  font-weight: 600;
  color: var(--color-text-primary);
}
.batch-meta { display: flex; align-items: center; gap: 8px; }
.batch-material { font-size: var(--text-small); color: var(--color-text-tertiary); }
.batch-actions { display: flex; align-items: center; gap: 4px; }
.expand-icon { transition: transform var(--duration-fast); }
.expand-icon.expanded { transform: rotate(180deg); }

.batch-questions {
  border-top: 1px solid var(--outline-variant);
  padding: 16px 20px;
}
.bank-question-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--outline-variant);
}
.bank-question-item:last-child { border-bottom: none; }
.bq-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.bq-actions-right { margin-left: auto; display: flex; gap: 4px; }
.bq-question {
  font-size: var(--text-body);
  color: var(--color-text-primary);
  line-height: 1.6;
  margin-bottom: 10px;
}
.bq-options { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.bq-option { display: flex; gap: 8px; font-size: var(--text-small); color: var(--color-text-secondary); }
.bq-opt-key { font-weight: 600; color: var(--color-primary); min-width: 20px; }
.bq-answer-collapse { margin-top: 8px; }
.bq-answer, .bq-explanation { font-size: var(--text-small); color: var(--color-text-secondary); margin-top: 4px; }
.bq-reanswer { margin-top: 12px; padding: 12px; background: var(--color-primary-light); border-radius: var(--radius-md); }
.bq-reanswer-options { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.bq-reanswer-opt {
  padding: 8px 12px;
  border: 1px solid var(--outline);
  border-radius: var(--radius-sm);
  background: var(--surface-card);
  text-align: left;
  cursor: pointer;
  font-size: var(--text-small);
  transition: all var(--duration-fast);
}
.bq-reanswer-opt:hover { border-color: var(--color-primary); }
.bq-reanswer-opt.selected { border-color: var(--color-primary); background: var(--color-primary-light); }
.bq-reanswer-actions { display: flex; gap: 8px; margin-top: 8px; }
.bq-reanswer-result {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--text-small);
  font-weight: 500;
}
.bq-reanswer-result.correct { background: var(--color-success-bg); color: var(--color-success); }
.bq-reanswer-result:not(.correct) { background: var(--color-error-bg); color: var(--color-error); }
</style>
