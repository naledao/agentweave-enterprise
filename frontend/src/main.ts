import { createApp } from 'vue'

import 'element-plus/dist/index.css'
import './style.css'

import App from './App.vue'
import { registerPlugins } from './app/plugins'
import { router } from './app/router'

const app = createApp(App)

registerPlugins(app)
app.use(router)

app.mount('#app')
