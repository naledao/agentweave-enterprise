import type { RouteLocationNormalized } from 'vue-router'
import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/features/auth/store/useAuthStore'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    permissions?: string[]
    title?: string
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/app/chat',
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/features/auth/views/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/app',
      component: () => import('@/app/layouts/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/app/chat',
        },
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/features/chat/views/ChatWorkspaceView.vue'),
          meta: { requiresAuth: true, title: '对话' },
        },
        {
          path: 'conversations',
          name: 'Conversations',
          component: () => import('@/features/conversations/views/ConversationListView.vue'),
          meta: { requiresAuth: true, title: '会话' },
        },
        {
          path: 'knowledge/documents',
          name: 'KnowledgeDocuments',
          component: () => import('@/features/knowledge/views/DocumentListView.vue'),
          meta: { requiresAuth: true, title: '知识库文档' },
        },
        {
          path: 'knowledge/documents/:documentId',
          name: 'KnowledgeDocumentDetail',
          component: () => import('@/features/knowledge/views/DocumentDetailView.vue'),
          meta: { requiresAuth: true, title: '文档详情' },
        },
        {
          path: 'tools',
          name: 'Tools',
          component: () => import('@/features/tools/views/ToolCenterView.vue'),
          meta: { requiresAuth: true, title: '工具中心' },
        },
        {
          path: 'settings/users',
          name: 'Users',
          component: () => import('@/features/settings/views/UserManagementView.vue'),
          meta: {
            requiresAuth: true,
            permissions: ['auth:user:read'],
            title: '用户管理',
          },
        },
        {
          path: 'settings/roles',
          name: 'Roles',
          component: () => import('@/features/settings/views/RoleManagementView.vue'),
          meta: {
            requiresAuth: true,
            permissions: ['auth:role:read'],
            title: '角色管理',
          },
        },
        {
          path: '403',
          name: 'Forbidden',
          component: () => import('@/shared/components/ForbiddenView.vue'),
          meta: { requiresAuth: true, title: '无权限' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/app/chat',
    },
  ],
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} - AgentWeave Enterprise` : 'AgentWeave Enterprise'

  const authStore = useAuthStore()

  if (!to.meta.requiresAuth) {
    if (to.name === 'Login' && authStore.token && !authStore.currentUser) {
      await tryRestoreSession(authStore)
    }

    if (to.name === 'Login' && authStore.isAuthenticated) {
      return redirectAfterLogin(to)
    }

    return true
  }

  await tryRestoreSession(authStore)

  if (!authStore.token) {
    return {
      name: 'Login',
      query: { redirect: to.fullPath },
    }
  }

  const permissions = to.meta.permissions ?? []
  if (!authStore.hasAnyPermission(permissions)) {
    return { name: 'Forbidden' }
  }

  return true
})

window.addEventListener('agentweave:unauthorized', () => {
  const authStore = useAuthStore()
  authStore.clearSession()

  if (router.currentRoute.value.name !== 'Login') {
    void router.replace({
      name: 'Login',
      query: { redirect: router.currentRoute.value.fullPath, reason: 'expired' },
    })
  }
})

async function tryRestoreSession(authStore: ReturnType<typeof useAuthStore>): Promise<void> {
  try {
    await authStore.restoreSession()
  } catch {
    authStore.clearSession()
  }
}

function redirectAfterLogin(to: RouteLocationNormalized) {
  const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/app/chat'
  return redirect
}
