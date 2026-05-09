const { test, expect } = require('@playwright/test');

async function login(page, username, password) {
  await page.goto('/login');
  await expect(page.getByPlaceholder('用户名')).toBeVisible();
  await page.getByPlaceholder('用户名').fill(username);
  await page.getByPlaceholder('密码').fill(password);
  await page.locator('.ant-tabs-tabpane-active button.ant-btn-primary').click();
}

test.describe('在线书店管理员与统计 E2E 黑盒测试', () => {
  test('管理员登录后可以访问订单管理页面', async ({ page }) => {
    await login(page, 'admin', '123456');
    await page.waitForURL('**/');

    await Promise.all([
      page.waitForResponse((response) =>
        response.url().includes('/api/orders/admin/all') && response.status() === 200
      ),
      page.goto('/admin/orders')
    ]);

    await expect(page).toHaveURL(/\/admin\/orders$/);
    await expect(page.locator('.ant-card').first()).toBeVisible();
  });

  test('管理员可以访问图书管理页并加载图书列表', async ({ page }) => {
    await login(page, 'admin', '123456');
    await page.waitForURL('**/');

    await Promise.all([
      page.waitForResponse((response) =>
        response.url().includes('/api/admin/books?page=0&size=10') && response.status() === 200
      ),
      page.goto('/admin/books')
    ]);

    await expect(page).toHaveURL(/\/admin\/books$/);
    await expect(page.getByRole('heading', { name: '书籍管理' })).toBeVisible();
    await expect(page.locator('.ant-table').first()).toBeVisible();
  });

  test('管理员可以查询书籍销量统计', async ({ page }) => {
    await login(page, 'admin', '123456');
    await page.waitForURL('**/');
    await page.goto('/admin/statistics');

    await Promise.all([
      page.waitForResponse((response) =>
        response.url().includes('/api/statistics/books') && response.status() === 200
      ),
      page.locator('button.ant-btn-primary').first().click()
    ]);

    await expect(page).toHaveURL(/\/admin\/statistics$/);
    await expect(page.locator('.ant-table')).toBeVisible();
  });

  test('管理员可以切换到用户消费统计并发起查询', async ({ page }) => {
    await login(page, 'admin', '123456');
    await page.waitForURL('**/');
    await page.goto('/admin/statistics');
    await page.locator('.ant-tabs-tab').nth(1).click();

    await Promise.all([
      page.waitForResponse((response) =>
        response.url().includes('/api/statistics/users') && response.status() === 200
      ),
      page.locator('button.ant-btn-primary').first().click()
    ]);

    await expect(page.locator('.ant-tabs-tabpane-active .ant-table').first()).toBeVisible();
  });

  test('普通用户可以查询个人购书统计', async ({ page }) => {
    await login(page, 'coco', '123456');
    await page.waitForURL('**/');
    await page.goto('/statistics');

    await Promise.all([
      page.waitForResponse((response) =>
        response.url().includes('/api/statistics/personal') && response.status() === 200
      ),
      page.locator('button.ant-btn-primary').first().click()
    ]);

    await expect(page).toHaveURL(/\/statistics$/);
    await expect(page.locator('.ant-card').first()).toBeVisible();
  });
});
