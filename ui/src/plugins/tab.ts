import type { LocationQueryRaw, RouteLocationRaw } from 'vue-router'
import useTagsViewStore from '@/store/modules/tagsView.ts'
import router from '@/router/index.ts'

/**
 * 页签对象：兼容 vue-router 的路由位置对象与 store 内部 TagView 两种形状
 */
interface TabViewParam {
  path?: string
  fullPath?: string
  name?: string | number | symbol | null
  query?: unknown
  meta?: unknown
}

export default {
  // 刷新当前tab页签
  refreshPage(obj?: TabViewParam) {
    const store = useTagsViewStore()
    const { path, query, matched } = router.currentRoute.value
    // 防止在重定向过程中重复刷新
    if (path.startsWith('/redirect/')) {
      return Promise.resolve()
    }
    if (obj === undefined) {
      matched.forEach((m) => {
        if (m.components && m.components.default && m.components.default) {
          const componentName = (m.components.default as { name?: string }).name
          if (componentName && !['Layout', 'ParentView'].includes(componentName)) {
            obj = { name: componentName, path: path, query: query }
          }
        }
      })
    }
    return store.delCachedView(obj as Parameters<typeof store.delCachedView>[0]).then(() => {
      const { path, query } = obj!
      router.replace({
        path: '/redirect' + path,
        query: query as LocationQueryRaw
      })
    })
  },
  // 关闭当前tab页签，打开新页签
  closeOpenPage(obj?: TabViewParam) {
    const store = useTagsViewStore()
    store.delView(router.currentRoute.value as Parameters<typeof store.delView>[0])
    if (obj !== undefined) {
      return router.push(obj as RouteLocationRaw)
    }
  },
  // 关闭指定tab页签
  closePage(obj?: TabViewParam) {
    const store = useTagsViewStore()
    if (obj === undefined) {
      return store.delView(router.currentRoute.value as Parameters<typeof store.delView>[0]).then(async (res) => {
        const latestView = res.visitedViews.slice(-1)[0]
        if (latestView) {
          await router.push(latestView.fullPath!)
        } else {
          await router.push('/')
        }
        return res
      })
    }
    return store.delView(obj as Parameters<typeof store.delView>[0])
  },
  // 关闭所有tab页签
  closeAllPage() {
    return useTagsViewStore().delAllViews()
  },
  // 关闭左侧tab页签
  closeLeftPage(obj?: TabViewParam) {
    const store = useTagsViewStore()
    return store.delLeftTags((obj || router.currentRoute.value) as Parameters<typeof store.delLeftTags>[0])
  },
  // 关闭右侧tab页签
  closeRightPage(obj?: TabViewParam) {
    const store = useTagsViewStore()
    return store.delRightTags((obj || router.currentRoute.value) as Parameters<typeof store.delRightTags>[0])
  },
  // 关闭其他tab页签
  closeOtherPage(obj?: TabViewParam) {
    const store = useTagsViewStore()
    return store.delOthersViews((obj || router.currentRoute.value) as Parameters<typeof store.delOthersViews>[0])
  },
  // 打开tab页签
  openPage(title: string, url: string, params?: LocationQueryRaw) {
    const store = useTagsViewStore()
    const obj = { path: url, meta: { title: title } }
    store.addView(obj as Parameters<typeof store.addView>[0])
    return router.push({ path: url, query: params })
  },
  // 修改tab页签
  updatePage(obj: TabViewParam) {
    const store = useTagsViewStore()
    return store.updateVisitedView(obj as Parameters<typeof store.updateVisitedView>[0])
  }
}
