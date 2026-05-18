<template>
  <section class="chart-panel">
    <div class="chart-panel__header">
      <h3>{{ title }}</h3>
      <span>{{ description }}</span>
    </div>
    <div v-if="loading" v-loading="loading" class="chart-placeholder" />
    <el-empty v-else-if="!points.length" :description="emptyText" />
    <div v-else ref="chartRef" class="chart-canvas" />
  </section>
</template>

<script setup lang="ts">
import * as echarts from 'echarts/core'
import { GridComponent, TooltipComponent, type GridComponentOption, type TooltipComponentOption } from 'echarts/components'
import { LineChart, type LineSeriesOption } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import type { MetricTrendPoint } from '@/features/observability/types'

type ChartOption = echarts.ComposeOption<GridComponentOption | TooltipComponentOption | LineSeriesOption>

echarts.use([GridComponent, TooltipComponent, LineChart, CanvasRenderer])

const props = withDefaults(defineProps<{
  title: string
  description?: string
  points: MetricTrendPoint[]
  loading?: boolean
  emptyText?: string
}>(), {
  description: '',
  loading: false,
  emptyText: '暂无趋势数据',
})

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const option = computed<ChartOption>(() => ({
  color: ['#2f6fed'],
  grid: {
    left: 36,
    right: 16,
    top: 24,
    bottom: 28,
  },
  tooltip: {
    trigger: 'axis',
    valueFormatter: (value) => `${value} ms`,
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: props.points.map((point) => point.label),
    axisLabel: {
      color: '#69778d',
      fontSize: 11,
    },
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      color: '#69778d',
      fontSize: 11,
    },
    splitLine: {
      lineStyle: {
        color: '#edf1f5',
      },
    },
  },
  series: [
    {
      type: 'line',
      smooth: true,
      symbolSize: 7,
      lineStyle: {
        width: 3,
      },
      areaStyle: {
        color: 'rgba(47, 111, 237, 0.12)',
      },
      data: props.points.map((point) => point.value),
    },
  ],
}))

watch(
  () => [props.points, props.loading] as const,
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
  if (!chartRef.value || props.loading || !props.points.length) {
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
