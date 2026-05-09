const { test, expect } = require('@playwright/test');

async function gotoLoginPage(page) {
  await page.goto('/login');
  await expect(page.getByPlaceholder('用户名')).toBeVisible();
}

async function login(page, username = 'coco', password = '123456') {
  await gotoLoginPage(page);
  await page.getByPlaceholder('用户名').fill(username);
  await page.getByPlaceholder('密码').fill(password);
  await page.locator('.ant-tabs-tabpane-active button.ant-btn-primary').click();
}

async function addFirstBookToCart(page) {
  const firstBookLink = page.locator('a[href^="/book/"]').first();
  await expect(firstBookLink).toBeVisible();
  await firstBookLink.click();
  await expect(page).toHaveURL(/\/book\/\d+$/);
  await Promise.all([
    page.waitForResponse((response) =>
      response.url().includes('/api/cart/add') &&
      response.request().method() === 'POST' &&
      response.status() === 200
    ),
    page.locator('button.ant-btn-primary').first().click()
  ]);
}

test.describe('在线书店决策表与边界值 E2E 黑盒测试', () => {
  test('决策表：未登录用户访问购物车时应被重定向到登录页', async ({ page }) => {
    await page.goto('/cart');
    await page.waitForURL('**/login');
    await expect(page.getByPlaceholder('用户名')).toBeVisible();
  });

  test('决策表：用户名正确但密码错误时应登录失败', async ({ page }) => {
    await login(page, 'coco', 'wrong-password');
    await expect(page.locator('.ant-modal')).toBeVisible();
    await expect(page.locator('.ant-modal')).toContainText('登录失败');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('决策表：普通用户访问管理员订单页时应显示权限不足', async ({ page }) => {
    await login(page);
    await page.waitForURL('**/');
    await page.goto('/admin/orders');
    await expect(page.getByRole('heading', { name: '权限不足' })).toBeVisible();
  });

  test('边界值：注册用户名长度小于 3 时应被前端校验拦截', async ({ page }) => {
    await gotoLoginPage(page);
    await page.locator('.ant-tabs-tab').nth(1).click();
    const registerPane = page.locator('.ant-tabs-tabpane-active');
    const inputs = registerPane.locator('input');

    await inputs.nth(0).fill('ab');
    await inputs.nth(1).fill('123456');
    await inputs.nth(2).fill('123456');
    await inputs.nth(3).fill('边界用户');
    await inputs.nth(4).fill('boundary-short@example.com');
    await registerPane.locator('button.ant-btn-primary').click();

    await expect(registerPane).toContainText('用户名至少3个字符');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('边界值：注册确认密码不一致时应被前端校验拦截', async ({ page }) => {
    await gotoLoginPage(page);
    await page.locator('.ant-tabs-tab').nth(1).click();
    const registerPane = page.locator('.ant-tabs-tabpane-active');
    const inputs = registerPane.locator('input');

    await inputs.nth(0).fill('boundaryuser');
    await inputs.nth(1).fill('123456');
    await inputs.nth(2).fill('654321');
    await inputs.nth(3).fill('边界用户');
    await inputs.nth(4).fill('boundary-mismatch@example.com');
    await registerPane.locator('button.ant-btn-primary').click();

    await expect(registerPane).toContainText('两次输入的密码不一致');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('边界值：购物车未勾选任何商品时结算按钮应禁用', async ({ page }) => {
    await login(page);
    await page.waitForURL('**/');
    await addFirstBookToCart(page);

    await page.goto('/cart');
    const cartRows = page.locator('.ant-table-tbody tr.ant-table-row');
    await expect(cartRows.first()).toBeVisible();

    const checkoutButton = page.locator('button.ant-btn-primary.ant-btn-lg');
    await expect(checkoutButton).toBeDisabled();
  });
});
