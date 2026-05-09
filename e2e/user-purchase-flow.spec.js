const { test, expect } = require('@playwright/test');

async function login(page, username = 'coco', password = '123456') {
  await page.goto('/login');
  await expect(page.getByPlaceholder('用户名')).toBeVisible();
  await page.getByPlaceholder('用户名').fill(username);
  await page.getByPlaceholder('密码').fill(password);
  await page.locator('.ant-tabs-tabpane-active button.ant-btn-primary').click();
}

test.describe('在线书店浏览器端到端黑盒测试', () => {
  test('等价类：用户搜索不存在的关键字时应显示空结果提示', async ({ page }) => {
    await login(page);
    await page.waitForURL('**/');

    const searchInput = page.locator('input[type="search"]').first();
    await searchInput.fill('definitely-no-such-book-keyword');
    await searchInput.press('Enter');

    await expect(page.locator('.ant-alert-info')).toContainText('没有找到相关书籍');
  });

  test('用户可以完成登录、浏览、加购、下单、查单整条链路', async ({ page }) => {
    await login(page);
    await page.waitForURL('**/');

    const firstBookLink = page.locator('a[href^="/book/"]').first();
    await expect(firstBookLink).toBeVisible();
    await firstBookLink.click();

    await expect(page).toHaveURL(/\/book\/\d+$/);

    const bookTitle = (await page.locator('h1').textContent()).trim();
    await expect(bookTitle).not.toBe('');

    const addToCartButton = page.locator('button.ant-btn-primary').first();
    await expect(addToCartButton).toBeEnabled();
    await addToCartButton.click();

    await page.goto('/cart');
    await expect(page).toHaveURL(/\/cart$/);

    const cartRow = page.locator('.ant-table-tbody tr').filter({ hasText: bookTitle }).first();
    await expect(cartRow).toBeVisible();

    const rowCheckbox = cartRow.locator('input.ant-checkbox-input');
    await rowCheckbox.check({ force: true });

    const checkoutButton = page.locator('button.ant-btn-primary.ant-btn-lg');
    await expect(checkoutButton).toBeEnabled();
    await checkoutButton.click();

    await expect(page.locator('.ant-modal')).toBeVisible();
    await page.locator('.ant-modal button.ant-btn-primary').click();

    await page.goto('/orders');
    await expect(page).toHaveURL(/\/orders$/);

    const searchInput = page.locator('.ant-card input').first();
    await searchInput.fill(bookTitle);
    await page.locator('.ant-card button.ant-btn-primary').first().click();

    const orderRows = page.locator('.ant-table-tbody tr.ant-table-row');
    await expect(orderRows.first()).toBeVisible();

    const expandIcon = orderRows.first().locator('.ant-table-row-expand-icon');
    if (await expandIcon.count()) {
      await expandIcon.click();
      await expect(page.getByText(bookTitle, { exact: false }).first()).toBeVisible();
    }
  });
});
