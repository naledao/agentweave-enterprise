<template>
  <section class="loading-state" :class="{ 'loading-state--compact': compact }">
    <el-icon class="loading-state__icon" aria-hidden="true">
      <Loading />
    </el-icon>
    <span>{{ text }}</span>
  </section>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

withDefaults(
  defineProps<{
    text?: string
    compact?: boolean
  }>(),
  {
    text: '加载中',
    compact: false,
  },
)
</script>

<style scoped>
.loading-state {
  display: inline-flex;
  min-height: 160px;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #69778d;
  font-size: 14px;
}

.loading-state--compact {
  min-height: 72px;
}

.loading-state__icon {
  color: #2f72ff;
  font-size: 18px;
  animation: loading-rotate 1s linear infinite;
}

@keyframes loading-rotate {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
