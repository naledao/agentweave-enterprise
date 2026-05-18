<template>
  <el-container class="app-shell">
    <el-aside class="app-sidebar" width="248px">
      <div class="brand">
        <div class="brand-mark">AW</div>
        <div>
          <strong>AgentWeave</strong>
          <span>Enterprise</span>
        </div>
      </div>

      <el-menu
        class="app-menu"
        :default-active="activePath"
        router
      >
        <el-menu-item
          v-for="menu in visibleMenus"
          :key="menu.key"
          :index="menu.path"
        >
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header" height="64px">
        <div>
          <p class="section-label">{{ routeTitle }}</p>
          <h1>{{ routeTitle }}</h1>
        </div>

        <el-dropdown trigger="click" @command="handleCommand">
          <button class="user-button" type="button">
            <span>{{ userInitial }}</span>
            <strong>{{ authStore.currentUser?.displayName ?? authStore.currentUser?.username }}</strong>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ authStore.currentUser?.username }}</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-container class="app-content-shell">
        <el-main class="app-main" :class="{ 'app-main--fixed': fixedMainRoutes.includes(String(route.name)) }">
          <RouterView />
        </el-main>

        <el-aside v-if="$slots.context" class="app-context-panel" width="320px">
          <slot name="context" />
        </el-aside>
      </el-container>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { appMenus } from '@/app/router/menus'
import { useAuthStore } from '@/features/auth/store/useAuthStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const fixedMainRoutes = ['Chat', 'KnowledgeDocuments', 'Roles', 'Tools', 'ToolInvocations', 'WorkflowRuns']

const activePath = computed(() => route.path)
const routeTitle = computed(() => route.meta.title ?? '工作台')
const visibleMenus = computed(() =>
  appMenus.filter((menu) => authStore.hasAnyPermission(menu.permissions)),
)
const userInitial = computed(() => {
  const name = authStore.currentUser?.displayName || authStore.currentUser?.username || 'A'
  return name.slice(0, 1).toUpperCase()
})

async function handleCommand(command: string): Promise<void> {
  if (command === 'logout') {
    await authStore.logout()
    await router.replace({ name: 'Login' })
  }
}
</script>

<style scoped>
.app-shell {
  height: 100vh;
  min-height: 0;
  background: #eef2f6;
  overflow: hidden;
}

.app-sidebar {
  border-right: 1px solid #dce3ed;
  background: #101722;
  color: #fff;
  overflow-y: auto;
}

.app-shell > .el-container {
  min-width: 0;
  min-height: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  background: #2f72ff;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.brand strong,
.brand span {
  display: block;
  line-height: 1.2;
}

.brand strong {
  font-size: 16px;
}

.brand span {
  color: #9aa8bc;
  font-size: 12px;
}

.app-menu {
  border-right: 0;
  background: transparent;
}

.app-menu :deep(.el-menu-item) {
  margin: 6px 10px;
  border-radius: 8px;
  color: #c8d2e0;
}

.app-menu :deep(.el-menu-item.is-active) {
  background: #20314a;
  color: #fff;
}

.app-menu :deep(.el-menu-item:hover) {
  background: #192536;
  color: #fff;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #dce3ed;
  background: #fff;
  padding: 0 24px;
}

.section-label {
  margin: 0 0 2px;
  color: #69778d;
  font-size: 12px;
}

h1 {
  margin: 0;
  color: #182233;
  font-size: 20px;
  font-weight: 700;
}

.user-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  color: #263143;
  cursor: pointer;
  padding: 0 12px;
}

.user-button > span {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 50%;
  background: #e8f0ff;
  color: #1f5fd8;
  font-size: 12px;
  font-weight: 800;
}

.user-button strong {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-main {
  min-height: 0;
  overflow: auto;
  padding: 24px;
}

.app-main--fixed {
  overflow: hidden;
}

.app-content-shell {
  min-width: 0;
  min-height: 0;
}

.app-context-panel {
  border-left: 1px solid #dce3ed;
  background: #f8fafc;
  overflow-y: auto;
  padding: 16px;
}

@media (max-width: 820px) {
  .app-shell {
    display: flex;
    flex-direction: column;
    overflow: auto;
  }

  .app-sidebar {
    width: 100% !important;
    flex: none;
    overflow: visible;
  }

  .app-menu {
    display: flex;
    overflow-x: auto;
  }

  .app-header {
    gap: 12px;
  }

  .app-content-shell {
    display: block;
  }

  .app-context-panel {
    width: 100% !important;
    border-left: 0;
    border-top: 1px solid #dce3ed;
  }
}
</style>
