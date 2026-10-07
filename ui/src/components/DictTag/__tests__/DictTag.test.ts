import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import type { DictDataOption } from '@/types/api'

// Mock element-plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElTag: {
      name: 'ElTag',
      template: '<span class="el-tag"><slot /></span>',
      props: ['type', 'disable-transitions', 'class', 'index']
    }
  }
})

describe('DictTag 组件', () => {
  const defaultOptions: DictDataOption[] = [
    { value: '0', label: '正常', elTagType: 'success' },
    { value: '1', label: '停用', elTagType: 'danger' },
    { value: '2', label: '冻结', elTagType: 'warning' }
  ]

  describe('基础渲染', () => {
    it('渲染单个匹配值', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: '0'
        }
      })

      await nextTick()
      expect(wrapper.text()).toContain('正常')
    })

    it('渲染多个匹配值', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: ['0', '1']
        }
      })

      await nextTick()
      expect(wrapper.text()).toContain('正常')
      expect(wrapper.text()).toContain('停用')
    })
  })

  describe('值匹配逻辑', () => {
    it('字符串数字匹配', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: '0'
        }
      })

      await nextTick()
      expect(wrapper.text()).toContain('正常')
    })

    it('数字匹配', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: 0
        }
      })

      await nextTick()
      expect(wrapper.text()).toContain('正常')
    })

    it('逗号分隔字符串匹配', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: '0,1'
        }
      })

      await nextTick()
      expect(wrapper.text()).toContain('正常')
      expect(wrapper.text()).toContain('停用')
    })

    it('自定义分隔符', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: '0;1',
          separator: ';'
        }
      })

      await nextTick()
      expect(wrapper.text()).toContain('正常')
      expect(wrapper.text()).toContain('停用')
    })
  })

  describe('未匹配值显示', () => {
    it('显示未匹配值', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: '0,999'
        }
      })

      await nextTick()
      expect(wrapper.text()).toContain('正常')
      expect(wrapper.text()).toContain('999')
    })

    it('隐藏未匹配值', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: '0,999',
          showValue: false
        }
      })

      await nextTick()
      expect(wrapper.text()).toContain('正常')
      expect(wrapper.text()).not.toContain('999')
    })
  })

  describe('边界情况', () => {
    it('空值不显示', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: ''
        }
      })

      await nextTick()
      expect(wrapper.text().trim()).toBe('')
    })

    it('null 值不显示', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: null
        }
      })

      await nextTick()
      expect(wrapper.text().trim()).toBe('')
    })

    it('undefined 值不显示', async () => {
      const DictTag = (await import('@/components/DictTag/index.vue')).default
      const wrapper = mount(DictTag, {
        props: {
          options: defaultOptions,
          value: undefined
        }
      })

      await nextTick()
      expect(wrapper.text().trim()).toBe('')
    })
  })
})
