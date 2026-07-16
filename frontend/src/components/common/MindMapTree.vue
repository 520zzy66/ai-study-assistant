<template>
  <div class="mindmap-tree" ref="containerRef">
    <svg :width="svgWidth" :height="svgHeight" class="mindmap-svg">
      <!-- 连接线 -->
      <path
        v-for="(link, i) in links"
        :key="'link-' + i"
        :d="link.path"
        fill="none"
        stroke="var(--el-color-primary-light-3)"
        stroke-width="1.5"
        stroke-opacity="0.6"
      />
      <!-- 节点 -->
      <g
        v-for="(node, i) in nodes"
        :key="'node-' + i"
        :transform="`translate(${node.x}, ${node.y})`"
        class="mindmap-node"
      >
        <rect
          :x="-node.w / 2"
          :y="-node.h / 2"
          :width="node.w"
          :height="node.h"
          :rx="node.depth === 0 ? 20 : 8"
          :fill="node.depth === 0 ? 'var(--color-primary)' : nodeBg(node.depth)"
          :stroke="node.depth === 0 ? 'none' : 'var(--el-color-primary-light-3)'"
          stroke-width="1"
        />
        <text
          text-anchor="middle"
          dominant-baseline="central"
          :fill="node.depth === 0 ? '#fff' : 'var(--color-text-primary)'"
          :font-size="node.depth === 0 ? 14 : node.depth === 1 ? 13 : 12"
          :font-weight="node.depth === 0 ? 600 : node.depth === 1 ? 500 : 400"
          class="node-text"
        >{{ node.name }}</text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const containerRef = ref(null)

// 布局参数
const NODE_H = 32
const ROOT_W = 140
const BRANCH_W = 110
const LEAF_W = 90
const H_GAP = 60
const V_GAP = 12
const PADDING = 20

/**
 * 递归计算子树高度
 */
function subtreeHeight(node) {
  if (!node.children || node.children.length === 0) return NODE_H
  return node.children.reduce((sum, child) => sum + subtreeHeight(child) + V_GAP, -V_GAP)
}

/**
 * 递归布局所有节点
 */
function layoutTree(node, depth, x, yStart, yEnd) {
  const w = depth === 0 ? ROOT_W : depth === 1 ? BRANCH_W : LEAF_W
  const h = NODE_H
  const y = (yStart + yEnd) / 2

  const result = [{ name: node.name, x, y, w, h, depth }]
  const linkResults = []

  if (node.children && node.children.length > 0) {
    const childX = x + w / 2 + H_GAP + (depth === 0 ? BRANCH_W : LEAF_W) / 2
    const totalH = node.children.reduce((sum, child) => sum + subtreeHeight(child) + V_GAP, -V_GAP)
    let currentY = y - totalH / 2

    for (const child of node.children) {
      const childH = subtreeHeight(child)
      const childYStart = currentY
      const childYEnd = currentY + childH
      const childResult = layoutTree(child, depth + 1, childX, childYStart, childYEnd)

      result.push(...childResult.nodes)
      linkResults.push(...childResult.links)

      // 连接线：从父节点右侧到子节点左侧
      const parentRight = x + w / 2
      const childLeft = childX - (depth === 0 ? BRANCH_W : LEAF_W) / 2
      const midX = (parentRight + childLeft) / 2
      linkResults.push({
        path: `M ${parentRight} ${y} C ${midX} ${y}, ${midX} ${childResult.nodes[0].y}, ${childLeft} ${childResult.nodes[0].y}`
      })

      currentY = childYEnd + V_GAP
    }
  }

  return { nodes: result, links: linkResults }
}

const layout = computed(() => {
  if (!props.data || !props.data.name) return { nodes: [], links: [] }

  const totalH = subtreeHeight(props.data)
  const startY = PADDING + totalH / 2

  return layoutTree(props.data, 0, PADDING + ROOT_W / 2, startY - totalH / 2, startY + totalH / 2)
})

const nodes = computed(() => layout.value.nodes)
const links = computed(() => layout.value.links)

const svgWidth = computed(() => {
  if (nodes.value.length === 0) return 300
  const maxX = Math.max(...nodes.value.map(n => n.x + n.w / 2))
  return maxX + PADDING * 2
})

const svgHeight = computed(() => {
  if (nodes.value.length === 0) return 200
  const maxY = Math.max(...nodes.value.map(n => n.y + n.h / 2))
  return maxY + PADDING * 2
})

function nodeBg(depth) {
  const colors = [
    'var(--el-color-primary-light-9)',
    'var(--el-color-primary-light-8)',
    'var(--el-color-primary-light-7)'
  ]
  return colors[Math.min(depth - 1, colors.length - 1)]
}
</script>

<style scoped>
.mindmap-tree {
  overflow-x: auto;
  overflow-y: hidden;
  padding: var(--space-2);
}

.mindmap-svg {
  display: block;
  min-width: 100%;
}

.node-text {
  pointer-events: none;
  user-select: none;
}

.mindmap-node {
  cursor: default;
}
</style>
