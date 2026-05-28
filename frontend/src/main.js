import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { installPerms } from '@/utils/perms'
import './style.css'

const app = createApp(App)

// 顺序很重要: Pinia 必须先于 router(router 守卫里要用 store)
app.use(createPinia())
app.use(router)
app.use(i18n)
// Element Plus 的 locale 由 App.vue 里的 <el-config-provider> 接管,
// 这样切换 i18n 语言时,分页/日期选择器/确认按钮等会一起切。
app.use(ElementPlus)
// Sprint 35: register v-perm + $hasPerm globals. Pinia must already be set up.
installPerms(app)

app.mount('#app')
