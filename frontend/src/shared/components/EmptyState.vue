<template>
  <section class="empty-state" :class="{ 'empty-state--compact': compact }">
    <el-icon class="empty-state__icon" aria-hidden="true">
      <component :is="iconComponent" />
    </el-icon>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.actions" class="empty-state__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'
import { computed, type Component } from 'vue'

const props = withDefaults(
  defineProps<{
    title?: string
    description?: string
    icon?: Component
    compact?: boolean
  }>(),
  {
    title: '暂无数据',
    description: '',
    icon: undefined,
    compact: false,
  },
)

const iconComponent = computed(() => props.icon ?? Box)
</script>

<style scoped>
.empty-state {
  display: grid;
  min-height: 220px;
  place-items: center;
  align-content: center;
  gap: 10px;
  padding: 32px 20px;
  color: #69778d;
  text-align: center;
}

.empty-state--compact {
  min-height: 120px;
  padding: 20px 16px;
}

.empty-state__icon {
  color: #8ea0b8;
  font-size: 34px;
}

h3 {
  margin: 0;
  color: #263143;
  font-size: 16px;
  font-weight: 700;
}

p {
  max-width: 420px;
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
}

.empty-state__actions {
  margin-top: 4px;
}
</style>
