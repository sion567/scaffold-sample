import router from './router/index.ts'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth.ts'
import { isHttp, isPathMatch } from '@/utils/validate.ts'
import { isRelogin } from '@/utils/request.ts'
import useUserStore from '@/store/modules/user.ts'
import useLockStore from '@/store/modules/lock.ts'
import useSettingsStore from '@/store/modules/settings.ts'
import usePermissionStore from '@/store/modules/permission.ts'
import { ensureSignKeys } from '@/utils/gmEnvelope.ts'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path: string) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

router.beforeEach(async (to, from) => {
  NProgress.start()
  if (getToken()) {
    // 会话签名密钥（规范 V1.2）：登录后/刷新页面后确保已向后端签发，
    // 后端对带 token 的请求强制验签；已就绪时零开销
    try { await ensureSignKeys() } catch (e) { console.warn('[国密] 签名会话初始化失败', e) }
    const pageTitle = to.meta.title as string | undefined
    pageTitle && useSettingsStore().setTitle(pageTitle)
    const isLock = useLockStore().isLock
    if (to.path === '/login') {
      NProgress.done()
      return { path: '/' }
    }
    if (isWhiteList(to.path)) {
      return true
    }
    if (isLock && to.path !== '/lock') {
      NProgress.done()
      return { path: '/lock' }
    }
    if (!isLock && to.path === '/lock') {
      NProgress.done()
      return { path: '/' }
    }
    if (useUserStore().roles.length === 0) {
      isRelogin.show = true
      try {
        // 拉取user_info信息
        await useUserStore().getInfo()
        isRelogin.show = false
        // 根据roles权限生成可访问的路由
        const accessRoutes = await usePermissionStore().generateRoutes()
        accessRoutes.forEach(route => {
          if (!isHttp(route.path)) {
            // MenuRoute（component 已转换为组件对象）与 RouteRecordRaw 结构兼容
            router.addRoute(route as unknown as import('vue-router').RouteRecordRaw)
          }
        })
        // 重新导航到目标路由，确保动态路由已注册
        return { ...to, replace: true }
      } catch (err) {
        await useUserStore().logOut()
        ElMessage.error(err instanceof Error ? err.message : String(err))
        return { path: '/' }
      }
    }
    return true
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true
    }
    NProgress.done()
    return `/login?redirect=${to.fullPath}` // 否则全部重定向到登录页
  }
})

router.afterEach(() => {
  NProgress.done()
})
