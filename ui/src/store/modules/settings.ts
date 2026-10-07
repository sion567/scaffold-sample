import defaultSettings from '@/settings.ts'
import { useDark, useToggle } from '@vueuse/core'
import { useDynamicTitle } from '@/utils/dynamicTitle.ts'
import { handleThemeStyle } from '@/utils/theme.ts'

const isDark = useDark()
const toggleDark = useToggle(isDark)

/** 布局设置（对应 src/settings.ts 默认配置项） */
interface SettingsConfig {
  /** 网页标题 */
  title: string
  /** 主题颜色 */
  theme: string
  /** 侧边栏主题 theme-dark / theme-light */
  sideTheme: string
  /** 是否系统布局配置 */
  showSettings: boolean
  /** 菜单导航模式 1、纯左侧 2、混合（左侧+顶部） 3、纯顶部 */
  navType: number
  /** 是否显示 tagsView */
  tagsView: boolean
  /** 是否持久化标签页 */
  tagsViewPersist: boolean
  /** 是否显示页签图标 */
  tagsIcon: boolean
  /** 标签页样式：card 卡片（默认）、chrome 谷歌浏览器风格 */
  tagsViewStyle: string
  /** 是否固定头部 */
  fixedHeader: boolean
  /** 是否显示 logo */
  sidebarLogo: boolean
  /** 是否显示动态标题 */
  dynamicTitle: boolean
  /** 是否显示底部版权 */
  footerVisible: boolean
  /** 底部版权文本内容 */
  footerContent: string
  /** 是否暗黑模式 */
  isDark: boolean
}

const { sideTheme, showSettings, navType, tagsView, tagsViewPersist, tagsIcon, tagsViewStyle, fixedHeader, sidebarLogo, dynamicTitle, footerVisible, footerContent } = defaultSettings

/** localStorage 缓存的布局设置（layout-setting） */
interface StoredSettings {
  theme?: string
  sideTheme?: string
  navType?: number
  tagsView?: boolean
  tagsViewPersist?: boolean
  tagsIcon?: boolean
  tagsViewStyle?: string
  fixedHeader?: boolean
  sidebarLogo?: boolean
  dynamicTitle?: boolean
  footerVisible?: boolean
}

const storageSetting: StoredSettings = JSON.parse(localStorage.getItem('layout-setting') ?? '{}') || {}

const useSettingsStore = defineStore(
  'settings',
  {
    state: (): SettingsConfig => ({
      title: '',
      theme: storageSetting.theme || '#409EFF',
      sideTheme: storageSetting.sideTheme || sideTheme,
      showSettings: showSettings,
      navType: storageSetting.navType === undefined ? navType : storageSetting.navType,
      tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
      tagsViewPersist: storageSetting.tagsViewPersist === undefined ? tagsViewPersist : storageSetting.tagsViewPersist,
      tagsIcon: storageSetting.tagsIcon === undefined ? tagsIcon : storageSetting.tagsIcon,
      tagsViewStyle: storageSetting.tagsViewStyle === undefined ? tagsViewStyle : storageSetting.tagsViewStyle,
      fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
      sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
      dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
      footerVisible: storageSetting.footerVisible === undefined ? footerVisible : storageSetting.footerVisible,
      footerContent: footerContent,
      isDark: isDark.value
    }),
    actions: {
      // 修改布局设置
      changeSetting<K extends keyof SettingsConfig>(data: { key: K; value: SettingsConfig[K] }) {
        const { key, value } = data
        if (this.hasOwnProperty(key)) {
          (this.$state as SettingsConfig)[key] = value
        }
      },
      // 设置网页标题
      setTitle(title: string) {
        this.title = title
        useDynamicTitle()
      },
      // 切换暗黑模式
      toggleTheme() {
        this.isDark = !this.isDark
        toggleDark()
        nextTick(() => {
          handleThemeStyle(this.theme)
        })
      }
    }
  })

export default useSettingsStore
