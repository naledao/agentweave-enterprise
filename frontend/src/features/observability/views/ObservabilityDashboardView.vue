<template>
  <section class="observability-dashboard-view">
    <PageHeader
      title="监控观测"
      description="集中查看模型调用、RAG、GraphRAG、工具调用、工作流、SSE、错误分布和审计日志摘要。"
      eyebrow="Observability"
    >
      <template #actions>
        <el-button :loading="refreshing" @click="refreshAll">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="summaryError.message"
      class="page-error"
      :title="summaryError.message"
      type="error"
      :closable="false"
      show-icon
    >
      <TraceIdText v-if="summaryError.traceId" :trace-id="summaryError.traceId" />
    </el-alert>

    <div class="page-toolbar">
      <div class="toolbar-filters">
        <el-input
          v-model="filterForm.traceId"
          clearable
          placeholder="traceId"
          style="width: 240px"
          @keyup.enter="searchLogs"
          @clear="searchLogs"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-date-picker
          v-model="filterForm.createdRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 360px"
          @change="searchLogs"
        />

        <el-input
          v-model="filterForm.modelName"
          clearable
          placeholder="模型名称"
          style="width: 170px"
          @keyup.enter="searchLogs"
          @clear="searchLogs"
        />

        <el-select
          v-model="filterForm.modelStatus"
          clearable
          placeholder="模型状态"
          style="width: 130px"
          @change="searchLogs"
          @clear="searchLogs"
        >
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="超时" value="TIMEOUT" />
          <el-option label="取消" value="CANCELLED" />
        </el-select>

        <el-select
          v-model="filterForm.auditResult"
          clearable
          placeholder="审计结果"
          style="width: 130px"
          @change="searchLogs"
          @clear="searchLogs"
        >
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="拒绝" value="DENIED" />
        </el-select>

        <template v-if="activeTab === 'index'">
          <el-select
            v-model="filterForm.indexStatus"
            clearable
            placeholder="构建状态"
            style="width: 130px"
            @change="searchLogs"
            @clear="searchLogs"
          >
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已构建" value="INDEXED" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已跳过" value="SKIPPED" />
          </el-select>
        </template>

        <template v-if="activeTab === 'rag'">
          <el-input
            v-model="filterForm.businessDomain"
            clearable
            placeholder="业务域"
            style="width: 130px"
            @keyup.enter="searchLogs"
            @clear="searchLogs"
          />

          <el-select
            v-model="filterForm.ragStatus"
            clearable
            placeholder="RAG 状态"
            style="width: 130px"
            @change="searchLogs"
            @clear="searchLogs"
          >
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="降级" value="DEGRADED" />
          </el-select>

          <el-select
            v-model="filterForm.ragRetrievalMode"
            clearable
            placeholder="RAG 模式"
            style="width: 130px"
            @change="searchLogs"
            @clear="searchLogs"
          >
            <el-option label="VECTOR_ONLY" value="VECTOR_ONLY" />
            <el-option label="HYBRID" value="HYBRID" />
            <el-option label="GRAPH_ONLY" value="GRAPH_ONLY" />
          </el-select>
        </template>

        <template v-if="activeTab === 'retrieval'">
          <el-input
            v-model="filterForm.workflowRunId"
            clearable
            placeholder="workflowRunId"
            style="width: 220px"
            @keyup.enter="searchLogs"
            @clear="searchLogs"
          />

          <el-input
            v-model="filterForm.workflowStepId"
            clearable
            placeholder="workflowStepId"
            style="width: 220px"
            @keyup.enter="searchLogs"
            @clear="searchLogs"
          />

          <el-input
            v-model="filterForm.businessDomain"
            clearable
            placeholder="业务域"
            style="width: 130px"
            @keyup.enter="searchLogs"
            @clear="searchLogs"
          />

          <el-select
            v-model="filterForm.retrievalStatus"
            clearable
            placeholder="检索状态"
            style="width: 130px"
            @change="searchLogs"
            @clear="searchLogs"
          >
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="降级" value="DEGRADED" />
          </el-select>

          <el-select
            v-model="filterForm.retrievalMode"
            clearable
            placeholder="检索模式"
            style="width: 130px"
            @change="searchLogs"
            @clear="searchLogs"
          >
            <el-option label="HYBRID" value="HYBRID" />
            <el-option label="GRAPH_ONLY" value="GRAPH_ONLY" />
          </el-select>
        </template>

        <el-button @click="searchLogs">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </div>

    <section class="metric-grid" v-loading="summaryQuery.isFetching.value">
      <MetricCard
        v-for="metric in metricCards"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :hint="metric.hint"
        :tone="metric.tone"
        :tag="metric.tag"
      />
    </section>

    <section class="chart-grid">
      <MetricTrendChart
        title="模型调用耗时趋势"
        description="基于当前筛选下最近模型调用记录"
        :points="modelDurationTrend"
        :loading="modelCallsQuery.isFetching.value"
      />
      <ErrorDistributionChart
        title="错误分布"
        description="汇总模型、RAG、工具、工作流和 SSE 异常"
        :items="errorDistribution"
        :loading="summaryQuery.isFetching.value"
      />
    </section>

    <GraphRagSummaryPanel :summary="graphRagSummary" />

    <section class="detail-metric-grid">
      <ToolInvocationMetricPanel :summary="summary?.toolSummary ?? null" />
      <WorkflowMetricPanel :summary="summary?.workflowSummary ?? null" />
      <SseMetricPanel :summary="summary?.sseSummary ?? null" />
      <GraphRagMetricPanel :summary="graphRagSummary" />
    </section>

    <el-alert
      v-if="activeListError.message"
      class="page-error"
      :title="activeListError.message"
      type="error"
      :closable="false"
      show-icon
    >
      <TraceIdText v-if="activeListError.traceId" :trace-id="activeListError.traceId" />
    </el-alert>

    <div class="page-surface log-surface">
      <el-tabs v-model="activeTab" class="log-tabs">
        <el-tab-pane label="模型调用" name="model">
          <ModelCallTable
            class="log-table"
            :calls="modelCalls"
            :loading="modelCallsQuery.isFetching.value"
          />

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="modelPage"
              v-model:page-size="modelSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="modelTotal"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="审计日志" name="audit">
          <AuditLogTable
            class="log-table"
            :logs="auditLogs"
            :loading="auditLogsQuery.isFetching.value"
          />

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="auditPage"
              v-model:page-size="auditSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="auditTotal"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="RAG 检索日志" name="rag">
          <RagRetrievalLogTable
            class="log-table"
            :logs="ragLogs"
            :loading="ragLogsQuery.isFetching.value"
          />

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="ragPage"
              v-model:page-size="ragSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="ragTotal"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="图谱构建日志" name="index">
          <GraphRagIndexLogTable
            class="log-table"
            :logs="indexLogs"
            :loading="indexLogsQuery.isFetching.value"
          />

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="indexPage"
              v-model:page-size="indexSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="indexTotal"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="路径检索日志" name="retrieval">
          <GraphRagRetrievalLogTable
            class="log-table"
            :logs="retrievalLogs"
            :loading="retrievalLogsQuery.isFetching.value"
          />

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="retrievalPage"
              v-model:page-size="retrievalSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="retrievalTotal"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue'
import { useQuery } from '@tanstack/vue-query'
import { computed, reactive, ref, watch } from 'vue'

import { observabilityApi } from '@/features/observability/api/observabilityApi'
import AuditLogTable from '@/features/observability/components/AuditLogTable.vue'
import ErrorDistributionChart from '@/features/observability/components/ErrorDistributionChart.vue'
import GraphRagIndexLogTable from '@/features/observability/components/GraphRagIndexLogTable.vue'
import GraphRagMetricPanel from '@/features/observability/components/GraphRagMetricPanel.vue'
import GraphRagRetrievalLogTable from '@/features/observability/components/GraphRagRetrievalLogTable.vue'
import GraphRagSummaryPanel from '@/features/observability/components/GraphRagSummaryPanel.vue'
import MetricCard from '@/features/observability/components/MetricCard.vue'
import MetricTrendChart from '@/features/observability/components/MetricTrendChart.vue'
import ModelCallTable from '@/features/observability/components/ModelCallTable.vue'
import RagRetrievalLogTable from '@/features/observability/components/RagRetrievalLogTable.vue'
import SseMetricPanel from '@/features/observability/components/SseMetricPanel.vue'
import ToolInvocationMetricPanel from '@/features/observability/components/ToolInvocationMetricPanel.vue'
import WorkflowMetricPanel from '@/features/observability/components/WorkflowMetricPanel.vue'
import type {
  AuditResult,
  ErrorDistributionItem,
  GraphRagIndexStatus,
  GraphRagRetrievalStatus,
  MetricTrendPoint,
  ModelCallStatus,
  RagRetrievalStatus,
} from '@/features/observability/types'
import PageHeader from '@/shared/components/PageHeader.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'
import { getApiErrorDisplay } from '@/shared/utils/apiError'

type DateRange = [Date, Date] | null
type ActiveTab = 'model' | 'audit' | 'rag' | 'index' | 'retrieval'
type MetricTone = 'default' | 'success' | 'warning' | 'danger'

interface MetricCardItem {
  label: string
  value: string
  hint: string
  tone: MetricTone
  tag?: string
}

const activeTab = ref<ActiveTab>('model')
const modelPage = ref(1)
const modelSize = ref(20)
const auditPage = ref(1)
const auditSize = ref(20)
const ragPage = ref(1)
const ragSize = ref(20)
const indexPage = ref(1)
const indexSize = ref(20)
const retrievalPage = ref(1)
const retrievalSize = ref(20)

const filterForm = reactive<{
  traceId: string
  modelName: string
  modelStatus: ModelCallStatus | ''
  auditResult: AuditResult | ''
  ragStatus: RagRetrievalStatus | ''
  ragRetrievalMode: string
  workflowRunId: string
  workflowStepId: string
  businessDomain: string
  indexStatus: GraphRagIndexStatus | ''
  retrievalStatus: GraphRagRetrievalStatus | ''
  retrievalMode: string
  createdRange: DateRange
}>({
  traceId: '',
  modelName: '',
  modelStatus: '',
  auditResult: '',
  ragStatus: '',
  ragRetrievalMode: '',
  workflowRunId: '',
  workflowStepId: '',
  businessDomain: '',
  indexStatus: '',
  retrievalStatus: '',
  retrievalMode: '',
  createdRange: null,
})

const appliedFilters = reactive<{
  traceId: string
  modelName: string
  modelStatus: ModelCallStatus | ''
  auditResult: AuditResult | ''
  ragStatus: RagRetrievalStatus | ''
  ragRetrievalMode: string
  workflowRunId: string
  workflowStepId: string
  businessDomain: string
  indexStatus: GraphRagIndexStatus | ''
  retrievalStatus: GraphRagRetrievalStatus | ''
  retrievalMode: string
  createdRange: DateRange
}>({
  traceId: '',
  modelName: '',
  modelStatus: '',
  auditResult: '',
  ragStatus: '',
  ragRetrievalMode: '',
  workflowRunId: '',
  workflowStepId: '',
  businessDomain: '',
  indexStatus: '',
  retrievalStatus: '',
  retrievalMode: '',
  createdRange: null,
})

const summaryQuery = useQuery({
  queryKey: ['observability', 'summary'],
  queryFn: () => observabilityApi.getSummary(),
})

const modelCallsQuery = useQuery({
  queryKey: computed(() => [
    'observability',
    'model-calls',
    {
      page: modelPage.value - 1,
      size: modelSize.value,
      modelName: appliedFilters.modelName || undefined,
      status: appliedFilters.modelStatus || undefined,
      traceId: appliedFilters.traceId || undefined,
      createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
      createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
    },
  ]),
  queryFn: () => observabilityApi.listModelCalls({
    page: modelPage.value - 1,
    size: modelSize.value,
    modelName: appliedFilters.modelName || undefined,
    status: appliedFilters.modelStatus || undefined,
    traceId: appliedFilters.traceId || undefined,
    createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
    createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
  }),
})

const auditLogsQuery = useQuery({
  queryKey: computed(() => [
    'observability',
    'audit-logs',
    {
      page: auditPage.value - 1,
      size: auditSize.value,
      result: appliedFilters.auditResult || undefined,
      traceId: appliedFilters.traceId || undefined,
      createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
      createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
    },
  ]),
  queryFn: () => observabilityApi.listAuditLogs({
    page: auditPage.value - 1,
    size: auditSize.value,
    result: appliedFilters.auditResult || undefined,
    traceId: appliedFilters.traceId || undefined,
    createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
    createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
  }),
})

const ragLogsQuery = useQuery({
  queryKey: computed(() => [
    'observability',
    'rag-retrievals',
    {
      page: ragPage.value - 1,
      size: ragSize.value,
      traceId: appliedFilters.traceId || undefined,
      retrievalMode: appliedFilters.ragRetrievalMode || undefined,
      businessDomain: appliedFilters.businessDomain || undefined,
      status: appliedFilters.ragStatus || undefined,
      createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
      createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
    },
  ]),
  queryFn: () => observabilityApi.listRagRetrievalLogs({
    page: ragPage.value - 1,
    size: ragSize.value,
    traceId: appliedFilters.traceId || undefined,
    retrievalMode: appliedFilters.ragRetrievalMode || undefined,
    businessDomain: appliedFilters.businessDomain || undefined,
    status: appliedFilters.ragStatus || undefined,
    createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
    createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
  }),
})

const indexLogsQuery = useQuery({
  queryKey: computed(() => [
    'observability',
    'graphrag',
    'index-logs',
    {
      page: indexPage.value - 1,
      size: indexSize.value,
      traceId: appliedFilters.traceId || undefined,
      status: appliedFilters.indexStatus || undefined,
      createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
      createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
    },
  ]),
  queryFn: () => observabilityApi.listGraphRagIndexLogs({
    page: indexPage.value - 1,
    size: indexSize.value,
    traceId: appliedFilters.traceId || undefined,
    status: appliedFilters.indexStatus || undefined,
    createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
    createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
  }),
})

const retrievalLogsQuery = useQuery({
  queryKey: computed(() => [
    'observability',
    'graphrag',
    'retrieval-logs',
    {
      page: retrievalPage.value - 1,
      size: retrievalSize.value,
      traceId: appliedFilters.traceId || undefined,
      workflowRunId: appliedFilters.workflowRunId || undefined,
      workflowStepId: appliedFilters.workflowStepId || undefined,
      retrievalMode: appliedFilters.retrievalMode || undefined,
      businessDomain: appliedFilters.businessDomain || undefined,
      status: appliedFilters.retrievalStatus || undefined,
      createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
      createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
    },
  ]),
  queryFn: () => observabilityApi.listGraphRagRetrievalLogs({
    page: retrievalPage.value - 1,
    size: retrievalSize.value,
    traceId: appliedFilters.traceId || undefined,
    workflowRunId: appliedFilters.workflowRunId || undefined,
    workflowStepId: appliedFilters.workflowStepId || undefined,
    retrievalMode: appliedFilters.retrievalMode || undefined,
    businessDomain: appliedFilters.businessDomain || undefined,
    status: appliedFilters.retrievalStatus || undefined,
    createdFrom: toIso(appliedFilters.createdRange?.[0] ?? null),
    createdTo: toIso(appliedFilters.createdRange?.[1] ?? null),
  }),
})

const summary = computed(() => summaryQuery.data.value ?? null)
const graphRagSummary = computed(() => summary.value?.graphRagSummary ?? null)
const modelCalls = computed(() => modelCallsQuery.data.value?.items ?? [])
const auditLogs = computed(() => auditLogsQuery.data.value?.items ?? [])
const ragLogs = computed(() => ragLogsQuery.data.value?.items ?? [])
const indexLogs = computed(() => indexLogsQuery.data.value?.items ?? [])
const retrievalLogs = computed(() => retrievalLogsQuery.data.value?.items ?? [])
const modelTotal = computed(() => modelCallsQuery.data.value?.total ?? 0)
const auditTotal = computed(() => auditLogsQuery.data.value?.total ?? 0)
const ragTotal = computed(() => ragLogsQuery.data.value?.total ?? 0)
const indexTotal = computed(() => indexLogsQuery.data.value?.total ?? 0)
const retrievalTotal = computed(() => retrievalLogsQuery.data.value?.total ?? 0)
const refreshing = computed(() =>
  summaryQuery.isFetching.value
    || modelCallsQuery.isFetching.value
    || auditLogsQuery.isFetching.value
    || ragLogsQuery.isFetching.value
    || indexLogsQuery.isFetching.value
    || retrievalLogsQuery.isFetching.value,
)

const metricCards = computed<MetricCardItem[]>(() => {
  const data = summary.value
  return [
    {
      label: '模型调用耗时',
      value: formatDuration(data?.modelCallSummary.averageDurationMs),
      hint: `${data?.modelCallSummary.total ?? 0} 次调用，失败率 ${formatRate(data?.modelCallSummary.failureRate)}`,
      tone: riskTone(data?.modelCallSummary.failureRate),
    },
    {
      label: 'RAG 检索耗时',
      value: formatDuration(data?.ragSummary.averageDurationMs),
      hint: `${data?.ragSummary.total ?? 0} 次检索，引用 ${data?.ragSummary.citationCount ?? 0}`,
      tone: riskTone(data?.ragSummary.failureRate),
    },
    {
      label: 'GraphRAG 路径数量',
      value: String(data?.graphRagSummary.latestRetrievalLog?.filteredPathCount ?? 0),
      hint: `${data?.graphRagSummary.retrievalLogCount ?? 0} 条检索日志`,
      tone: data?.graphRagSummary.latestRetrievalLog?.status === 'FAILED' ? 'danger' : 'default',
    },
    {
      label: '工具失败率',
      value: formatRate(data?.toolSummary.failureRate),
      hint: `${data?.toolSummary.total ?? 0} 次工具调用，拒绝率 ${formatRate(data?.toolSummary.deniedRate)}`,
      tone: riskTone(data?.toolSummary.failureRate),
    },
    {
      label: 'SSE 连接时长',
      value: formatDuration(data?.sseSummary.averageConnectionDurationMs),
      hint: `${Math.round(data?.sseSummary.activeConnections ?? 0)} 个活动连接`,
      tone: (data?.sseSummary.failedConnections ?? 0) > 0 ? 'warning' : 'default',
    },
    {
      label: '工作流运行耗时',
      value: formatDuration(data?.workflowSummary.averageDurationMs),
      hint: `${data?.workflowSummary.total ?? 0} 次运行，失败率 ${formatRate(data?.workflowSummary.failureRate)}`,
      tone: riskTone(data?.workflowSummary.failureRate),
    },
    {
      label: '依赖健康',
      value: data?.healthSummary.status ?? '-',
      hint: healthHint(data?.healthSummary.components ?? {}),
      tone: data?.healthSummary.status === 'UP' ? 'success' : 'danger',
      tag: `${Object.keys(data?.healthSummary.components ?? {}).length} components`,
    },
  ]
})

const modelDurationTrend = computed<MetricTrendPoint[]>(() =>
  [...modelCalls.value]
    .reverse()
    .slice(-12)
    .map((call) => ({
      label: formatShortTime(call.createdAt),
      value: call.durationMs,
    })),
)

const errorDistribution = computed<ErrorDistributionItem[]>(() => {
  const data = summary.value
  if (!data) {
    return []
  }

  return [
    { label: '模型失败', value: data.modelCallSummary.failed, color: '#c03639' },
    { label: '模型超时', value: data.modelCallSummary.timedOut, color: '#a15c09' },
    { label: 'RAG 失败', value: data.ragSummary.failed, color: '#d9480f' },
    { label: 'RAG 降级', value: data.ragSummary.degraded, color: '#b7791f' },
    { label: '工具失败', value: data.toolSummary.failed, color: '#9f1239' },
    { label: '工具拒绝', value: data.toolSummary.denied, color: '#7c3aed' },
    { label: '工具超时', value: data.toolSummary.timeout, color: '#0369a1' },
    { label: '工作流失败', value: data.workflowSummary.failed, color: '#be123c' },
    { label: '工作流取消', value: data.workflowSummary.cancelled, color: '#64748b' },
    { label: 'SSE 失败', value: Math.round(data.sseSummary.failedConnections), color: '#dc2626' },
    { label: 'SSE 超时', value: Math.round(data.sseSummary.timedOutConnections), color: '#ea580c' },
  ].filter((item) => item.value > 0)
})

const summaryError = computed(() => {
  if (!summaryQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(summaryQuery.error.value, '监控摘要加载失败')
})
const modelListError = computed(() => {
  if (!modelCallsQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(modelCallsQuery.error.value, '模型调用记录加载失败')
})
const auditListError = computed(() => {
  if (!auditLogsQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(auditLogsQuery.error.value, '审计日志加载失败')
})
const ragListError = computed(() => {
  if (!ragLogsQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(ragLogsQuery.error.value, 'RAG 检索日志加载失败')
})
const indexListError = computed(() => {
  if (!indexLogsQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(indexLogsQuery.error.value, 'GraphRAG 构建日志加载失败')
})
const retrievalListError = computed(() => {
  if (!retrievalLogsQuery.isError.value) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(retrievalLogsQuery.error.value, 'GraphRAG 检索日志加载失败')
})
const activeListError = computed(() => {
  const errors = {
    model: modelListError.value,
    audit: auditListError.value,
    rag: ragListError.value,
    index: indexListError.value,
    retrieval: retrievalListError.value,
  }
  return errors[activeTab.value]
})

watch(modelSize, () => {
  modelPage.value = 1
})

watch(auditSize, () => {
  auditPage.value = 1
})

watch(ragSize, () => {
  ragPage.value = 1
})

watch(indexSize, () => {
  indexPage.value = 1
})

watch(retrievalSize, () => {
  retrievalPage.value = 1
})

function searchLogs(): void {
  appliedFilters.traceId = filterForm.traceId.trim()
  appliedFilters.modelName = filterForm.modelName.trim()
  appliedFilters.modelStatus = filterForm.modelStatus
  appliedFilters.auditResult = filterForm.auditResult
  appliedFilters.ragStatus = filterForm.ragStatus
  appliedFilters.ragRetrievalMode = filterForm.ragRetrievalMode.trim()
  appliedFilters.workflowRunId = filterForm.workflowRunId.trim()
  appliedFilters.workflowStepId = filterForm.workflowStepId.trim()
  appliedFilters.businessDomain = filterForm.businessDomain.trim()
  appliedFilters.indexStatus = filterForm.indexStatus
  appliedFilters.retrievalStatus = filterForm.retrievalStatus
  appliedFilters.retrievalMode = filterForm.retrievalMode.trim()
  appliedFilters.createdRange = filterForm.createdRange
  modelPage.value = 1
  auditPage.value = 1
  ragPage.value = 1
  indexPage.value = 1
  retrievalPage.value = 1
}

function resetFilters(): void {
  filterForm.traceId = ''
  filterForm.modelName = ''
  filterForm.modelStatus = ''
  filterForm.auditResult = ''
  filterForm.ragStatus = ''
  filterForm.ragRetrievalMode = ''
  filterForm.workflowRunId = ''
  filterForm.workflowStepId = ''
  filterForm.businessDomain = ''
  filterForm.indexStatus = ''
  filterForm.retrievalStatus = ''
  filterForm.retrievalMode = ''
  filterForm.createdRange = null
  searchLogs()
}

function refreshAll(): void {
  void summaryQuery.refetch()
  void modelCallsQuery.refetch()
  void auditLogsQuery.refetch()
  void ragLogsQuery.refetch()
  void indexLogsQuery.refetch()
  void retrievalLogsQuery.refetch()
}

function toIso(value: Date | null): string | undefined {
  return value ? value.toISOString() : undefined
}

function formatDuration(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '-'
  }

  return `${Math.round(value)} ms`
}

function formatRate(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '0.0%'
  }

  return `${(value * 100).toFixed(1)}%`
}

function formatShortTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function riskTone(rate: number | null | undefined): MetricTone {
  if (!rate) {
    return 'default'
  }
  if (rate >= 0.1) {
    return 'danger'
  }
  if (rate >= 0.03) {
    return 'warning'
  }
  return 'default'
}

function healthHint(components: Record<string, string>): string {
  const entries = Object.entries(components)
  if (!entries.length) {
    return '暂无组件健康明细'
  }
  const down = entries.filter(([, status]) => status !== 'UP')
  return down.length ? `${down.length} 个组件异常` : '全部组件正常'
}
</script>

<style scoped>
.observability-dashboard-view {
  display: grid;
  min-height: 0;
  gap: 16px;
}

.page-error {
  margin-bottom: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 12px;
}

.detail-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.log-surface {
  display: flex;
  min-height: 520px;
  flex-direction: column;
  overflow: hidden;
}

.log-tabs {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

.log-tabs :deep(.el-tabs__header) {
  flex: none;
  margin: 0;
  padding: 0 16px;
}

.log-tabs :deep(.el-tabs__content) {
  min-height: 0;
  flex: 1;
}

.log-tabs :deep(.el-tab-pane) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.log-table {
  min-height: 0;
  flex: 1;
}

.pagination-row {
  display: flex;
  flex: none;
  justify-content: flex-end;
  border-top: 1px solid #edf1f5;
  padding: 14px 16px;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }

  .detail-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .toolbar-filters > * {
    width: 100% !important;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .detail-metric-grid {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
