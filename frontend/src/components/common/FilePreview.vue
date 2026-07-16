<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="80%"
    top="5vh"
    :destroy-on-close="true"
  >
    <div class="preview-container" v-loading="loading">
      <!-- TXT/PDF 文本内容展示 -->
      <div v-if="supportText" class="text-container">
        <pre class="text-content">{{ textContent }}</pre>
      </div>

      <!-- DOCX 使用 docx-preview 渲染 -->
      <div v-else-if="isDocx" class="docx-container" ref="docxContainer"></div>

      <!-- PDF 使用 iframe（通过 blob URL 不泄露 token） -->
      <iframe
        v-else-if="isPdf && blobUrl"
        :src="blobUrl"
        class="preview-iframe"
        frameborder="0"
      ></iframe>

      <!-- 不支持的文件类型 -->
      <div v-else-if="!loading" class="unsupported-container">
        <el-empty description="该文件格式不支持在线预览">
          <el-button type="primary" @click="handleDownload">下载文件</el-button>
        </el-empty>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick, watch, onBeforeUnmount } from 'vue'
import api from '@/api'
import { renderAsync } from 'docx-preview'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  fileId: {
    type: [Number, String],
    required: true
  },
  fileName: {
    type: String,
    default: '文件预览'
  },
  fileType: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const title = computed(() => `预览：${props.fileName}`)
const loading = ref(false)
const docxContainer = ref(null)
const textContent = ref('')
const blobUrl = ref('')

const supportText = computed(() => {
  const type = (props.fileType || '').toLowerCase()
  return type === 'txt' || type === 'md'
})

const isPdf = computed(() => {
  return (props.fileType || '').toLowerCase() === 'pdf'
})

const isDocx = computed(() => {
  return (props.fileType || '').toLowerCase() === 'docx'
})

/** 释放 Blob URL 防止内存泄漏 */
function revokeBlobUrl() {
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
    blobUrl.value = ''
  }
}

/** 统一获取文件 Blob（携带认证） */
async function fetchFileBlob() {
  const response = await api.get(`/material/${props.fileId}/preview`, {
    responseType: 'blob'
  })
  return response
}

/** 加载 TXT/MD 文本内容 */
async function loadText() {
  loading.value = true
  textContent.value = ''
  try {
    const blob = await fetchFileBlob()
    textContent.value = await blob.text()
  } catch (error) {
    console.error('文本预览失败', error)
    ElMessage.error('无法加载文件内容，请检查登录状态')
  } finally {
    loading.value = false
  }
}

/** 加载 PDF（转为 blob URL） */
async function loadPdf() {
  loading.value = true
  revokeBlobUrl()
  try {
    const blob = await fetchFileBlob()
    blobUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('PDF 预览失败', error)
    ElMessage.error('无法加载 PDF，请检查登录状态')
  } finally {
    loading.value = false
  }
}

/** 加载 DOCX */
async function renderDocx() {
  loading.value = true
  try {
    const blob = await fetchFileBlob()
    await nextTick()
    if (docxContainer.value) {
      await renderAsync(blob, docxContainer.value, null, {
        className: 'docx-preview-content',
        inWrapper: false,
        breakPages: true,
        trimXmlDeclaration: true,
      })
    }
  } catch (error) {
    console.error('DOCX 预览失败', error)
    ElMessage.error('无法加载该 DOCX 文档')
  } finally {
    loading.value = false
  }
}

watch(() => visible.value, async (newVal) => {
  if (newVal) {
    if (supportText.value) {
      await loadText()
    } else if (isPdf.value) {
      await loadPdf()
    } else if (isDocx.value) {
      await renderDocx()
    }
  } else {
    // 关闭时清理
    loading.value = false
    textContent.value = ''
    revokeBlobUrl()
    if (docxContainer.value) {
      docxContainer.value.innerHTML = ''
    }
  }
})

onBeforeUnmount(() => {
  revokeBlobUrl()
})

const handleDownload = async () => {
  try {
    const blob = await fetchFileBlob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = props.fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}
</script>

<style scoped>
.preview-container {
  width: 100%;
  height: 70vh;
  min-height: 500px;
  background-color: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.text-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background: #fff;
  padding: 24px 32px;
  box-sizing: border-box;
}

.text-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.7;
  color: #2d3748;
  margin: 0;
}

.docx-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background-color: #fff;
  padding: 20px;
}

.unsupported-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}

:deep(.docx-wrapper) {
  background: transparent !important;
  padding: 0 !important;
}
:deep(.docx-wrapper > section.docx) {
  margin-bottom: 0 !important;
  box-shadow: none !important;
}
</style>
