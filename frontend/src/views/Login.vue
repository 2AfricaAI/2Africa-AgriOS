<template>
  <div class="login-wrap">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-header">
          <span class="dot"></span>
          <span>2Africa AgriOS</span>
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
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="admin"
            :prefix-icon="UserIcon"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
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
          登 录
        </el-button>
      </el-form>

      <p class="hint">默认账号: admin / Admin@123456</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User as UserIcon, Lock as LockIcon } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'Admin@123456',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await login(form.username, form.password)
    auth.setLogin(data)
    ElMessage.success(`欢迎回来,${data.nickname || data.username}`)
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    // 错误已在 axios 拦截器里 ElMessage 提示了, 这里只防止 loading 一直转
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
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 50%, #a5d6a7 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  border-radius: 12px;
}

.login-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  color: #1f2329;
}

.login-header .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #52c41a;
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
