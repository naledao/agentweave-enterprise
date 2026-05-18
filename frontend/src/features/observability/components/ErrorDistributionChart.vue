<template>
  <section class="chart-panel">
    <div class="chart-panel__header">
      <h3>{{ title }}</h3>
      <span>{{ description }}</span>
    </div>
    <div v-if="loading" v-loading="loading" class="chart-placeholder" />
    <el-empty v-else-if="!items.length" :description="emptyText" />
    <div v-else ref="chartRef" class="chart-canvas" />
  </section>
</template>

<script setup lang="ts">
import * as echarts from 'echarts/core'
import { LegendComponent, TooltipComponent, type LegendComponentOption, type TooltipComponentOption } from 'echarts/components'
import { PieChart, type PieSeriesOption } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import type { ErrorDistributionItem } from '@/features/observability/types'

type ChartOption = echarts.ComposeOption<LegendComponentOption | TooltipComponentOption | PieSeriesOption>

echarts.use([LegendComponent, TooltipComponent, PieChart, CanvasRenderer])

const props = withDefaults(defineProps<{
  title: string
  description?: string
  items: ErrorDistributionItem[]
  loading?: boolean
  emptyText?: string
}>(), {
  description: '',
  loading: false,
  emptyText: '暂无错误分布数据',
})

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const option = computed<ChartOption>(() => ({
  color: props.items.map((item) => item.color ?? '#6c7b90'),
  tooltip: {
    trigger: 'item',
  },
  legend: {
    bottom: 0,
    left: 'center',
    textStyle: {
      color: '#69778d',
      fontSize: 11,
    },
  },
  series: [
    {
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      label: {
        formatter: '{b}: {c}',
        color: '#263143',
        fontSize: 11,
      },
      data: props.items.map((item) => ({
        name: item.label,
        value: item.value,
      })),
    },
  ],
}))

watch(
  () => [props.items, props.loading] as const,
  () => {
    void renderChart()
  },
  { deep: true },
)

onMounted(() => {
  void renderChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})

async function renderChart(): Promise<void> {
  await nextTick()
  if (!chartRef.value || props.loading || !props.items.length) {
    return
  }

  chart ??= echarts.init(chartRef.value)
  chart.setOption(option.value, true)
}

function resizeChart(): void {
  chart?.resize()
}
</script>

<style scoped>
.chart-panel {
  display: grid;
  min-width: 0;
  min-height: 260px;
  gap: 12px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.chart-panel__header {
  display: grid;
  gap: 4px;
}

.chart-panel__header h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.chart-panel__header span {
  color: #69778d;
  font-size: 12px;
}

.chart-canvas,
.chart-placeholder {
  min-height: 190px;
}
</style>
