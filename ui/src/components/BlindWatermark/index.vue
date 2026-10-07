<template>
  <div v-if="active" ref="wmRef" class="blind-watermark" :style="layerStyle"></div>
</template>

<script setup lang="ts">
/**
 * 盲水印层：肉眼近不可见（alpha 1/255），截图后通过反色/对比度拉伸/直方图增强
 * 可还原出操作人信息，用于公安项目敏感页面截图溯源。
 * 开关由 sys_config 参数 sys.watermark.enabled 控制（true/false），未配置或查询失败时默认开启。
 * 未登录（如登录页）不渲染也不请求开关；登录进入业务页后仅查询一次，登出后自动隐藏。
 */
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import useUserStore from '@/store/modules/user'
import { getToken } from '@/utils/auth'
import { getConfigKey } from '@/api/system/config'

const route = useRoute()

/** 服务端开关结果（查询失败/未配置时兜底为 true） */
const switchOn = ref(false)
/** 实际渲染 = 已登录 且 switchOn */
const active = ref(false)
const wmRef = ref<HTMLElement | null>(null)
const layerStyle = computed(() => ({
  backgroundImage: `url(${makeWatermarkImage()})`
}))

// alpha=1/255：屏幕与普通截图均不可察觉，Photoshop 曲线/反色后显形
const ALPHA = 1 / 255

function makeWatermarkImage(): string {
  const userStore = useUserStore()
  const now = new Date()
  const pad = (n: number): string => String(n).padStart(2, '0')
  const text = `${userStore.name || '未知用户'} ${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`

  const canvas = document.createElement('canvas')
  canvas.width = 300
  canvas.height = 220
  const ctx = canvas.getContext('2d')
  if (!ctx) return ''
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.save()
  ctx.translate(150, 110)
  ctx.rotate(-20 * Math.PI / 180)
  ctx.font = '16px Arial'
  ctx.textAlign = 'center'
  ctx.fillStyle = `rgba(0, 0, 0, ${ALPHA})`
  ctx.fillText(text, 0, 0)
  ctx.restore()
  return canvas.toDataURL('image/png')
}

// 防篡改：被 F12 删除或改样式时自动重建
let observer: MutationObserver | null = null
function guard() {
  if (!wmRef.value || typeof MutationObserver === 'undefined') return
  const currentObserver = new MutationObserver(() => {
    const el = wmRef.value
    if (!el) return
    if (!el.isConnected || el.style.display === 'none' || el.style.visibility === 'hidden') {
      currentObserver.disconnect()
      observer = null
      const parent = el.parentElement
      if (parent && !el.isConnected) {
        const clone = el.cloneNode(true)
        if (clone instanceof HTMLElement) {
          parent.appendChild(clone)
          wmRef.value = clone
        }
      } else {
        el.style.display = ''
        el.style.visibility = ''
      }
      guard()
    }
  })
  currentObserver.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class'] })
  observer = currentObserver
}

function stopGuard() {
  observer?.disconnect()
  observer = null
}

let checked = false
function applyState() {
  if (!getToken()) {
    // 未登录（登录页/注册页/被踢出）：不渲染、不请求；先停掉防篡改监听，避免与 v-if 移除互相拉扯
    stopGuard()
    active.value = false
    return
  }
  if (!checked) {
    checked = true
    // 查询失败/未配置时兜底开启
    getConfigKey('sys.watermark.enabled').then(res => {
      switchOn.value = res.msg !== 'false'
      applyState()
    }).catch(() => {
      switchOn.value = true
      applyState()
    })
  }
  active.value = switchOn.value
  if (active.value) {
    // 等 v-if 渲染出 wmRef 后再挂防篡改监听，否则 ref 为空会静默跳过
    nextTick(() => guard())
  } else {
    stopGuard()
  }
}

watch(() => route.path, applyState, { immediate: true })

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.blind-watermark {
  position: fixed;
  inset: 0;
  z-index: 2147483646;
  pointer-events: none;
  background-repeat: repeat;
}
</style>
