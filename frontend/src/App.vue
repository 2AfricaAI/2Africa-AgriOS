<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElConfigProvider } from 'element-plus'
import { getElementLocale } from '@/i18n'

const { locale } = useI18n()
const elementLocale = computed(() => getElementLocale(locale.value))
</script>
