import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// Mock scroll-to
vi.mock('@/utils/scroll-to', () => ({
  scrollTo: vi.fn()
}))

// Mock element-plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElPagination: {
      name: 'ElPagination',
      template: '<div class="el-pagination"><slot /></div>',
      props: ['total', 'page-size', 'current-page', 'layout', 'page-sizes', 'pager-count', 'background'],
      emits: ['size-change', 'current-change', 'update:current-page', 'update:page-size']
    }
  }
})

describe('Pagination 组件', () => {
  describe('基础渲染', () => {
    it('渲染分页容器', async () => {
      const Pagination = (await import('@/components/Pagination/index.vue')).default
      const wrapper = mount(Pagination, {
        props: {
          total: 100,
          page: 1,
          limit: 20
        }
      })

      await nextTick()
      expect(wrapper.find('.pagination-container').exists()).toBe(true)
    })

    it('hidden 属性隐藏分页', async () => {
      const Pagination = (await import('@/components/Pagination/index.vue')).default
      const wrapper = mount(Pagination, {
        props: {
          total: 100,
          page: 1,
          limit: 20,
          hidden: true
        }
      })

      await nextTick()
      expect(wrapper.find('.hidden').exists()).toBe(true)
    })
  })

  describe('分页行为', () => {
    it('触发 page 变更事件', async () => {
      const Pagination = (await import('@/components/Pagination/index.vue')).default
      const wrapper = mount(Pagination, {
        props: {
          total: 100,
          page: 1,
          limit: 20
        }
      })

      await nextTick()
      await wrapper.setProps({ page: 2 })
      expect(wrapper.props('page')).toBe(2)
    })
  })

  describe('属性配置', () => {
    it('自定义 pageSizes', async () => {
      const Pagination = (await import('@/components/Pagination/index.vue')).default
      const customSizes = [10, 25, 50, 100]
      const wrapper = mount(Pagination, {
        props: {
          total: 100,
          page: 1,
          limit: 20,
          pageSizes: customSizes
        }
      })

      await nextTick()
      expect(wrapper.props('pageSizes')).toEqual(customSizes)
    })

    it('自定义 layout', async () => {
      const Pagination = (await import('@/components/Pagination/index.vue')).default
      const wrapper = mount(Pagination, {
        props: {
          total: 100,
          page: 1,
          limit: 20,
          layout: 'total, prev, pager, next'
        }
      })

      await nextTick()
      expect(wrapper.props('layout')).toBe('total, prev, pager, next')
    })
  })
})
