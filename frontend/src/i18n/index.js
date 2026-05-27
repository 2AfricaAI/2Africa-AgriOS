import { createI18n } from 'vue-i18n'
import en from './locales/en'
import zh from './locales/zh'
import sw from './locales/sw'

import enElement from 'element-plus/es/locale/lang/en'
import zhElement from 'element-plus/es/locale/lang/zh-cn'
// Element Plus 没有 sw 内置语言包, 沿用英文 (Element 控件文案非核心)

export const SUPPORT_LOCALES = [
  { code: 'en', label: 'English',   flag: 'EN', element: enElement },
  { code: 'zh', label: '中文',      flag: '中', element: zhElement },
  { code: 'sw', label: 'Kiswahili', flag: 'SW', element: enElement },
]

const STORAGE_KEY = 'agrios.locale'

export function getStoredLocale() {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    if (v && SUPPORT_LOCALES.some(l => l.code === v)) return v
  } catch {}
  return 'en'   // 默认 EN, 面向非洲本地员工
}

export function persistLocale(code) {
  try { localStorage.setItem(STORAGE_KEY, code) } catch {}
}

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: getStoredLocale(),
  fallbackLocale: 'en',
  messages: { en, zh, sw },
  missingWarn: false,
  fallbackWarn: false,
})

export function getElementLocale(code = i18n.global.locale.value) {
  return SUPPORT_LOCALES.find(l => l.code === code)?.element || enElement
}

export default i18n
