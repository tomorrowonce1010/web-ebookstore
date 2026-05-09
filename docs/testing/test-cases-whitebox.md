# 《在线书店系统》白盒测试点清单
## 1. 编制依据

- 需求文档：[README.md](d:\course\softwaretest\hw1\web-ebookstore\README.md)
- 代码范围：`backend/src/main/java/com/ebookstore/service/impl/`、`backend/src/main/java/com/ebookstore/controller/`
- 测试计划：[test-plan.md](d:\course\softwaretest\hw1\web-ebookstore\docs\testing\test-plan.md)

## 2. 白盒覆盖策略

本项目白盒测试以“后端核心业务层为主，控制器层为辅”的方式组织，原因如下：

1. `ServiceImpl` 中集中了主要业务分支、异常路径和数据流，最适合做语句覆盖、分支覆盖和基本路径覆盖。
2. `Controller` 中存在明显的权限控制、参数校验和 HTTP 状态码映射，适合使用 `MockMvc` 做接口级白盒验证。
3. 前端白盒只选取与业务强相关的上下文和路由守卫代码，不追求用前端覆盖率替代后端核心业务覆盖率。

## 3. 核心模块与覆盖重点

| 模块 | 文件 | 白盒目标 | 关键分支/路径 |
| --- | --- | --- | --- |
| 认证服务 | `AuthServiceImpl` | 登录、注册、Session、密码校验 | 用户不存在、密码错误、禁用用户、注册冲突、成功登录、成功注册 |
| 图书服务 | `BookServiceImpl` | 图书查询、软删除、库存更新、库存扣减 | 图书存在/不存在、库存不足、扣减后库存为 0、恢复上架 |
| 购物车服务 | `CartServiceImpl` | 加购、删改、权限校验、选择状态 | 未登录、已有购物项累加、越权删除、数量小于等于 0 |
| 订单服务 | `OrderServiceImpl` | 购物车下单、直接下单、订单查询、过滤 | 空购物车、库存不足、扣库存失败、越权查单、过滤条件为空/非空 |
| 统计服务 | `StatisticsServiceImpl` | 销量统计、消费统计、个人统计 | 空数据集、聚合累计、排序、过滤零值 |
| 用户服务 | `UserServiceImpl` | 当前用户获取、状态切换、资料更新 | Session 缺失、当前用户为空、切换启用状态、更新资料成功 |
| 控制器层 | `AuthController` 等 | 接口状态码、参数透传、角色边界 | `200/400/401/403` 返回路径 |

## 4. 基本路径覆盖设计

### 4.1 `AuthServiceImpl`

| 路径编号 | 入口方法 | 路径说明 | 预期 |
| --- | --- | --- | --- |
| WB-AUTH-01 | `login` | 用户名不存在 -> 直接失败返回 | 返回“用户不存在” |
| WB-AUTH-02 | `login` | 用户存在 -> 密码不匹配 -> 失败返回 | 返回“密码错误” |
| WB-AUTH-03 | `login` | 用户存在 -> 密码正确 -> 用户被禁用 | 返回“用户已被禁用” |
| WB-AUTH-04 | `login` | 用户存在 -> 密码正确 -> 用户启用 -> 写入 Session | 返回成功 DTO，Session 保存当前用户 |
| WB-AUTH-05 | `register` | 密码与确认密码不一致 | 返回失败信息 |
| WB-AUTH-06 | `register` | 用户名已存在 | 返回失败信息 |
| WB-AUTH-07 | `register` | 邮箱已存在 | 返回失败信息 |
| WB-AUTH-08 | `register` | 所有校验通过 -> 保存 User 与 UserAuth | 返回成功 DTO |

### 4.2 `BookServiceImpl`

| 路径编号 | 入口方法 | 路径说明 | 预期 |
| --- | --- | --- | --- |
| WB-BOOK-01 | `softDeleteBook` | 图书存在 -> 标记删除 | 返回 `true` |
| WB-BOOK-02 | `softDeleteBook` | 图书不存在 | 返回 `false` |
| WB-BOOK-03 | `restoreBook` | 已删除图书恢复上架 | 返回 `true` |
| WB-BOOK-04 | `updateStock` | 库存更新为正数 | 返回 `true` 且库存更新 |
| WB-BOOK-05 | `reduceStock` | 当前库存不足 | 返回 `false` |
| WB-BOOK-06 | `reduceStock` | 库存充足 -> 扣减后为 0 | 返回 `true` 且触发后续下架逻辑 |

### 4.3 `CartServiceImpl`

| 路径编号 | 入口方法 | 路径说明 | 预期 |
| --- | --- | --- | --- |
| WB-CART-01 | `getCartItems` | 当前用户为空 | 抛出 `SecurityException` |
| WB-CART-02 | `addToCart` | 当前用户为空 | 抛出 `SecurityException` |
| WB-CART-03 | `addToCart` | 已存在相同图书购物项 | 数量累加并保存 |
| WB-CART-04 | `addToCart` | 不存在购物项 | 新建购物项 |
| WB-CART-05 | `removeFromCart` | 登录用户与参数用户不一致 | 抛出 `SecurityException` |
| WB-CART-06 | `updateCartItemQuantity` | 数量小于等于 0 | 删除购物项 |
| WB-CART-07 | `toggleCartItemSelection` | 购物项不属于当前用户 | 抛出 `SecurityException` |

### 4.4 `OrderServiceImpl`

| 路径编号 | 入口方法 | 路径说明 | 预期 |
| --- | --- | --- | --- |
| WB-ORDER-01 | `getOrderById` | 订单不属于当前用户 | 抛出 `SecurityException` |
| WB-ORDER-02 | `createOrder` | 购物项列表为空 | 抛出 `IllegalArgumentException` |
| WB-ORDER-03 | `createOrder` | 选中项为空 | 抛出 `IllegalArgumentException` |
| WB-ORDER-04 | `createOrder` | 有商品库存不足 | 抛出 `IllegalArgumentException` |
| WB-ORDER-05 | `createOrder` | 校验通过 -> 创建订单 -> 扣库存 -> 删除购物项 | 返回订单项 DTO 列表 |
| WB-ORDER-06 | `createOrder` | 扣库存失败 | 抛出 `RuntimeException` |
| WB-ORDER-07 | `createDirectOrder` | 直接购买库存不足 | 抛出 `IllegalArgumentException` |
| WB-ORDER-08 | `createDirectOrder` | 直接购买成功 | 返回订单项 DTO 列表 |
| WB-ORDER-09 | `searchUserOrders` | 书名、开始时间、结束时间不同组合 | 返回过滤后的订单 DTO 列表 |
| WB-ORDER-10 | `searchAllOrders` | 管理员查询全量订单并带条件过滤 | 返回过滤后的系统订单 DTO 列表 |

### 4.5 `StatisticsServiceImpl`

| 路径编号 | 入口方法 | 路径说明 | 预期 |
| --- | --- | --- | --- |
| WB-STAT-01 | `getBookSalesStatistics` | 时间范围内无订单 | 返回空列表 |
| WB-STAT-02 | `getBookSalesStatistics` | 多订单多书聚合 | 销量与销售额累计正确并排序 |
| WB-STAT-03 | `getUserConsumptionStatistics` | 无用户消费 | 返回空列表 |
| WB-STAT-04 | `getUserConsumptionStatistics` | 多用户消费聚合 | 消费金额与订单数统计正确 |
| WB-STAT-05 | `getPersonalStatistics` | 指定用户无订单 | 返回零值统计 |
| WB-STAT-06 | `getPersonalStatistics` | 指定用户有多种图书订单 | 返回按图书聚合的个人统计 |

### 4.6 `UserServiceImpl`

| 路径编号 | 入口方法 | 路径说明 | 预期 |
| --- | --- | --- | --- |
| WB-USER-01 | `getCurrentUser` | Session 不存在 | 抛出“用户未登录”异常 |
| WB-USER-02 | `getCurrentUser` | Session 存在但无当前用户信息 | 抛出“用户未登录”异常 |
| WB-USER-03 | `toggleUserStatus` | 目标用户存在 | 启用状态取反并持久化 |
| WB-USER-04 | `updateUserInfo` | 当前用户不存在 | 抛出 `EntityNotFoundException` |
| WB-USER-05 | `updateUserInfo` | 当前用户存在 | 返回更新后的 `UserDTO` |

## 5. 数据流分析重点

### 5.1 登录与会话数据流

`username/password -> UserAuth 查询 -> BCrypt 校验 -> UserInfoDTO -> Session`

重点检查：
- 明文密码不会直接持久化
- 登录成功后 Session 中保存当前用户
- 被禁用用户不会写入有效 Session

### 5.2 下单事务数据流

`CartItem -> 校验库存 -> 创建 Order -> 创建 OrderItem -> reduceStock -> deleteCartItem`

重点检查：
- 任一环节失败时是否抛出异常
- 订单金额与订单项金额是否一致
- 成功路径是否完成库存扣减与购物车清理闭环

### 5.3 统计聚合数据流

`Order/OrderItem -> 按书籍或用户聚合 -> 计算数量/金额 -> 排序 -> DTO`

重点检查：
- 聚合口径是否一致
- 时间过滤是否同时作用于订单与订单项
- 零值数据是否被正确过滤

## 6. 推荐自动化测试类命名

| 层级 | 推荐类名 |
| --- | --- |
| Service 单元测试 | `AuthServiceImplTest`、`BookServiceImplTest`、`CartServiceImplTest`、`OrderServiceImplTest`、`StatisticsServiceImplTest`、`UserServiceImplTest` |
| Controller 切片测试 | `AuthControllerTest`、`CartControllerTest`、`OrderControllerTest`、`StatisticsControllerTest` |
| 前端关键逻辑测试 | `ProtectedRoute.test.js`、`AuthContext.test.js`、`CartContext.test.js` |

## 7. 覆盖率目标与统计口径

- 白盒核心统计范围：`backend/src/main/java/com/ebookstore/service/impl/`
- 目标：关键业务类语句覆盖率与分支覆盖率尽可能逼近并达到课程要求中的 `95%`
- 控制器层覆盖作为补充，不单独承担全部覆盖率目标
- 最终以 JaCoCo 报告与前端 Jest 覆盖率报告中的真实执行结果为准

## 8. 可直接写入作业的结论

本项目白盒测试不以“全仓库平均覆盖率”为唯一目标，而是围绕认证、图书、购物车、订单、统计、用户六个核心业务服务进行路径覆盖和数据流分析。这样既能满足课程对白盒方法的要求，又能把覆盖率资源集中在最能体现系统正确性的关键逻辑上，更适合作为“大模型驱动智能化软件测试”的落地方案。
