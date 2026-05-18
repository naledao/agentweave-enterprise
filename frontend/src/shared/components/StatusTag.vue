<template>
  <el-tag :type="type" :effect="effect" :round="round">
    <span v-if="dot" class="status-tag__dot" aria-hidden="true" />
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
type StatusTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'
type StatusTagEffect = 'dark' | 'light' | 'plain'

withDefaults(
  defineProps<{
    label: string
    type?: StatusTagType
    effect?: StatusTagEffect
    round?: boolean
    dot?: boolean
  }>(),
  {
    type: 'info',
    effect: 'plain',
    round: false,
    dot: false,
  },
)
</script>

<style scoped>
.status-tag__dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 50%;
  background: currentColor;
  vertical-align: middle;
}
</style>
