<template>
  <div class="interactive-mindmap-container">
    <div class="mindmap-wrapper" ref="mindmapRef"></div>

    <!-- 极简 Apple Slate 风格悬浮控制栏 -->
    <div class="mindmap-toolbar">
      <el-tooltip content="放大" placement="top" :show-after="300">
        <button class="toolbar-btn" @click="zoomIn">
          <el-icon><ZoomIn /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip content="缩小" placement="top" :show-after="300">
        <button class="toolbar-btn" @click="zoomOut">
          <el-icon><ZoomOut /></el-icon>
        </button>
      </el-tooltip>
      <div class="toolbar-divider"></div>
      <el-tooltip content="定位居中" placement="top" :show-after="300">
        <button class="toolbar-btn" @click="fitView">
          <el-icon><Aim /></el-icon>
        </button>
      </el-tooltip>
      <div class="toolbar-divider"></div>
      <el-tooltip content="导出为图片" placement="top" :show-after="300">
        <button class="toolbar-btn" @click="exportImage">
          <el-icon><Download /></el-icon>
        </button>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ZoomIn, ZoomOut, Aim, Download } from '@element-plus/icons-vue'
import MindMap from 'simple-mind-map'
import DragPlugin from 'simple-mind-map/src/plugins/Drag.js'
import KeyboardNavigationPlugin from 'simple-mind-map/src/plugins/KeyboardNavigation.js'
import ExportPlugin from 'simple-mind-map/src/plugins/Export.js'
import SelectPlugin from 'simple-mind-map/src/plugins/Select.js'
import RichTextPlugin from 'simple-mind-map/src/plugins/RichText.js'

// 按需注册插件
MindMap.usePlugin(DragPlugin)
  .usePlugin(KeyboardNavigationPlugin)
  .usePlugin(ExportPlugin)
  .usePlugin(SelectPlugin)
  .usePlugin(RichTextPlugin)

const props = defineProps({
  data: {
    type: Object,
    required: true,
    default: () => ({ name: '中心主题', children: [] })
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update'])

const mindmapRef = ref(null)
let mindMapInstance = null
let isInternalChange = false // 用于切断数据更新循环的死锁标志

// QwenTools JSON 到 simple-mind-map 的格式映射，增强健壮性防空节点
const transformData = (node) => {
  if (!node) return null;
  // 防止已经是符合 simple-mind-map 标准的格式被再次错误转化
  if (node.data && typeof node.data.text !== 'undefined') {
    return {
      data: { ...node.data },
      children: Array.isArray(node.children) ? node.children.map(transformData) : []
    };
  }
  
  const textContent = node.name || node.text || node.title || node.content || node.topic || '新节点';
  return {
    data: { text: String(textContent) }, // 强制转字符串，并保留文本
    children: Array.isArray(node.children) ? node.children.map(transformData) : []
  };
}

const initMindMap = () => {
  if (!mindmapRef.value) return;
  
  if (mindMapInstance) {
    mindMapInstance.destroy();
  }

  // 深度解绑 Vue 的 Proxy 响应式对象，防止 simple-mind-map 内部测量和变更操作抛错/无法取值
  const initialData = JSON.parse(JSON.stringify(transformData(props.data) || { data: { text: '中心主题' }, children: [] }));

  mindMapInstance = new MindMap({
    el: mindmapRef.value,
    data: initialData,
    theme: 'classic',
    layout: 'logicalStructure',
    readonly: props.readonly, // 支持只读模式
    fit: true
  });

  // 监听数据改变事件反馈给上层组件
  mindMapInstance.on('data_change', (data) => {
    // 标记这是内部改变，避免外层 watch 接收到再次 setData
    isInternalChange = true;
    emit('update', data);
  });
}

onMounted(() => {
  nextTick(() => {
    initMindMap()
  })
})

onUnmounted(() => {
  if (mindMapInstance) {
    mindMapInstance.destroy()
  }
})

// 监听外部数据刷新 (例如 AI 返回新内容)
watch(() => props.data, (newVal) => {
  if (isInternalChange) {
    isInternalChange = false;
    return; // 如果是内部操作导致的数据回流，直接阻断，防止闪烁
  }
  if (mindMapInstance && newVal) {
    const safeData = JSON.parse(JSON.stringify(transformData(newVal) || { data: { text: '中心主题' }, children: [] }));
    mindMapInstance.setData(safeData)
    mindMapInstance.fit()
  }
}, { deep: true })

// 工具栏动作
const zoomIn = () => {
  if (mindMapInstance) {
    mindMapInstance.view.enlarge()
  }
}

const zoomOut = () => {
  if (mindMapInstance) {
    mindMapInstance.view.narrow()
  }
}

const fitView = () => {
  if (mindMapInstance) {
    mindMapInstance.view.fit()
  }
}

const exportImage = async () => {
  if (mindMapInstance) {
    try {
      await mindMapInstance.export('png', true, 'AI-Study-MindMap')
    } catch (e) {
      console.error('Export failed:', e)
    }
  }
}

const exportPdf = async () => {
  if (mindMapInstance) {
    try {
      await mindMapInstance.export('pdf', true, 'AI-Study-MindMap')
    } catch (e) {
      console.error('Export failed:', e)
    }
  }
}

// 将方法暴露给父组件（工作台）使用
defineExpose({
  exportImage,
  exportPdf
})
</script>

<style scoped>
.interactive-mindmap-container {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 500px;
  background-color: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--outline);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.mindmap-wrapper {
  flex: 1;
  width: 100%;
  height: 100%;
  outline: none;
}

/* 覆盖 simple-mind-map 内部的输入框，使其匹配系统风格 */
:deep(.smm-node-input) {
  border: 1px solid var(--color-primary) !important;
  border-radius: 4px;
  box-shadow: var(--shadow-sm);
  color: var(--color-text-primary);
  font-family: var(--font-sans);
}

/* 保护 simple-mind-map 节点字体大小不塌陷 */
:deep(.smm-node) {
  font-size: 14px;
}

/* Apple Slate 风格悬浮控制栏 */
.mindmap-toolbar {
  position: absolute;
  bottom: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid var(--outline);
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-md);
  z-index: 10;
}

.dark .mindmap-toolbar {
  background: rgba(30, 30, 30, 0.85);
}

.toolbar-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.toolbar-btn:hover {
  background: var(--surface-hover);
  color: var(--color-text-primary);
  transform: scale(1.05);
}

.toolbar-btn:active {
  transform: scale(0.95);
}

.toolbar-divider {
  width: 1px;
  height: 16px;
  background: var(--outline-variant);
  margin: 0 4px;
}
</style>
