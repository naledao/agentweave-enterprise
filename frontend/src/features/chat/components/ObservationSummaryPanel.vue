<template>
  <section class="panel-section">
    <div class="panel-title">
      <h3>调用链摘要</h3>
      <el-tag :type="statusMeta.type" effect="plain">{{ statusMeta.label }}</el-tag>
    </div>

    <TraceIdText v-if="primaryTraceId" :trace-id="primaryTraceId" />

    <el-timeline class="summary-timeline">
      <el-timeline-item
        v-for="item in summaryItems"
        :key="item.key"
        :type="item.type"
        :timestamp="item.value"
      >
        <strong>{{ item.label }}</strong>
        <p v-if="item.detail">{{ item.detail }}</p>
        <TraceIdText v-if="item.traceId" class="item-trace" :trace-id="item.traceId" />
      </el-timeline-item>
    </el-timeline>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type {
  ChatMessage,
  ChatStreamState,
  GraphPath,
  RagCitation,
  ToolInvocation,
  WorkflowStep,
} from '@/features/chat/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

type TimelineType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

interface SummaryItem {
  key: string
  label: string
  value: string
  detail: string
  type: TimelineType
  traceId?: string | null
}

const props = defineProps<{
  message: ChatMessage | null
  stream: ChatStreamState
  conversationTraceId?: string | null
  citations: RagCitation[]
  graphPaths: GraphPath[]
  toolInvocations: ToolInvocation[]
  workflowSteps: WorkflowStep[]
}>()

const primaryTraceId = computed(() =>
  props.stream.error?.traceId
    ?? firstTraceId(props.toolInvocations)
    ?? firstTraceId(props.workflowSteps)
    ?? props.message?.traceId
    ?? props.conversationTraceId
    ?? null,
)

const statusMeta = computed<{ label: string; type: 'info' | 'success' | 'warning' | 'danger' }>(() => {
  if (props.stream.status === 'failed' || props.message?.status === 'FAILED') {
    return { label: '异常', type: 'danger' }
  }
  if (props.stream.status === 'completed' || props.message?.status === 'SUCCEEDED') {
    return { label: '完成', type: 'success' }
  }
  if (['connecting', 'streaming', 'tool_calling'].includes(props.stream.status)) {
    return { label: '执行中', type: 'warning' }
  }
  return { label: '待观察', type: 'info' }
})

const summaryItems = computed<SummaryItem[]>(() => [
  {
    key: 'message',
    label: '消息生成',
    value: messageStatusText.value,
    detail: props.message?.errorMessage || props.stream.error?.message || messageDetail.value,
    type: props.stream.error || props.message?.status === 'FAILED' ? 'danger' : statusMeta.value.type,
    traceId: props.message?.traceId ?? null,
  },
  {
    key: 'rag',
    label: 'Vector RAG',
    value: `${props.citations.length} 条引用`,
    detail: props.citations.length ? citationPreview.value : '未返回引用资料',
    type: props.citations.length ? 'success' : 'info',
  },
  {
    key: 'graphrag',
    label: 'GraphRAG',
    value: `${props.graphPaths.length} 条路径`,
    detail: props.graphPaths.length ? graphPathPreview.value : '未返回图谱路径',
    type: props.graphPaths.length ? 'success' : 'info',
  },
  {
    key: 'tools',
    label: '工具调用',
    value: toolSummary.value,
    detail: toolPreview.value,
    type: failedToolCount.value > 0 ? 'danger' : props.toolInvocations.length ? 'success' : 'info',
    traceId: firstTraceId(props.toolInvocations),
  },
  {
    key: 'workflow',
    label: '工作流步骤',
    value: workflowSummary.value,
    detail: workflowPreview.value,
    type: failedWorkflowCount.value > 0 ? 'danger' : props.workflowSteps.length ? 'success' : 'info',
    traceId: firstTraceId(props.workflowSteps),
  },
])

const messageStatusText = computed(() => {
  if (props.stream.status !== 'idle') {
    const map: Record<ChatStreamState['status'], string> = {
      idle: '就绪',
      connecting: '连接中',
      streaming: '生成中',
      tool_calling: '工具调用中',
      completed: '完成',
      failed: '失败',
      cancelled: '已取消',
    }
    return map[props.stream.status]
  }

  if (!props.message) {
    return '暂无消息'
  }

  const map: Record<ChatMessage['status'], string> = {
    PENDING: '等待生成',
    STREAMING: '生成中',
    SUCCEEDED: '完成',
    FAILED: '失败',
    CANCELLED: '已取消',
  }
  return map[props.message.status]
})

const messageDetail = computed(() => {
  if (props.stream.content.trim()) {
    return `已生成 ${props.stream.content.length} 个字符`
  }
  if (props.message?.content.trim()) {
    return `已写入 ${props.message.content.length} 个字符`
  }
  return '暂无模型输出正文'
})

const citationPreview = computed(() =>
  props.citations
    .slice(0, 2)
    .map((citation) => citation.title)
    .join(' / '),
)

const graphPathPreview = computed(() =>
  props.graphPaths
    .slice(0, 2)
    .map((path) => path.entities.join(' -> '))
    .join(' / '),
)

const failedToolCount = computed(() => props.toolInvocations.filter((tool) => tool.status === 'FAILED').length)
const toolSummary = computed(() => {
  if (!props.toolInvocations.length) {
    return '0 次调用'
  }
  const succeeded = props.toolInvocations.filter((tool) => tool.status === 'SUCCEEDED').length
  return `${succeeded}/${props.toolInvocations.length} 成功`
})
const toolPreview = computed(() =>
  props.toolInvocations.length
    ? props.toolInvocations.map((tool) => tool.toolName).slice(0, 3).join(' / ')
    : '未触发工具调用',
)

const failedWorkflowCount = computed(() => props.workflowSteps.filter((step) => step.status === 'FAILED').length)
const workflowSummary = computed(() => {
  if (!props.workflowSteps.length) {
    return '0 个步骤'
  }
  const succeeded = props.workflowSteps.filter((step) => step.status === 'SUCCEEDED').length
  return `${succeeded}/${props.workflowSteps.length} 成功`
})
const workflowPreview = computed(() =>
  props.workflowSteps.length
    ? props.workflowSteps.map((step) => step.stepName).slice(0, 3).join(' / ')
    : '未触发工作流步骤',
)

function firstTraceId(items: Array<{ traceId?: string | null }>): string | null {
  return items.find((item) => item.traceId)?.traceId ?? null
}
</script>

<style scoped>
.panel-section {
  display: grid;
  gap: 10px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-title h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.summary-timeline {
  margin: 0;
}

:deep(.el-timeline) {
  padding-left: 4px;
}

strong {
  color: #263143;
  font-size: 13px;
}

p {
  overflow: hidden;
  margin: 4px 0 0;
  color: #69778d;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-trace {
  margin-top: 6px;
}
</style>
