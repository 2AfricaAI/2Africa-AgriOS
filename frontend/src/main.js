import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)

// 顺序很重要: Pinia 必须先于 router(router 守卫里要用 store)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
