import { VueQueryPlugin, QueryClient } from '@tanstack/vue-query'
import ElementPlus from 'element-plus'
import { createPinia } from 'pinia'
import type { App } from 'vue'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30_000,
    },
    mutations: {
      retry: 0,
    },
  },
})

export function registerPlugins(app: App): void {
  app.use(createPinia())
  app.use(VueQueryPlugin, { queryClient })
  app.use(ElementPlus)
}
