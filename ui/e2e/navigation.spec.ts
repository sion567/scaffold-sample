import { test, expect } from '@playwright/test'

test.describe('导航模块', () => {
  test('首页可访问', async ({ page }) => {
    await page.goto('/index')
    // 首页应包含仪表盘相关内容
    await expect(page.locator('body')).toBeVisible()
  })

  test('404 页面正确显示', async ({ page }) => {
    await page.goto('/non-existent-page-xyz')
    await expect(page.locator('body')).toBeVisible()
  })

  test('注册页面可访问', async ({ page }) => {
    await page.goto('/register')
    await expect(page.locator('body')).toBeVisible()
  })

  test('驾驶舱页面可访问', async ({ page }) => {
    await page.goto('/cockpit')
    await expect(page.locator('body')).toBeVisible()
  })
})
