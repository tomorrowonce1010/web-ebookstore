# 《在线书店系统》测试报告

## 1. 执行环境

- 执行日期：`2026-04-24`
- 项目目录：`D:\course\softwaretest\hw1\web-ebookstore`
- 前端地址：`http://localhost:3000`
- 后端地址：`http://localhost:8081`
- 黑盒框架：`Playwright`
- 白盒覆盖率工具：`JaCoCo 0.8.12`

## 2. 当前黑盒自动化策略

本轮仓库调整后，黑盒自动化只保留一条执行主线：

- 浏览器端到端黑盒：`Playwright`

本轮已经彻底移除：

- 后端 API 级黑盒测试代码
- 前端页面级黑盒测试代码

因此，当前黑盒测试结果全部来自真实浏览器端到端执行。

## 3. Playwright 浏览器端到端黑盒真实执行结果

### 3.1 真实执行前提

先手动启动前端：

```bash
cd D:\course\softwaretest\hw1\web-ebookstore
npm start
```

再手动启动后端：

```bash
cd D:\course\softwaretest\hw1\web-ebookstore\backend
mvn spring-boot:run
```

### 3.2 真实执行命令

```bash
cmd /c "set PLAYWRIGHT_MANUAL_SERVER=1 && npx playwright test --workers=1"
```

### 3.3 真实执行结果

| 指标 | 结果 |
| --- | --- |
| 测试文件数 | 3 |
| 测试用例总数 | 13 |
| 失败数 | 0 |
| 错误数 | 0 |
| 执行结果 | PASS |
| 总耗时 | 38.0s |

对应测试文件：

- `e2e/decision-boundary.spec.js`
- `e2e/user-purchase-flow.spec.js`
- `e2e/admin-statistics.spec.js`

## 4. 当前 Playwright 已覆盖的黑盒内容

### 4.1 决策表类场景

- 未登录访问购物车，重定向到登录页
- 用户名正确但密码错误，登录失败
- 普通用户访问管理员订单页，显示权限不足
- 管理员登录后访问订单管理页
- 管理员访问图书管理页
- 管理员查询图书销量统计
- 管理员切换到用户消费统计并查询
- 普通用户查询个人购书统计

### 4.2 边界值类场景

- 注册用户名长度小于 3，被前端校验拦截
- 注册确认密码不一致，被前端校验拦截
- 购物车未勾选任何商品时，结算按钮禁用

### 4.3 等价类与核心链路场景

- 搜索不存在的图书关键字，显示空结果提示
- 普通用户成功登录
- 浏览图书列表
- 进入图书详情页
- 将图书加入购物车
- 勾选商品结算成功
- 按书名查询订单
- 跑通“登录 -> 浏览 -> 加购 -> 下单 -> 查单”核心业务链路

## 5. Playwright 真实发现并修复的问题

### 5.1 第一轮真实执行结果

在第一次全量执行 Playwright 时，真实结果为：

- 测试用例数：`13`
- 失败数：`13`

失败原因不是业务逻辑异常，而是黑盒脚本定位器假设错误：

- 错误假设登录页存在 `form[name="login"]`
- 实际页面中 Ant Design 渲染结构与该假设不一致
- 因此所有场景在登录页定位失败

### 5.2 修复措施

对 Playwright 黑盒脚本进行了如下修复：

1. 登录页改为基于真实可见控件定位
2. 登录按钮改为定位激活页签中的主按钮
3. 注册场景改为在激活页签中按输入顺序定位
4. 搜索与统计查询尽量改用结构化定位，减少对中文按钮名的依赖

### 5.3 修复后真实回归结果

修复后分组真实回归结果：

- `e2e/decision-boundary.spec.js`：`6 passed`
- `e2e/user-purchase-flow.spec.js`：`2 passed`
- `e2e/admin-statistics.spec.js`：`5 passed`

最终全量真实回归结果：

- `13 passed`

这说明当前 Playwright 黑盒方案已经能够稳定执行。

## 6. 黑盒设计场景与自动化映射

对应映射表见：

- `docs/testing/blackbox-automation-mapping.md`

## 7. 白盒部分说明

本轮只调整黑盒自动化，不修改既有白盒测试实现。

白盒覆盖率数据继续沿用已有真实执行产物：

- `backend/target/site/jacoco/jacoco.csv`

当前 JaCoCo 结果保持为：

| 覆盖指标 | 结果 |
| --- | --- |
| 总行覆盖率 | 97.97% |
| 总指令覆盖率 | 96.46% |
| 总分支覆盖率 | 82.54% |

## 8. 小组成员贡献

| 成员 | 贡献内容 | 比例 |
| --- | --- | ---: |
| 当前负责人 | 使用 Codex 构建测试工作流，完成黑盒设计、Playwright 黑盒实现、真实执行、问题修复与文档整理 | 100% |

## 9. 结论

当前仓库中的黑盒自动化已经完全统一为 Playwright 浏览器端到端方案。2026-04-24 在手动启动前后端后，真实执行 `cmd /c "set PLAYWRIGHT_MANUAL_SERVER=1 && npx playwright test --workers=1"`，共得到 `13` 个测试全部通过。

这套黑盒成果同时覆盖了等价类、边界值和决策表三类设计方法，并真实跑通了在线书店系统的用户核心购买链路和管理员核心管理链路，已经可以作为课程作业中的黑盒自动化执行成果提交。
