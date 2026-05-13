import { VueQueryPlugin } from '@tanstack/vue-query'
import ElementPlus from 'element-plus'
import { createPinia } from 'pinia'
import { createApp } from 'vue'

import 'element-plus/dist/index.css'
import './style.css'

import App from './App.vue'
import { router } from './app/router'

const app = createApp(App)

app.use(createPinia())
app.use(VueQueryPlugin)
app.use(ElementPlus)
app.use(router)

app.mount('#app')
