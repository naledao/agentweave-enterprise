import { defineStore } from 'pinia'

interface UiState {
  sidebarCollapsed: boolean
  contextPanelOpen: boolean
  contextPanelMode: 'citations' | 'tools' | 'workflow' | 'trace'
}

export const useUiStore = defineStore('ui', {
  state: (): UiState => ({
    sidebarCollapsed: false,
    contextPanelOpen: true,
    contextPanelMode: 'citations',
  }),

  actions: {
    setSidebarCollapsed(collapsed: boolean): void {
      this.sidebarCollapsed = collapsed
    },

    toggleSidebar(): void {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },

    openContextPanel(mode?: UiState['contextPanelMode']): void {
      this.contextPanelMode = mode ?? this.contextPanelMode
      this.contextPanelOpen = true
    },

    closeContextPanel(): void {
      this.contextPanelOpen = false
    },
  },
})
