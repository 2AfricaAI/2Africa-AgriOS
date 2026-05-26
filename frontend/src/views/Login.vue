<template>
  <div class="login-wrap">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-header">
          <img src="/logo.svg" alt="2Africa AgriOS" class="login-logo" />
          <span class="lang-mini" @click="toggleLang">{{ otherLocaleFlag }}</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        label-position="top"
        @keyup.enter="onSubmit"
      >
        <el-form-item :label="t('auth.username')" prop="username">
          <el-input
            v-model="form.username"
            placeholder="admin"
            :prefix-icon="UserIcon"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item :label="t('auth.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('auth.passwordPlaceholder')"
            :prefix-icon="LockIcon"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-button
          type="primary"
          :loading="loading"
          class="submit-btn"
          @click="onSubmit"
        >
          {{ t('auth.login') }}
        </el-button>
      </el-form>

      <p class="hint">Default: admin / Admin@123456</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User as UserIcon, Lock as LockIcon } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { SUPPORT_LOCALES, persistLocale } from '@/i18n'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { t, locale } = useI18n()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'Admin@123456',
})

const rules = computed(() => ({
  username: [{ required: true, message: t('auth.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('auth.passwordRequired'), trigger: 'blur' }],
}))

const otherLocaleFlag = computed(() => {
  const other = SUPPORT_LOCALES.find(l => l.code !== locale.value)
  return other ? other.flag : ''
})
function toggleLang() {
  const next = SUPPORT_LOCALES.find(l => l.code !== locale.value)
  if (!next) return
  locale.value = next.code
  persistLocale(next.code)
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await login(form.username, form.password)
    auth.setLogin(data)
    ElMessage.success(t('home.welcomeBack', { name: data.nickname || data.username }))
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    // axios 拦截器已经 ElMessage
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse at top left, rgba(82, 196, 26, 0.25), transparent 55%),
    radial-gradient(ellipse at bottom right, rgba(22, 119, 255, 0.15), transparent 50%),
    linear-gradient(135deg, #f3faf4 0%, #e3f5e8 50%, #c8e6c9 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(15, 58, 38, 0.12);
  border: 1px solid #e7f7ec;
}

.login-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.login-logo {
  height: 36px;
  max-width: 240px;
  object-fit: contain;
}

.lang-mini {
  margin-left: auto;
  cursor: pointer;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.4px;
  user-select: none;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  font-weight: 600;
  letter-spacing: 6px;
}

.hint {
  margin-top: 14px;
  text-align: center;
  color: #909399;
  font-size: 12px;
}
</style>
