<template>
  <!--
    Sprint 50d -- public CSAT survey page. Sits OUTSIDE the auth wall
    (route flagged meta.public = true). One job: collect a 1-5 star
    rating + optional comment, post it to the backend with the URL
    token, then say thanks.

    Token validation is server-side; we just relay the token from the
    URL into the POST body. On a 4xx (expired / already submitted /
    invalid) we show an inline error rather than the form.
  -->
  <div class="csat-page">
    <header class="csat-brand">
      <img src="/logo.svg" alt="2Africa.AI AgriOS" class="csat-logo" />
    </header>

    <main class="csat-card" v-loading="loading">
      <!-- ===== State A: ready to submit ===== -->
      <template v-if="state === 'ready'">
        <h1 class="csat-title">{{ t('csat.title') }}</h1>
        <p class="csat-sub">{{ t('csat.subtitle') }}</p>

        <div class="csat-stars">
          <button
            v-for="n in 5"
            :key="n"
            type="button"
            class="csat-star"
            :class="{ active: rating >= n, hover: hoverRating >= n }"
            @click="rating = n"
            @mouseenter="hoverRating = n"
            @mouseleave="hoverRating = 0"
            :aria-label="t('csat.rateN', { n })"
          >★</button>
        </div>
        <div class="csat-stars-label">
          {{ rating > 0 ? t('csat.ratingLabel.' + rating) : t('csat.pickAStar') }}
        </div>

        <label class="csat-comment-label">{{ t('csat.commentOptional') }}</label>
        <textarea
          v-model="comment"
          class="csat-comment"
          maxlength="2000"
          rows="4"
          :placeholder="t('csat.commentPlaceholder')"
        ></textarea>

        <button
          type="button"
          class="csat-submit"
          :disabled="rating === 0 || submitting"
          @click="onSubmit"
        >
          {{ submitting ? t('csat.submitting') : t('csat.submit') }}
        </button>
      </template>

      <!-- ===== State B: thank you ===== -->
      <template v-else-if="state === 'thanks'">
        <div class="csat-thanks-icon">✓</div>
        <h1 class="csat-title">{{ t('csat.thanksTitle') }}</h1>
        <p class="csat-sub">{{ t('csat.thanksBody', { n: submittedRating }) }}</p>
      </template>

      <!-- ===== State C: error (expired / used / invalid) ===== -->
      <template v-else-if="state === 'error'">
        <div class="csat-error-icon">!</div>
        <h1 class="csat-title">{{ t('csat.errorTitle') }}</h1>
        <p class="csat-sub">{{ errorMsg }}</p>
      </template>
    </main>

    <footer class="csat-foot">
      <span>{{ t('csat.footer') }}</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'

const { t } = useI18n()
const route = useRoute()

// 'loading' before probe, then 'ready' / 'thanks' / 'error'
const state = ref('loading')
const loading = ref(true)
const submitting = ref(false)

const rating = ref(0)
const hoverRating = ref(0)
const comment = ref('')
const submittedRating = ref(null)
const errorMsg = ref('')

async function probe() {
  loading.value = true
  try {
    await request.get(`/v1/cs/csat/public/${route.params.token}`)
    state.value = 'ready'
  } catch (err) {
    state.value = 'error'
    errorMsg.value = err?.message || t('csat.errorGeneric')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (rating.value === 0 || submitting.value) return
  submitting.value = true
  try {
    await request.post(`/v1/cs/csat/public/${route.params.token}`, {
      rating: rating.value,
      comment: comment.value || null,
    })
    submittedRating.value = rating.value
    state.value = 'thanks'
  } catch (err) {
    errorMsg.value = err?.message || t('csat.submitFailed')
    state.value = 'error'
  } finally {
    submitting.value = false
  }
}

onMounted(probe)
</script>

<style scoped>
.csat-page {
  min-height: 100vh;
  background: #f4f7f5;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 16px;
}
.csat-brand { margin-bottom: 16px; }
.csat-logo { height: 36px; }
.csat-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e6ece9;
  box-shadow: 0 8px 24px rgba(15, 58, 38, 0.06);
  padding: 32px 28px;
  width: 100%;
  max-width: 460px;
  text-align: center;
}
.csat-title {
  margin: 0 0 6px;
  font-size: 22px;
  color: #0f3a26;
}
.csat-sub {
  margin: 0 0 22px;
  color: #5b6b62;
  font-size: 14px;
  line-height: 1.5;
}
.csat-stars {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-bottom: 6px;
}
.csat-star {
  font-size: 44px;
  line-height: 1;
  background: none;
  border: 0;
  cursor: pointer;
  color: #d6dcd8;
  transition: color 0.12s ease, transform 0.08s ease;
  padding: 4px;
}
.csat-star.active,
.csat-star.hover { color: #f5b400; }
.csat-star:hover { transform: scale(1.08); }
.csat-stars-label {
  font-size: 13px;
  color: #5b6b62;
  margin-bottom: 16px;
  min-height: 18px;
}
.csat-comment-label {
  display: block;
  text-align: left;
  font-size: 12px;
  color: #5b6b62;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  margin-bottom: 6px;
}
.csat-comment {
  width: 100%;
  border: 1px solid #cbd5d0;
  border-radius: 8px;
  padding: 10px 12px;
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
  margin-bottom: 20px;
  box-sizing: border-box;
}
.csat-comment:focus {
  outline: none;
  border-color: #0f3a26;
}
.csat-submit {
  width: 100%;
  padding: 12px 20px;
  background: #0f3a26;
  color: #fff;
  border: 0;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.12s ease;
}
.csat-submit:hover:not(:disabled) { background: #163f2c; }
.csat-submit:disabled {
  background: #b9c4be;
  cursor: not-allowed;
}
.csat-thanks-icon,
.csat-error-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #fff;
  font-weight: 700;
}
.csat-thanks-icon { background: #2BA84A; }
.csat-error-icon  { background: #c45a4d; }
.csat-foot {
  margin-top: 28px;
  color: #8a9690;
  font-size: 12px;
}
</style>
