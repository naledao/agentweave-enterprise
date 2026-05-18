import { storeToRefs } from 'pinia'

import { useUiStore } from '@/app/store/useUiStore'

export function useAppShell() {
  const uiStore = useUiStore()
  const { sidebarCollapsed, contextPanelOpen, contextPanelMode } = storeToRefs(uiStore)

  return {
    sidebarCollapsed,
    contextPanelOpen,
    contextPanelMode,
    closeContextPanel: uiStore.closeContextPanel,
    openContextPanel: uiStore.openContextPanel,
    setSidebarCollapsed: uiStore.setSidebarCollapsed,
    toggleSidebar: uiStore.toggleSidebar,
  }
}
