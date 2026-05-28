// Sprint 35: fine-grained perm helpers.
//
// hasPerm('module:resource:action')      -> boolean
// hasAnyPerm(['p1', 'p2'])               -> boolean   (OR)
// hasAllPerms(['p1', 'p2'])              -> boolean   (AND)
//
// SUPER_ADMIN bypasses all checks (defence in depth - the backend also issues
// a token with every perm baked in, so this is mostly a UX nicety).
//
// Also wires the v-perm directive so templates can write:
//   <el-button v-perm="'system:user:add'">New user</el-button>
//   <el-button v-perm="['system:user:edit', 'system:user:delete']">...</el-button>

import { useAuthStore } from '@/stores/auth'

function permSet() {
  const auth = useAuthStore()
  if (auth.isSuperAdmin) return null // sentinel: bypass
  return new Set(auth.permissions || [])
}

export function hasPerm(p) {
  if (!p) return true
  const set = permSet()
  if (set === null) return true
  return set.has(p)
}

export function hasAnyPerm(list) {
  if (!list || list.length === 0) return true
  const set = permSet()
  if (set === null) return true
  return list.some(p => set.has(p))
}

export function hasAllPerms(list) {
  if (!list || list.length === 0) return true
  const set = permSet()
  if (set === null) return true
  return list.every(p => set.has(p))
}

/** Convenience for templates that don't want to import every time. */
export function installPerms(app) {
  app.config.globalProperties.$hasPerm = hasPerm
  app.config.globalProperties.$hasAnyPerm = hasAnyPerm
  app.config.globalProperties.$hasAllPerms = hasAllPerms

  app.directive('perm', {
    mounted(el, binding) { applyPerm(el, binding.value) },
    updated(el, binding) { applyPerm(el, binding.value) },
  })
}

function applyPerm(el, value) {
  let ok
  if (Array.isArray(value)) ok = hasAnyPerm(value)
  else ok = hasPerm(value)
  if (!ok) {
    // Remove from DOM rather than just hide so layout collapses cleanly.
    el.style.display = 'none'
    el.setAttribute('data-perm-hidden', '1')
  } else if (el.getAttribute('data-perm-hidden') === '1') {
    el.style.display = ''
    el.removeAttribute('data-perm-hidden')
  }
}
