<template>
  <div class="page-header">
    <div>
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <span v-if="description">{{ description }}</span>
    </div>
    <slot name="actions" />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
  eyebrow?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  flex: none;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

h2 {
  margin: 0;
  color: #182233;
  font-size: 22px;
  font-weight: 750;
}

p {
  margin: 0 0 4px;
  color: #69778d;
  font-size: 13px;
}

span {
  display: block;
  margin-top: 6px;
  color: #69778d;
  font-size: 14px;
}

@media (max-width: 720px) {
  .page-header {
    display: block;
  }
}
</style>
