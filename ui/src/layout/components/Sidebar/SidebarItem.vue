<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path, onlyOneChild.query)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{ 'submenu-title-noDropdown': !isNest }">
          <svg-icon :icon-class="(onlyOneChild.meta.icon || (item.meta && item.meta.icon)) as string"/>
          <template #title><span class="menu-title" :title="hasTitle(onlyOneChild.meta.title)">{{ onlyOneChild.meta.title }}</span></template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" teleported>
      <template v-if="item.meta" #title>
        <svg-icon :icon-class="(item.meta && item.meta.icon) as string" />
        <span class="menu-title" :title="hasTitle(item.meta.title)">{{ item.meta.title }}</span>
      </template>

      <sidebar-item
        v-for="(child, index) in item.children"
        :key="child.path + index"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup lang="ts">
import { isExternal } from '@/utils/validate'
import AppLink from './Link.vue'
import { getNormalPath } from '@/utils/ct'

/** 菜单路由 meta 的本地投影（与 @/store/modules/permission 内部 MenuMeta 保持结构兼容） */
type MenuMeta = {
  title?: string
  /** 菜单图标（实际路由可能缺失，按空串兜底渲染） */
  icon?: string
  noCache?: boolean
  affix?: boolean
  [key: string]: unknown
}

/** 菜单路由节点的本地投影 */
interface MenuItem {
  hidden?: boolean
  alwaysShow?: boolean
  path: string
  name?: string
  /** 路由查询参数（JSON 字符串） */
  query?: string
  redirect?: string
  noShowingChildren?: boolean
  meta?: MenuMeta
  children?: MenuItem[]
}

interface Props {
  // route object
  item: MenuItem
  isNest?: boolean
  basePath?: string
}

const props = withDefaults(defineProps<Props>(), {
  isNest: false,
  basePath: ''
})

const onlyOneChild = ref<MenuItem>({ path: '', meta: { icon: '' } })

function hasOneShowingChild(children: MenuItem[] = [], parent?: MenuItem) {
  if (!children) {
    children = []
  }
  const showingChildren = children.filter(item => {
    if (item.hidden) {
      return false
    }
    onlyOneChild.value = item
    return true
  })

  // When there is only one child router, the child router is displayed by default
  if (showingChildren.length === 1) {
    return true
  }

  // Show parent if there are no child router to display
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
    return true
  }

  return false
}

function resolvePath(routePath: string): string
function resolvePath(routePath: string, routeQuery?: string): string | { path: string; query: Record<string, unknown> }
function resolvePath(routePath: string, routeQuery?: string): string | { path: string; query: Record<string, unknown> } {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(props.basePath)) {
    return props.basePath
  }
  if (routeQuery) {
    let query = JSON.parse(routeQuery)
    return { path: getNormalPath(props.basePath + '/' + routePath), query: query }
  }
  return getNormalPath(props.basePath + '/' + routePath)
}

function hasTitle(title?: string) {
  if (title && title.length > 5) {
    return title
  } else {
    return ""
  }
}
</script>
