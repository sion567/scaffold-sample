<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
        <span v-if="item.redirect === 'noRedirect' || index == levelList.length - 1" class="no-redirect">{{ item.meta.title }}</span>
        <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import type { MenuRoute } from '@/store/modules/permission'
import usePermissionStore from '@/store/modules/permission'

/** 面包屑 meta 投影（兼容 RouteMeta / MenuMeta，字段在模板中按真值使用） */
type CrumbMeta = {
  title?: unknown
  breadcrumb?: unknown
  [key: string]: unknown
}

/** 面包屑层级项（过滤后必含 meta） */
interface BreadcrumbItem {
  path: string
  name?: string | symbol
  redirect?: unknown
  meta: CrumbMeta
}

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const levelList = ref<BreadcrumbItem[]>([])

function getBreadcrumb() {
  // only show routes with meta.title
  let matched: BreadcrumbItem[] = []
  const pathNum = findPathNum(route.path)
  // multi-level menu
  if (pathNum > 2) {
    const reg = /\/\w+/gi
    const pathList = (route.path.match(reg) || []).map((item, index) => {
      if (index !== 0) item = item.slice(1)
      return item
    })
    getMatched(pathList, permissionStore.defaultRoutes, matched)
  } else {
    matched = route.matched
      .filter((item) => item.meta && item.meta.title)
      .map(item => ({ path: item.path, name: item.name, redirect: item.redirect, meta: item.meta }))
  }
  // 判断是否为首页
  if (!isDashboard(matched[0])) {
    matched = [{ path: "/index", meta: { title: "首页" } }, ...matched]
  }
  levelList.value = matched.filter(item => item.meta && item.meta.title && item.meta.breadcrumb !== false)
}
function findPathNum(str: string, char = "/") {
  let index = str.indexOf(char)
  let num = 0
  while (index !== -1) {
    num++
    index = str.indexOf(char, index + 1)
  }
  return num
}
function getMatched(pathList: string[], routeList: MenuRoute[], matched: BreadcrumbItem[]) {
  let data = routeList.find(item => item.path == pathList[0] || String(item.name ?? '').toLowerCase() == pathList[0])
  if (data) {
    matched.push({ path: data.path, name: data.name, redirect: data.redirect, meta: data.meta ?? {} })
    if (data.children && pathList.length) {
      pathList.shift()
      getMatched(pathList, data.children, matched)
    }
  }
}
function isDashboard(route?: BreadcrumbItem) {
  const name = route && route.name
  if (!name) {
    return false
  }
  return String(name).trim() === 'Index'
}
function handleLink(item: BreadcrumbItem) {
  const { redirect, path } = item
  if (redirect) {
    router.push(redirect as string)
    return
  }
  router.push(path)
}

watchEffect(() => {
  // if you go to the redirect page, do not update the breadcrumbs
  if (route.path.startsWith('/redirect/')) {
    return
  }
  getBreadcrumb()
})
getBreadcrumb()
</script>

<style lang='scss' scoped>
.app-breadcrumb.el-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 50px;

  .no-redirect {
    color: #97a8be;
    cursor: text;
  }
}
</style>