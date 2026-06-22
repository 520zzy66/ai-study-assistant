<template>
  <div :class="['skeleton-container', `skeleton-${type}`]">
    <!-- Card skeleton -->
    <template v-if="type === 'card'">
      <div v-for="n in count" :key="n" class="sk-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="rect" style="width:100%;height:100px;border-radius:var(--radius-lg);" />
          </template>
        </el-skeleton>
      </div>
    </template>

    <!-- List skeleton -->
    <template v-else-if="type === 'list'">
      <div v-for="n in count" :key="n" class="sk-list-item">
        <el-skeleton-item variant="circle" style="width:36px;height:36px;" />
        <div style="flex:1;">
          <el-skeleton-item variant="text" style="width:60%;" />
          <el-skeleton-item variant="text" style="width:40%;" />
        </div>
      </div>
    </template>

    <!-- Table skeleton -->
    <template v-else-if="type === 'table'">
      <div v-for="n in count" :key="n" class="sk-table-row">
        <el-skeleton-item variant="rect" style="width:100%;height:44px;border-radius:var(--radius-sm);" />
      </div>
    </template>

    <!-- Text skeleton (paragraph) -->
    <template v-else-if="type === 'text'">
      <el-skeleton-item variant="text" style="width:100%;" />
      <el-skeleton-item variant="text" style="width:85%;" />
      <el-skeleton-item variant="text" style="width:65%;" />
    </template>

    <!-- Circle skeleton -->
    <template v-else-if="type === 'circle'">
      <el-skeleton-item variant="circle" style="width:48px;height:48px;" />
    </template>
  </div>
</template>

<script setup>
defineProps({
  type: { type: String, default: 'card', validator: v => ['card','list','table','text','circle'].includes(v) },
  count: { type: Number, default: 3 }
})
</script>

<style scoped>
.skeleton-container :deep(.el-skeleton__item) {
  background: var(--surface-container);
  border-radius: var(--radius-sm);
}

.skeleton-card {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.skeleton-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
