import {
  ChatLineRound,
  Collection,
  DataAnalysis,
  Lock,
  MessageBox,
  Operation,
  Tools,
  User,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

export interface AppMenu {
  key: string
  path: string
  title: string
  permissions: string[]
  icon: Component
}

export const appMenus: AppMenu[] = [
  {
    key: 'chat',
    path: '/app/chat',
    title: '对话',
    permissions: [],
    icon: ChatLineRound,
  },
  {
    key: 'conversations',
    path: '/app/conversations',
    title: '会话',
    permissions: [],
    icon: MessageBox,
  },
  {
    key: 'knowledge-documents',
    path: '/app/knowledge/documents',
    title: '知识库文档',
    permissions: [],
    icon: Collection,
  },
  {
    key: 'tools',
    path: '/app/tools',
    title: '工具中心',
    permissions: [],
    icon: Tools,
  },
  {
    key: 'tool-invocations',
    path: '/app/tools/invocations',
    title: '调用记录',
    permissions: [],
    icon: Operation,
  },
  {
    key: 'workflow-runs',
    path: '/app/workflows/runs',
    title: '工作流运行',
    permissions: [],
    icon: Operation,
  },
  {
    key: 'observability',
    path: '/app/observability',
    title: '监控观测',
    permissions: ['observability:read'],
    icon: DataAnalysis,
  },
  {
    key: 'users',
    path: '/app/settings/users',
    title: '用户管理',
    permissions: ['auth:user:read'],
    icon: User,
  },
  {
    key: 'roles',
    path: '/app/settings/roles',
    title: '角色管理',
    permissions: ['auth:role:read'],
    icon: Lock,
  },
]
