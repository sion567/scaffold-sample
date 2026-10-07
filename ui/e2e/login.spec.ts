import { test, expect } from '@playwright/test'

test.describe('登录模块', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test('页面标题正确', async ({ page }) => {
    await expect(page).toHaveTitle(/脚手架管理系统/)
  })

  test('登录表单元素存在', async ({ page }) => {
    await expect(page.getByPlaceholder('账号')).toBeVisible()
    await expect(page.getByPlaceholder('密码')).toBeVisible()
    await expect(page.getByRole('button', { name: /登 录/ })).toBeVisible()
  })

  test('默认账号密码填充', async ({ page }) => {
    const username = page.getByPlaceholder('账号')
    const password = page.getByPlaceholder('密码')
    await expect(username).toHaveValue('admin')
    await expect(password).toHaveValue('admin123')
  })

  test('空用户名提交显示验证错误', async ({ page }) => {
    const username = page.getByPlaceholder('账号')
    await username.fill('')
    await page.getByRole('button', { name: /登 录/ }).click()
    await expect(page.getByText('请输入您的账号')).toBeVisible()
  })

  test('空密码提交显示验证错误', async ({ page }) => {
    const password = page.getByPlaceholder('密码')
    await password.fill('')
    await page.getByRole('button', { name: /登 录/ }).click()
    await expect(page.getByText('请输入您的密码')).toBeVisible()
  })
})
