import auth from '@/plugins/auth.ts'
import router, { constantRoutes, dynamicRoutes } from '@/router/index.ts'
import { getRouters } from '@/api/menu.ts'
import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'
import ParentView from '@/components/ParentView/index.vue'
import InnerLink from '@/layout/components/InnerLink/index.vue'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue')

/** 路由 meta 信息（对应后端 MetaVo 及前端扩展字段） */
export interface MenuMeta {
  /** 菜单标题 */
  title?: string
  /** 菜单图标 */
  icon?: string
  /** 设置为 true 则不会被 keep-alive 缓存 */
  noCache?: boolean
  /** 外链地址（如 https://www.baidu.com） */
  link?: string | null
  /** 高亮菜单路径 */
  activeMenu?: string
  /** 是否在面包屑中显示 */
  breadcrumb?: boolean
  /** 页签是否固定 */
  affix?: boolean
  /** 其余动态扩展字段 */
  [key: string]: unknown
}

/** 后端菜单路由（对应 RouterVo；component 经 filterAsyncRouter 由字符串转换为组件对象） */
export interface MenuRoute {
  /** 路由名称 */
  name?: string
  /** 路由地址 */
  path: string
  /** 是否在侧边栏隐藏 */
  hidden?: boolean
  /** 重定向地址 */
  redirect?: string
  /** 组件：后端返回字符串，filterAsyncRouter 转换后为组件对象 */
  component?: string | Component
  /** 默认传参（JSON 字符串） */
  query?: string
  /** 一直显示根路由 */
  alwaysShow?: boolean
  /** 路由 meta 信息 */
  meta?: MenuMeta
  /** 子路由 */
  children?: MenuRoute[]
  /** 菜单权限字符（前端 dynamicRoutes 使用） */
  permissions?: string[]
  /** 角色权限字符（前端 dynamicRoutes 使用） */
  roles?: string[]
}

// 路由常量含 hidden 等扩展字段（未标注 RouteRecordRaw），统一收窄为 MenuRoute 便于与后端路由拼接
// 注意：本模块与 @/router/index.ts 存在循环依赖（router → Layout → 布局组件 → 本 store），
// 模块初始化阶段 constantRoutes 尚未赋值，必须延迟到函数调用时再读取
const constantRouteList = () => constantRoutes as MenuRoute[]

const usePermissionStore = defineStore(
  'permission',
  {
    state: (): {
      routes: MenuRoute[]
      addRoutes: MenuRoute[]
      defaultRoutes: MenuRoute[]
      topbarRouters: MenuRoute[]
      sidebarRouters: MenuRoute[]
    } => ({
      routes: [],
      addRoutes: [],
      defaultRoutes: [],
      topbarRouters: [],
      sidebarRouters: []
    }),
    actions: {
      setRoutes(routes: MenuRoute[]) {
        this.addRoutes = routes
        this.routes = constantRouteList().concat(routes)
      },
      setDefaultRoutes(routes: MenuRoute[]) {
        this.defaultRoutes = constantRouteList().concat(routes)
      },
      setTopbarRoutes(routes: MenuRoute[]) {
        this.topbarRouters = routes
      },
      setSidebarRouters(routes: MenuRoute[]) {
        this.sidebarRouters = routes
      },
      generateRoutes(roles?: string[]): Promise<MenuRoute[]> {
        return new Promise(resolve => {
          // 向后端请求路由数据
          getRouters().then(res => {
            const sdata = JSON.parse(JSON.stringify(res.data))
            const rdata = JSON.parse(JSON.stringify(res.data))
            const defaultData = JSON.parse(JSON.stringify(res.data))
            const sidebarRoutes = filterAsyncRouter(sdata)
            const rewriteRoutes = filterAsyncRouter(rdata, false, true)
            const defaultRoutes = filterAsyncRouter(defaultData)
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            // MenuRoute 的 component 可能为字符串，经 filterAsyncRouter 转换后均为组件对象
            asyncRoutes.forEach(route => { router.addRoute(route as unknown as RouteRecordRaw) })
            this.setRoutes(rewriteRoutes)
            this.setSidebarRouters(constantRouteList().concat(sidebarRoutes))
            this.setDefaultRoutes(sidebarRoutes)
            this.setTopbarRoutes(defaultRoutes)
            resolve(rewriteRoutes)
          })
        })
      }
    }
  })

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap: MenuRoute[], lastRouter: MenuRoute | false = false, type = false): MenuRoute[] {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      const component = route.component
      if (component === 'Layout') {
        route.component = Layout
      } else if (component === 'ParentView') {
        route.component = ParentView
      } else if (component === 'InnerLink') {
        route.component = InnerLink
      } else {
        // 其余为视图路径字符串
        route.component = loadView(component as string)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap: MenuRoute[], lastRouter: MenuRoute | false = false): MenuRoute[] {
  let children: MenuRoute[] = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes: MenuRoute[]): MenuRoute[] {
  const res: MenuRoute[] = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

export const loadView = (view: string): (() => Promise<unknown>) | undefined => {
  let res: (() => Promise<unknown>) | undefined
  for (const path in modules) {
    const dir = path.split('views/')[1].split('.vue')[0]
    if (dir === view) {
      res = () => modules[path]()
    }
  }
  return res
}

export default usePermissionStore
