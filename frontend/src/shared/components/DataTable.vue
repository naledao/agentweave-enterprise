<template>
  <section class="data-table page-surface">
    <div v-if="$slots.toolbar" class="data-table__toolbar">
      <slot name="toolbar" />
    </div>

    <ErrorState
      v-if="errorMessage"
      compact
      :message="errorMessage"
      :trace-id="errorTraceId"
    >
      <template v-if="$slots.errorActions" #actions>
        <slot name="errorActions" />
      </template>
    </ErrorState>

    <el-table
      v-else
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      :border="border"
      :stripe="stripe"
      :height="height"
      :max-height="maxHeight"
      @sort-change="emit('sort-change', $event)"
      @row-click="emit('row-click', $event)"
    >
      <template #empty>
        <EmptyState compact :title="emptyTitle" :description="emptyDescription" />
      </template>
      <slot />
    </el-table>

    <div v-if="showPagination" class="data-table__pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :current-page="page + 1"
        :page-size="size"
        :page-sizes="pageSizes"
        :total="total"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>
  </section>
</template>

<script setup lang="ts" generic="T extends Record<string, unknown>">
import { computed } from 'vue'

import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'

const props = withDefaults(
  defineProps<{
    data: T[]
    loading?: boolean
    errorMessage?: string | null
    errorTraceId?: string | null
    rowKey?: string | ((row: T) => string)
    page?: number
    size?: number
    total?: number
    pageSizes?: number[]
    border?: boolean
    stripe?: boolean
    height?: string | number
    maxHeight?: string | number
    emptyTitle?: string
    emptyDescription?: string
  }>(),
  {
    loading: false,
    errorMessage: null,
    errorTraceId: null,
    rowKey: 'id',
    page: 0,
    size: 20,
    total: 0,
    pageSizes: () => [10, 20, 50, 100],
    border: false,
    stripe: false,
    height: undefined,
    maxHeight: undefined,
    emptyTitle: '暂无数据',
    emptyDescription: '',
  },
)

const emit = defineEmits<{
  'page-change': [page: number]
  'size-change': [size: number]
  'sort-change': [payload: unknown]
  'row-click': [row: T]
}>()

const showPagination = computed(() => props.total > 0)

function handleCurrentChange(page: number): void {
  emit('page-change', page - 1)
}

function handleSizeChange(size: number): void {
  emit('size-change', size)
}
</script>

<style scoped>
.data-table {
  overflow: hidden;
}

.data-table__toolbar {
  border-bottom: 1px solid #e6eaf0;
  padding: 14px 16px;
}

.data-table__pagination {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #e6eaf0;
  padding: 14px 16px;
}
</style>
