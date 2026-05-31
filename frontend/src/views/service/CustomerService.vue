<template>
  <div class="service-page">
    <!-- Banner -->
    <div class="banner">
      <div class="banner-text">
        <el-icon class="banner-icon"><ChatRoundIcon /></el-icon>
        <span class="banner-title">{{ t('service.title') }}</span>
        <el-tag size="small" type="info" effect="plain">{{ t('service.poweredBy') }}</el-tag>
      </div>
      <div class="banner-actions">
        <el-button text @click="reloadFrame">
          <el-icon><RefreshIcon /></el-icon>
          {{ t('service.reload') }}
        </el-button>
        <el-button type="primary" @click="openInNewTab">
          <el-icon><LinkIcon /></el-icon>
          {{ t('service.openInNewTab') }}
        </el-button>
      </div>
    </div>

    <!-- Embedded Chatwoot. If X-Frame-Options blocks it the iframe will be empty;
         users still have the "Open in new tab" CTA in the banner. -->
    <div class="frame-wrap">
      <iframe
        ref="frameRef"
        :src="chatwootUrl"
        class="frame"
        :title="t('service.title')"
        loading="lazy"
        allow="clipboard-read; clipboard-write; microphone; camera; autoplay"
      />
      <!-- Subtle fallback hint shown only after a delay, in case the iframe
           never paints (X-Frame-Options / connection issue). -->
      <transition name="fade">
        <div v-if="showHint" class="hint">
          <el-alert
            :title="t('service.iframeHintTitle')"
            type="info"
            show-icon
            :closable="false"
          >
            <template #default>
              <p>{{ t('service.iframeHintBody') }}</p>
              <el-button type="primary" size="small" @click="openInNewTab">
                {{ t('service.openInNewTab') }}
              </el-button>
            </template>
          </el-alert>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ChatRound as ChatRoundIcon,
  Refresh as RefreshIcon,
  Link as LinkIcon,
} from '@element-plus/icons-vue'

const { t } = useI18n()

/**
 * Chatwoot base URL.
 *
 * In dev, the user's Chatwoot stack runs in the local docker-compose on
 * http://localhost:3000. Operators that deploy AgriOS behind a different
 * domain can override via the Vite-time env var VITE_CHATWOOT_URL.
 *
 * We deliberately read import.meta.env so this falls back gracefully when
 * the env var is unset (default 90% case for Albert's Farm dev).
 */
const chatwootUrl = import.meta.env.VITE_CHATWOOT_URL || 'http://localhost:3000'

const frameRef = ref(null)
const showHint = ref(false)
let hintTimer = null

onMounted(() => {
  // Give the iframe ~4s to paint. If X-Frame-Options blocks it the iframe
  // will silently stay blank and the cross-origin `load` event still fires
  // — so we use a wall clock as the only reliable cue and just suggest the
  // fallback as a non-blocking hint.
  hintTimer = setTimeout(() => { showHint.value = true }, 4000)
})

onBeforeUnmount(() => {
  if (hintTimer) clearTimeout(hintTimer)
})

function reloadFrame() {
  if (frameRef.value) {
    // Force a fresh load — bust any stale cookie/login state.
    // eslint-disable-next-line no-self-assign
    frameRef.value.src = frameRef.value.src
  }
  showHint.value = false
  if (hintTimer) clearTimeout(hintTimer)
  hintTimer = setTimeout(() => { showHint.value = true }, 4000)
}

function openInNewTab() {
  window.open(chatwootUrl, '_blank', 'noopener')
}
</script>

<style scoped>
.service-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px); /* topbar + main padding budget */
  gap: 12px;
}

.banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #f4f6f9;
  border: 1px solid #e6e8eb;
  border-radius: 8px;
}

.banner-text {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.banner-icon {
  color: #0f3a26;
  font-size: 18px;
}

.banner-title {
  font-size: 15px;
}

.banner-actions {
  display: flex;
  gap: 8px;
}

.frame-wrap {
  position: relative;
  flex: 1;
  min-height: 500px;
  border: 1px solid #e6e8eb;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}

.hint {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  width: min(560px, 90%);
  z-index: 2;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
