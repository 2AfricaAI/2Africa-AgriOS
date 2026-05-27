/**
 * Offline submit queue (Sprint 20.7)
 *
 * Persists failed mobile submits to IndexedDB and replays them when network returns.
 * Photo files are stored as native Blobs (IndexedDB supports this directly).
 *
 * Queue item shape:
 *   {
 *     id          : auto-increment PK
 *     kind        : 'activity' | 'harvest'
 *     form        : { ...JSON form, EXCLUDING photos:[] (separate field) }
 *     photoBlobs  : Array<{ name, type, blob }>     -- original File objects, repackaged
 *     createdAt   : ISO timestamp
 *     retries     : N (incremented on each failed flush)
 *     lastError   : last error message
 *   }
 */
import { openDB } from 'idb'
import { uploadFile } from '@/api/file'
import { createActivity } from '@/api/activity'
import { createHarvest } from '@/api/harvest'

const DB_NAME    = 'agrios-mobile'
const DB_VERSION = 1
const STORE      = 'pendingSubmits'

let _dbPromise = null
function db() {
  if (!_dbPromise) {
    _dbPromise = openDB(DB_NAME, DB_VERSION, {
      upgrade(d) {
        if (!d.objectStoreNames.contains(STORE)) {
          d.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true })
        }
      },
    })
  }
  return _dbPromise
}

/** Convert File[] -> serialisable photoBlobs[] suitable for IndexedDB */
function packPhotos(files) {
  return (files || []).map(f => ({
    name: f.name || 'photo.jpg',
    type: f.type || 'image/jpeg',
    blob: f,                    // File extends Blob, idb handles it
  }))
}

/** Add a failed submit to the queue. Returns the new id. */
export async function enqueue({ kind, form, photoFiles }) {
  const item = {
    kind,
    form,
    photoBlobs: packPhotos(photoFiles),
    createdAt: new Date().toISOString(),
    retries: 0,
    lastError: '',
  }
  const conn = await db()
  return conn.add(STORE, item)
}

/** Count pending items - used for badge in top bar */
export async function countPending() {
  const conn = await db()
  return conn.count(STORE)
}

/** List all pending items (for debug / pending-detail page) */
export async function listPending() {
  const conn = await db()
  return conn.getAll(STORE)
}

/** Re-submit one item. Returns true on success. */
async function resubmit(item) {
  // 1) Re-upload photo blobs (server-side files were never created)
  const fileIds = []
  for (const p of item.photoBlobs || []) {
    // Reconstruct a File so backend gets a sensible filename
    const file = new File([p.blob], p.name, { type: p.type })
    const tag = item.kind === 'activity' ? 'activity_photo' : 'harvest_photo'
    const res = await uploadFile(file, tag)
    if (res?.id) fileIds.push(res.id)
  }
  // 2) Post the form with fresh photo ids
  const payload = { ...item.form, photos: fileIds }
  if (item.kind === 'activity') await createActivity(payload)
  else if (item.kind === 'harvest') await createHarvest(payload)
  else throw new Error('Unknown kind: ' + item.kind)
  return true
}

/**
 * Flush the queue (called on 'online' event or manually).
 * Returns { ok: N, failed: M }.
 * Stops on first network failure (assume still offline), keeps going on logic errors.
 */
export async function flush() {
  const conn = await db()
  const all = await conn.getAll(STORE)
  let ok = 0, failed = 0
  for (const item of all) {
    try {
      await resubmit(item)
      await conn.delete(STORE, item.id)
      ok++
    } catch (e) {
      failed++
      item.retries = (item.retries || 0) + 1
      item.lastError = e?.message || String(e)
      await conn.put(STORE, item)
      // If it's a network error, stop trying further items - we're probably offline again
      if (isNetworkError(e)) break
    }
  }
  return { ok, failed }
}

function isNetworkError(e) {
  if (!e) return false
  const msg = (e.message || '').toLowerCase()
  return msg.includes('network') || msg.includes('failed to fetch') ||
         msg.includes('timeout')  || e.code === 'ERR_NETWORK'
}
