# 黑盒设计场景与 Playwright 自动化映射表

## 1. 说明

本表用于说明《黑盒测试用例》中的哪些场景已经落地为当前自动化测试，以及它们对应到哪些 Playwright 文件。

当前仓库中黑盒自动化只保留：

- `Playwright` 浏览器端到端黑盒测试

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

覆盖关系说明：

- `已覆盖`：当前 Playwright 已直接覆盖该黑盒场景
- `部分覆盖`：当前 Playwright 覆盖了该功能点，但未完整覆盖全部输入组合
- `未覆盖`：当前仍只停留在设计文档，尚未落地到 Playwright

## 2. 映射表

| 黑盒设计场景 | 设计编号 | 当前自动化覆盖关系 | Playwright 文件 | 是否已真实执行 |
| --- | --- | --- | --- | --- |
| 用户注册成功 | BB-01 | 未覆盖 | 无 | 否 |
| 注册用户名长度小于 3 | BB-02 | 已覆盖 | `e2e/decision-boundary.spec.js` | 是 |
| 注册确认密码不一致 | BB-03 | 已覆盖 | `e2e/decision-boundary.spec.js` | 是 |
| 普通用户登录成功 | BB-04 | 已覆盖 | `e2e/user-purchase-flow.spec.js` | 是 |
| 密码错误登录失败 | BB-05 | 已覆盖 | `e2e/decision-boundary.spec.js` | 是 |
| 未登录访问受保护页面 | BB-06 | 已覆盖 | `e2e/decision-boundary.spec.js` | 是 |
| 普通用户访问管理员页面被拒绝 | BB-07 | 已覆盖 | `e2e/decision-boundary.spec.js` | 是 |
| 图书列表浏览 | BB-08 | 已覆盖 | `e2e/user-purchase-flow.spec.js` | 是 |
| 图书详情查看 | BB-09 | 已覆盖 | `e2e/user-purchase-flow.spec.js` | 是 |
| 搜索存在的图书关键字 | BB-10 | 部分覆盖 | `e2e/user-purchase-flow.spec.js` | 是 |
| 搜索不存在的图书关键字 | BB-11 | 已覆盖 | `e2e/user-purchase-flow.spec.js` | 是 |
| 将图书加入购物车 | BB-12 | 已覆盖 | `e2e/user-purchase-flow.spec.js` | 是 |
| 购物车未勾选商品时不可结算 | BB-13 | 已覆盖 | `e2e/decision-boundary.spec.js` | 是 |
| 勾选商品后结算成功 | BB-14 | 已覆盖 | `e2e/user-purchase-flow.spec.js` | 是 |
| 用户按书名查询订单 | BB-15 | 已覆盖 | `e2e/user-purchase-flow.spec.js` | 是 |
| 管理员订单管理 | BB-16 | 已覆盖 | `e2e/admin-statistics.spec.js` | 是 |
| 管理员图书管理 | BB-17 | 已覆盖 | `e2e/admin-statistics.spec.js` | 是 |
| 管理员图书销量统计 | BB-18 | 已覆盖 | `e2e/admin-statistics.spec.js` | 是 |
| 管理员用户消费统计 | BB-19 | 已覆盖 | `e2e/admin-statistics.spec.js` | 是 |
| 普通用户个人购书统计 | BB-20 | 已覆盖 | `e2e/admin-statistics.spec.js` | 是 |

## 3. 可直接写入报告的结论

当前黑盒自动化已经完全统一为 Playwright 浏览器端到端执行方案，不再混用后端 API 级黑盒或前端页面级黑盒。

当前 Playwright 已真实覆盖：

- 登录成功
- 登录失败
- 未登录重定向
- 普通用户越权访问管理员页面
- 注册用户名边界值
- 注册确认密码边界值
- 图书浏览
- 图书详情
- 搜索无结果
- 加入购物车
- 购物车未勾选不可结算
- 勾选后成功结算
- 按书名查询订单
- 管理员订单管理
- 管理员图书管理
- 图书销量统计
- 用户消费统计
- 个人购书统计
- “登录 -> 浏览 -> 加购 -> 下单 -> 查单”核心业务链路

因此，当前黑盒自动化已经形成一套只基于真实浏览器行为、可重复执行、可直接用于课程作业提交的浏览器端到端黑盒方案。
