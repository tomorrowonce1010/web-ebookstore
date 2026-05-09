# Codex 驱动自动化测试工作流说明

## 1. 工作流目标

本工作流用于支撑课程作业《大模型驱动的智能化软件测试》，实现以下闭环：

1. Codex 阅读需求文档
2. Codex 提取功能点与测试范围
3. Codex 生成黑盒与白盒测试设计
4. Codex 在仓库内生成测试代码
5. Codex 真实执行测试
6. Codex 基于真实产物撰写测试报告

## 2. 输入

### 2.1 文档输入

- `README.md`

### 2.2 代码输入

- 前端：`src/`
- 后端：`backend/src/main/java/`

### 2.3 环境输入

- 前端运行环境
- 后端运行环境
- MySQL 数据库
- 测试账号：`admin / 123456`、`coco / 123456`

## 3. 输出

### 3.1 文档输出

- `docs/testing/feature-inventory.md`
- `docs/testing/test-plan.md`
- `docs/testing/test-cases-blackbox.md`
- `docs/testing/test-cases-whitebox.md`
- `docs/testing/blackbox-automation-mapping.md`
- `docs/testing/test-report.md`
- `docs/testing/llm-software-testing-report.tex`

### 3.2 代码输出

- Playwright 黑盒自动化
- 后端白盒自动化

## 4. 分工

### 4.1 Codex 负责

- 阅读需求和代码
- 提取功能点
- 设计黑盒与白盒场景
- 编写 Playwright 黑盒测试
- 整理黑盒映射表
- 汇总真实执行结果
- 修改文档和 LaTeX 报告

### 4.2 用户负责

- 保持前后端环境可运行
- 提供稳定账号
- 在需要时手动启动前端和后端
- 保存对话截图与结果截图

## 5. 当前黑盒工作流

本轮黑盒自动化已经统一为 Playwright 浏览器端到端方案，不再保留后端 API 黑盒或前端页面级黑盒。

黑盒代码文件：

- `playwright.config.js`
- `e2e/decision-boundary.spec.js`
- `e2e/user-purchase-flow.spec.js`
- `e2e/admin-statistics.spec.js`

真实执行命令：

```bash
cmd /c "set PLAYWRIGHT_MANUAL_SERVER=1 && npx playwright test --workers=1"
```

2026-04-24 真实执行结果：

- 测试文件数：`3`
- 测试用例总数：`13`
- 失败数：`0`
- 错误数：`0`
- 执行结果：`PASS`

## 6. 本轮真实发现并修复的问题

### 6.1 登录页定位器问题

Playwright 第一轮真实执行中，登录页已经正常打开，但脚本错误地假设页面存在 `form[name="login"]` 结构，导致全部用例在登录页定位失败。

修复方式：

- 改为基于真实可见控件定位
- 登录按钮改为激活页签中的主按钮定位
- 注册页输入框改为在激活页签中按输入顺序定位

### 6.2 结果

修复后重新真实执行：

- `e2e/decision-boundary.spec.js`：`6 passed`
- `e2e/user-purchase-flow.spec.js`：`2 passed`
- `e2e/admin-statistics.spec.js`：`5 passed`
- 全量 Playwright：`13 passed`

## 7. 提交时建议补充的截图

1. Codex 阅读 `README.md` 并提取功能点
2. Codex 生成 Playwright 黑盒场景
3. Playwright 第一轮失败截图与定位问题分析
4. Playwright 最终 `13 passed` 截图
5. JaCoCo 覆盖率报告截图
