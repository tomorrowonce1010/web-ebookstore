# Codex 自动化测试目录与执行结构

## 1. 当前执行口径

本仓库当前黑盒自动化只保留一条执行主线：

- 浏览器端到端黑盒：`Playwright`

白盒自动化保持现状，不在本轮黑盒调整中改动：

- 后端白盒：`JUnit 5 + Mockito + Spring Test + MockMvc + JaCoCo`

## 2. 目录规划

### 2.1 文档资产

- `docs/testing/feature-inventory.md`
- `docs/testing/test-plan.md`
- `docs/testing/test-cases-blackbox.md`
- `docs/testing/test-cases-whitebox.md`
- `docs/testing/blackbox-automation-mapping.md`
- `docs/testing/test-report.md`
- `docs/testing/codex-workflow.md`
- `docs/testing/llm-software-testing-report.tex`

### 2.2 黑盒自动化代码

- `playwright.config.js`
- `e2e/decision-boundary.spec.js`
- `e2e/user-purchase-flow.spec.js`
- `e2e/admin-statistics.spec.js`

### 2.3 白盒自动化代码

- `backend/src/test/java/...`

## 3. 黑盒执行方式

### 3.1 自动启动前后端

```bash
npx playwright test
```

适用场景：

- 本机允许 Playwright 自行拉起前端和后端

### 3.2 手动启动前后端后执行

先启动前端：

```bash
cd D:\course\softwaretest\hw1\web-ebookstore
npm start
```

再启动后端：

```bash
cd D:\course\softwaretest\hw1\web-ebookstore\backend
mvn spring-boot:run
```

然后执行黑盒：

```bash
cmd /c "set PLAYWRIGHT_MANUAL_SERVER=1 && npx playwright test --workers=1"
```

仓库中也提供了等价脚本：

```bash
npm run test:e2e:manual
```

## 4. 真实执行结果读取位置

### 4.1 黑盒

- `test-results/`

### 4.2 白盒

- `backend/target/site/jacoco/`

## 5. 当前黑盒真实执行结果

2026-04-24 在手动启动前后端后，真实执行：

```bash
cmd /c "set PLAYWRIGHT_MANUAL_SERVER=1 && npx playwright test --workers=1"
```

真实结果：

- 测试文件数：`3`
- 测试用例数：`13`
- 失败数：`0`
- 错误数：`0`
- 执行结果：`PASS`

## 6. 你需要配合的事项

1. 保持前端可通过 `http://localhost:3000` 访问
2. 保持后端可通过 `http://localhost:8081` 访问
3. 测试账号保持稳定
4. 如数据库状态被手工改动，及时说明
