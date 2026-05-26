<template>
  <div class="page">
    <!-- 介绍卡片 -->
    <el-card shadow="never">
      <template #header>
        <div class="hdr">
          <span>{{ t('file.demoIntroHeader') }}</span>
          <el-tag size="small" type="info">{{ t('file.demoIntroTag') }}</el-tag>
        </div>
      </template>
      <p class="intro">
        {{ t('file.demoIntro') }}
      </p>
    </el-card>

    <!-- 多文件 - 图片 (典型用法 1) -->
    <el-card shadow="never">
      <template #header>{{ t('file.demoGalleryHeader') }}</template>
      <FileUploader
        v-model="gallery"
        biz-type="demo_gallery"
        accept="image/*"
        :limit="6"
        :max-size-mb="5"
        @success="onUploadSuccess"
      />
    </el-card>

    <!-- 单文件 - 任意类型 (典型用法 2) -->
    <el-card shadow="never">
      <template #header>{{ t('file.demoSingleHeader') }}</template>
      <FileUploader
        v-model="singleFile"
        biz-type="demo_attach"
        accept="*"
        :limit="1"
        :max-size-mb="10"
      />
    </el-card>

    <!-- v-model 实时打印 (调试用) -->
    <el-card shadow="never">
      <template #header>{{ t('file.demoModelHeader') }}</template>
      <pre class="dump">{{ JSON.stringify({ gallery, singleFile }, null, 2) }}</pre>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import FileUploader from '@/components/FileUploader.vue'

const { t } = useI18n()

const gallery = ref([])
const singleFile = ref([])

function onUploadSuccess(file) {
  ElMessage.success(t('file.uploadSuccessTpl', { name: file.originalName, id: file.id }))
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.hdr { display: flex; align-items: center; gap: 10px; }
.intro {
  color: #606266;
  line-height: 1.7;
  font-size: 14px;
  margin: 0;
}
.intro code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #d63384;
  font-family: 'Consolas', monospace;
  font-size: 13px;
}
.dump {
  margin: 0;
  padding: 14px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 6px;
  font-size: 12px;
  max-height: 400px;
  overflow: auto;
  white-space: pre-wrap;
}
</style>
