<template>
  <section class="conversation-page">
    <PageHeader title="会话" eyebrow="Conversations" />

    <el-alert
      v-if="pageError || listError"
      class="page-error"
      :title="pageError || listError"
      type="error"
      :closable="false"
      show-icon
    />

    <div class="page-surface">
      <ConversationList
        :conversations="conversations"
        :active-conversation-id="activeConversationId"
        :loading="conversationsQuery.isFetching.value"
        :creating="createConversationMutation.isPending.value"
        :keyword="keyword"
        searchable
        @create="createConversation"
        @select="openConversation"
        @search="searchConversations"
      />

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { conversationsApi } from '@/features/chat/api/conversationsApi'
import ConversationList from '@/features/chat/components/ConversationList.vue'
import PageHeader from '@/shared/components/PageHeader.vue'
import { formatApiError } from '@/shared/utils/apiError'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const pageError = ref('')
const page = ref(1)
const size = ref(20)
const keyword = ref('')

const activeConversationId = computed(() => {
  const conversationId = route.query.conversationId
  return typeof conversationId === 'string' && conversationId.trim() ? conversationId : null
})

const conversationsQuery = useQuery({
  queryKey: computed(() => ['conversations', {
    page: page.value - 1,
    size: size.value,
    keyword: keyword.value || undefined,
  }]),
  queryFn: () => conversationsApi.queryConversations({
    page: page.value - 1,
    size: size.value,
    keyword: keyword.value || undefined,
  }),
})

const createConversationMutation = useMutation({
  mutationFn: () => conversationsApi.createConversation({ title: '新的对话' }),
  onSuccess: async (conversation) => {
    pageError.value = ''
    await queryClient.invalidateQueries({ queryKey: ['conversations'] })
    await router.push({ name: 'Chat', query: { conversationId: conversation.id } })
  },
  onError: (error) => {
    pageError.value = formatApiError(error)
  },
})

const conversations = computed(() => conversationsQuery.data.value?.items ?? [])
const total = computed(() => conversationsQuery.data.value?.total ?? 0)
const listError = computed(() => {
  if (!conversationsQuery.isError.value || !conversationsQuery.error.value) {
    return ''
  }

  return formatApiError(conversationsQuery.error.value)
})

watch(size, () => {
  page.value = 1
})

function createConversation(): void {
  createConversationMutation.mutate()
}

function searchConversations(value: string): void {
  keyword.value = value
  page.value = 1
}

async function openConversation(conversationId: string): Promise<void> {
  pageError.value = ''
  await router.push({ name: 'Chat', query: { conversationId } })
}
</script>

<style scoped>
.conversation-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: calc(100vh - 128px);
}

.page-error {
  flex: 0 0 auto;
}

.page-surface {
  display: grid;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.page-surface :deep(.conversation-list) {
  min-height: calc(100vh - 244px);
  border: 0;
  border-radius: 0;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #edf1f5;
  padding: 14px 16px;
}
</style>
