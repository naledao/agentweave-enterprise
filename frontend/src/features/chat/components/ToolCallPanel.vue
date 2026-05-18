<template>
  <section class="panel-section">
    <h3>工具调用</h3>
    <div v-if="invocations.length === 0" class="panel-empty">暂无工具调用</div>
    <ToolInvocationCard
      v-for="invocation in invocations"
      v-else
      :key="invocation.toolCallId"
      :invocation="invocation"
    />
  </section>
</template>

<script setup lang="ts">
import type { ToolInvocation } from '@/features/chat/types'
import ToolInvocationCard from '@/features/tools/components/ToolInvocationCard.vue'

defineProps<{
  invocations: ToolInvocation[]
}>()
</script>

<style scoped>
.panel-section {
  display: grid;
  gap: 10px;
}

.panel-section h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.panel-empty {
  color: #69778d;
  font-size: 13px;
}

</style>
