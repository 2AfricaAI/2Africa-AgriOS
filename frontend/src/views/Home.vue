<template>
  <el-card shadow="hover">
    <template #header>
      <span>🎉 登录成功 - 鉴权链路已打通</span>
    </template>

    <el-descriptions :column="1" border>
      <el-descriptions-item label="用户 ID">{{ auth.userId }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ auth.username }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ auth.nickname }}</el-descriptions-item>
      <el-descriptions-item label="角色">
        <el-tag
          v-for="r in auth.roles"
          :key="r"
          type="primary"
          size="small"
          style="margin-right: 6px"
        >{{ r }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="权限点">
        <span v-if="auth.permissions.length === 0" style="color: #909399">(无显式权限点, 走角色判断)</span>
        <template v-else>
          <el-tag
            v-for="p in auth.permissions"
            :key="p"
            size="small"
            style="margin-right: 6px"
          >{{ p }}</el-tag>
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="Access Token">
        <code class="token">{{ auth.accessToken.slice(0, 40) }}...</code>
      </el-descriptions-item>
    </el-descriptions>

    <div class="api-test">
      <el-button type="primary" :loading="testLoading" @click="testCallMe">
        调一下 GET /v1/auth/me 试试
      </el-button>
      <pre v-if="meResult" class="api-result">{{ meResult }}</pre>
    </div>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { me } from '@/api/auth'

const auth = useAuthStore()
const testLoading = ref(false)
const meResult = ref('')

async function testCallMe() {
  testLoading.value = true
  try {
    const data = await me()
    meResult.value = JSON.stringify(data, null, 2)
  } catch {
    meResult.value = ''
  } finally {
    testLoading.value = false
  }
}
</script>

<style scoped>
.token {
  font-family: 'Consolas', 'Monaco', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  word-break: break-all;
}

.api-test {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px dashed #ebeef5;
}

.api-result {
  margin-top: 14px;
  padding: 14px;
  background: #f6f8fa;
  border-radius: 6px;
  font-size: 12px;
  max-height: 400px;
  overflow: auto;
  white-space: pre-wrap;
}
</style>
