<template>
  <div class="mindmap-workbench">
    <BasePageHeader
      title="导图工作台"
      description="沉浸式浏览、编辑和导出你的思维导图"
    >
      <template #actions>
        <div class="workbench-actions">
          <el-button @click="downloadPdf" :disabled="!mindMapData">
            <el-icon><Download /></el-icon> 导出 PDF
          </el-button>
          <el-button @click="downloadImage" :disabled="!mindMapData">
            <el-icon><Picture /></el-icon> 导出图片
          </el-button>
          <el-button type="primary" :loading="saving" :disabled="!mindMapData" @click="saveMindMap">
            <el-icon><Check /></el-icon> 保存更改
          </el-button>
        </div>
      </template>
    </BasePageHeader>

    <div class="workbench-content">
      <!-- 顶部筛选区域 -->
      <div class="filter-bar glass-effect">
        <div class="filter-group">
          <span class="filter-label">数据范围</span>
          <el-radio-group v-model="filterType" size="small" @change="handleTypeChange">
            <el-radio-button value="material">按学习资料</el-radio-button>
            <el-radio-button value="folder">按文件夹</el-radio-button>
          </el-radio-group>
        </div>
        
        <div class="filter-group">
          <span class="filter-label">选择目标</span>
          <!-- 资料选择 -->
          <el-select 
            v-if="filterType === 'material'" 
            v-model="selectedMaterialId" 
            placeholder="请选择学习资料" 
            style="width: 300px" 
            filterable 
            @change="loadMaterialMindMap"
          >
            <el-option 
              v-for="item in materialList" 
              :key="item.id" 
              :label="item.originalName" 
              :value="item.id" 
            />
          </el-select>
          
          <!-- 文件夹选择 -->
          <el-tree-select
            v-else
            v-model="selectedFolderId"
            :data="folderTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择文件夹"
            check-strictly
            style="width: 300px"
            @change="loadFolderMindMap"
          />
        </div>
      </div>

      <!-- 导图展示区 -->
      <div class="mindmap-canvas-container" v-loading="loading">
        <template v-if="mindMapData">
          <InteractiveMindMap 
            ref="mindMapRef" 
            :data="mindMapData" 
            @update="handleMindMapUpdate" 
          />
        </template>
        <template v-else-if="!loading">
          <AppEmpty 
            icon="Connection" 
            title="未加载导图" 
            description="请在上方选择包含导图数据的资料或文件夹，如果资料还未生成，请先进行AI总结" 
          />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Picture, Check, Connection } from '@element-plus/icons-vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import InteractiveMindMap from '@/components/common/InteractiveMindMap.vue'
import { getMaterialList } from '@/api/material'
import { getFolderTree } from '@/api/materialFolder'
import { getMindMap, generateMindMap, getFolderMindMap, generateFolderMindMap } from '@/api/ai'
import api from '@/api'

const route = useRoute()
const router = useRouter()

const filterType = ref('material')
const selectedMaterialId = ref(null)
const selectedFolderId = ref(null)

const materialList = ref([])
const folderTree = ref([])

const mindMapData = ref(null)
const mindMapRef = ref(null)
const loading = ref(false)
const saving = ref(false)

// 临时存储最新编辑的导图数据，防止修改被冲掉
const latestMindMapJson = ref(null)

onMounted(async () => {
  await fetchOptions()
  // 从 URL 参数初始化
  const queryMaterialId = route.query.materialId
  const queryFolderId = route.query.folderId
  
  if (queryMaterialId) {
    filterType.value = 'material'
    selectedMaterialId.value = parseInt(queryMaterialId)
    await loadMaterialMindMap(selectedMaterialId.value)
  } else if (queryFolderId) {
    filterType.value = 'folder'
    selectedFolderId.value = parseInt(queryFolderId)
    await loadFolderMindMap(selectedFolderId.value)
  }
})

// 加载下拉选项数据
async function fetchOptions() {
  try {
    const materialsRes = await getMaterialList({ size: 100, status: 'ready' })
    materialList.value = materialsRes.records || []
    const foldersRes = await getFolderTree()
    folderTree.value = foldersRes || []
  } catch (error) {
    console.error('加载选项数据失败', error)
  }
}

function handleTypeChange() {
  mindMapData.value = null
  latestMindMapJson.value = null
}

async function loadMaterialMindMap(materialId) {
  if (!materialId) return
  loading.value = true
  try {
    // API 返回格式: { code, message, data: { materialId, mindMap } }
    // axios 拦截器返回的 res 已经是 { materialId, mindMap }
    const resData = await getMindMap(materialId)
    let mindMapJsonStr = resData?.mindMap

    // 如果数据库还没有导图，自动触发生成
    if (!mindMapJsonStr) {
      ElMessage.info('该资料尚无导图，正在自动生成，请稍候...')
      try {
        const genRes = await generateMindMap(materialId)
        mindMapJsonStr = genRes?.mindMap
      } catch (genErr) {
        console.warn('自动生成导图失败:', genErr)
      }
    }

    if (mindMapJsonStr) {
      try {
        mindMapData.value = JSON.parse(mindMapJsonStr)
      } catch (e) {
        // 如果已经是对象，直接用
        mindMapData.value = mindMapJsonStr
      }
      latestMindMapJson.value = typeof mindMapJsonStr === 'string' ? mindMapJsonStr : JSON.stringify(mindMapJsonStr)
    } else {
      mindMapData.value = null
      ElMessage.warning('该资料暂无导图数据，请先在「AI总结」页面生成总结')
    }
  } catch (error) {
    mindMapData.value = null
    console.error('加载导图失败:', error)
    ElMessage.error('加载导图失败，请检查是否已完成AI总结')
  } finally {
    loading.value = false
  }
}

async function loadFolderMindMap(folderId) {
  if (!folderId) return
  loading.value = true
  try {
    const resData = await getFolderMindMap(folderId)
    let mindMapJsonStr = resData?.mindMap

    if (!mindMapJsonStr) {
      ElMessage.info('该文件夹尚无导图，正在自动生成，请稍候...')
      try {
        const genRes = await generateFolderMindMap(folderId)
        mindMapJsonStr = genRes?.mindMap
      } catch (genErr) {
        console.warn('自动生成文件夹导图失败:', genErr)
      }
    }

    if (mindMapJsonStr) {
      try {
        mindMapData.value = JSON.parse(mindMapJsonStr)
      } catch (e) {
        mindMapData.value = mindMapJsonStr
      }
      latestMindMapJson.value = typeof mindMapJsonStr === 'string' ? mindMapJsonStr : JSON.stringify(mindMapJsonStr)
    } else {
      mindMapData.value = null
      ElMessage.warning('该文件夹暂无导图数据，请先在「AI总结」页面生成总结')
    }
  } catch (error) {
    mindMapData.value = null
    console.error('加载文件夹导图失败:', error)
    ElMessage.error('加载导图失败，请检查是否已完成AI总结')
  } finally {
    loading.value = false
  }
}

// 接收 InteractiveMindMap 抛出的数据更新事件
function handleMindMapUpdate(newData) {
  latestMindMapJson.value = JSON.stringify(newData)
}

function downloadPdf() {
  if (mindMapRef.value && mindMapRef.value.exportPdf) {
    mindMapRef.value.exportPdf()
  } else {
    ElMessage.warning('导出功能正在初始化，请稍后重试')
  }
}

function downloadImage() {
  if (mindMapRef.value && mindMapRef.value.exportImage) {
    mindMapRef.value.exportImage()
  } else {
    ElMessage.warning('导出功能正在初始化，请稍后重试')
  }
}

async function saveMindMap() {
  if (!latestMindMapJson.value) return
  
  saving.value = true
  try {
    // 根据类型保存到不同的后端接口
    // 我们在这里使用一种优雅降级的方式，如果后端还没有相关API，这里会提示但是不崩溃。
    // 如果之后后端增加 PUT /material/{id}/mindmap 即可对接成功。
    if (filterType.value === 'material' && selectedMaterialId.value) {
      await api.put(`/material/${selectedMaterialId.value}/mindmap`, { mindMap: latestMindMapJson.value })
      ElMessage.success('资料导图保存成功')
    } else if (filterType.value === 'folder' && selectedFolderId.value) {
      await api.put(`/folder/${selectedFolderId.value}/mindmap`, { mindMap: latestMindMapJson.value })
      ElMessage.success('文件夹导图保存成功')
    }
  } catch (error) {
    ElMessage.warning('保存API尚未支持，请联系管理员升级后端。导图临时修改在前端生效。')
    console.warn('保存接口调用异常:', error)
  } finally {
    saving.value = false
  }
}

// 监听 URL 变化自动更新
watch(() => route.query, (newQuery) => {
  if (route.path !== '/ai/mindmap') return
  if (newQuery.materialId && parseInt(newQuery.materialId) !== selectedMaterialId.value) {
    filterType.value = 'material'
    selectedMaterialId.value = parseInt(newQuery.materialId)
    loadMaterialMindMap(selectedMaterialId.value)
  }
})
</script>

<style scoped>
.mindmap-workbench {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--header-height) - var(--space-16));
  min-height: 640px;
  overflow: hidden;
  background-color: transparent;
}

.workbench-actions {
  display: flex;
  gap: 12px;
}

.workbench-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  gap: var(--space-4);
  overflow: hidden;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding: var(--space-3) var(--space-4);
  background-color: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  z-index: 10;
}

.glass-effect {
  background: color-mix(in srgb, var(--surface-card) 78%, transparent);
  backdrop-filter: blur(12px);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.mindmap-canvas-container {
  flex: 1;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-xs);
  border: 1px solid var(--outline);
  background-color: var(--surface-card);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 767px) {
  .mindmap-workbench {
    height: auto;
    min-height: 720px;
  }

  .filter-bar {
    align-items: stretch;
    flex-direction: column;
    gap: var(--space-3);
  }

  .mindmap-canvas-container {
    min-height: 520px;
  }
}
</style>
