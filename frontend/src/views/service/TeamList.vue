<template>
  <StubPage
    :icon="UserFilled"
    :title="t('cs.teamStubTitle')"
    :description="t('cs.teamStubDesc')"
    sprint="Sprint 49"
    :callout="{
      title: t('cs.teamStubCalloutTitle'),
      body: t('cs.teamStubCalloutBody'),
      linkText: t('cs.teamStubCalloutLink'),
      linkTo: '/service/settings',
    }"
  />
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { UserFilled } from '@element-plus/icons-vue'
import StubPage from './stubs/StubPage.vue'

const { t } = useI18n()
</script>
