# 二手交易平台前端 AI 生成说明书

本文档用于指导 AI 基于当前仓库生成 `dbksf` 前端代码。

这不是普通接口清单，而是一份面向前端实现的完整规格文档，包含：

- 当前前端基础现状
- 当前后端真实能力与限制
- 前端页面路由设计
- 页面信息架构
- API 契约与类型定义
- 前端登录态与鉴权策略
- 视觉与交互规范
- AI 实现约束
- 联调与验收清单

---

## 1. 项目目标

目标是生成一个基于 Vue 的前端应用，分为两套业务界面：

- 用户端
- 管理端

用户从统一登录页进入，根据登录模式与后续身份接口结果，路由到对应界面：

- 用户端进入 `/user/shop`
- 管理端进入 `/admin/review`

前端本期只基于当前 `dbksh` 后端真实能力实现，不允许臆造不存在的后端接口。

---

## 2. 当前前端基础

`dbksf` 当前只是一个 Vite 初始化的 Vue 3 项目。

### 2.1 当前已存在依赖

- `vue`
- `axios`
- `element-plus`

### 2.2 当前不存在的业务能力

- 没有业务页面
- 没有路由系统
- 没有 API 封装层
- 没有登录态管理
- 没有布局壳层
- 没有菜单导航

### 2.3 前端架构前提

本说明书按以下最小前端架构设计：

- Vue 3
- Axios
- Element Plus
- Vue Router
- 不引入 Pinia
- 登录态使用组合式 API + `localStorage`

> 注意：当前 `dbksf/package.json` 里还没有 `vue-router`。本文档可以把它作为目标架构前提写清楚，但真正实施时需要先获得允许再安装依赖。

---

## 3. 当前后端真实能力与限制

这一节是 AI 生成前端时必须遵守的硬约束。

### 3.1 真实接口地址

当前后端只存在以下 14 个接口：

- `POST /user/code`
- `POST /user/login`
- `GET /user/me`
- `GET /shop/products/category`
- `GET /shop/products/search`
- `GET /shop/products/{id}`
- `GET /user/product/mine`
- `POST /user/product`
- `PUT /user/product/{id}`
- `POST /user/report`
- `POST /admin/login`
- `GET /admin/me`
- `GET /admin/reports`
- `PUT /admin/reports/{id}`

### 3.2 不允许臆造的后端能力

前端实现时，不允许假设以下接口存在：

- 不存在短信手机号发送接口
- 不存在图片文件上传接口
- 不存在商品分页接口
- 不存在“获取我的商品完整详情”接口
- 不存在退出登录接口
- 不存在刷新 token 接口
- 不存在管理员单独发送验证码接口
- 不存在用户名称、卖家名称映射接口

### 3.3 与产品表述的差异

用户需求里提到“短信接口”“上传商品图片”，但当前后端实际情况是：

- 验证码发送接口是 `POST /user/code`
- 发送参数名是 `wechat`
- 登录表单字段是 `wechat + code`
- 发布商品和更新商品只接受 `imageUrls: string[]`
- `imageUrls` 表示图片 URL 字符串数组，不是文件对象

### 3.4 当前后端的关键业务限制

#### 登录

- `POST /user/login` 只允许用户端角色
- `POST /admin/login` 只允许管理员角色
- 登录接口成功后只返回 `token`
- 登录接口不会直接返回 `role`
- 前端必须登录成功后再调用 `/user/me` 或 `/admin/me`

#### 商品浏览

- 商品列表接口没有“全部商品”接口
- 只能：
  - 按分类查询
  - 按关键词查询
- 搜索接口实际匹配的是：
  - 商品名称
  - 标签名称
- 当前后端不支持按商品描述全文搜索

#### 商品详情

- `GET /shop/products/{id}` 只返回已上架商品详情
- 返回结构不包含 `tags`
- 返回结构不包含 `status`

#### 我的商品

- `GET /user/product/mine` 只返回：
  - `id`
  - `name`
  - `tags`
  - `imageUrl`
  - `description`
  - `status`
- 该接口不返回：
  - `category`
  - `imageUrls`
  - `price`
  - `wechat`
  - `address`
- 但 `PUT /user/product/{id}` 是全量更新接口，上述字段都需要提交

这意味着前端编辑“我的商品”时存在真实后端缺口：

- 已上架商品可以额外调用 `GET /shop/products/{id}` 补齐部分字段
- 已下架商品和审核中商品没有对应完整详情接口可读
- 因此前端编辑表单必须允许“部分已有值 + 手动补填剩余必填项”

#### 举报

- 举报接口 `POST /user/report` 需要登录
- 被举报商品必须处于：
  - `已上架`
  - 或 `审核中`
- 重复提交同一商品的待处理举报会失败

#### 审核

- 管理端审核接口 `PUT /admin/reports/{id}` 只接受：
  - `已上架`
  - `已下架`
- 不接受 `审核中`

### 3.5 token 与鉴权行为

- 请求头名称固定为：`authorization`
- 值为原始 token 字符串
- 不是 `Bearer <token>`

后端还有两个重要特征：

- 所有正常业务失败通常返回 HTTP `200`，但 `code = 0`
- 未登录或无权限时，拦截器直接返回：
  - `401`
  - `403`

所以前端不能只看 HTTP 状态码，也不能只看 `code`。

---

## 4. 前端页面路由设计

这一节只定义前端页面路由，不是后端接口地址。

## 4.1 路由表

| 前端路由 | 是否登录 | 角色要求 | 页面说明 |
| --- | --- | --- | --- |
| `/login` | 否 | 无 | 统一登录页，含用户端/管理端切换 |
| `/user/shop` | 是 | 用户端 | 商品浏览页 |
| `/user/publish` | 是 | 用户端 | 商品发布页 |
| `/user/me` | 是 | 用户端 | 用户个人页 + 我的商品 |
| `/admin/review` | 是 | 管理员 | 举报审核页 |
| `/admin/me` | 是 | 管理员 | 管理员个人页 |

### 4.2 重定向规则

- `/`：
  - 无登录态时跳 `/login`
  - 用户端跳 `/user/shop`
  - 管理员跳 `/admin/review`
- 未知路径：
  - 有登录态时跳对应角色默认首页
  - 无登录态时跳 `/login`

### 4.3 路由守卫

必须实现统一路由守卫，规则如下：

- 无 token 时，禁止进入所有 `/user/*` 和 `/admin/*`
- `role = 用户端` 时，禁止进入 `/admin/*`
- `role = 管理员` 时，禁止进入 `/user/*`
- 访问受保护页面但本地没有 `role` 时，需要先尝试用对应 `/me` 接口恢复身份
- 如果恢复失败：
  - 清空本地登录态
  - 跳转 `/login`

---

## 5. 页面信息架构

## 5.1 登录页 `/login`

### 页面目标

- 让用户选择登录模式
- 发送验证码
- 登录
- 登录成功后根据身份进入正确界面

### 页面结构

- 页面中部为登录面板
- 登录面板顶部提供模式切换：
  - 用户端
  - 管理端
- 表单字段：
  - `wechat`
  - `code`
- 操作按钮：
  - 获取验证码
  - 登录

### 交互规则

- 获取验证码：
  - 无论当前是用户端还是管理端，都调用 `POST /user/code`
- 用户端登录：
  - 调用 `POST /user/login`
  - 成功后调用 `GET /user/me`
  - `role` 必须为 `用户端`
  - 最终跳转 `/user/shop`
- 管理端登录：
  - 调用 `POST /admin/login`
  - 成功后调用 `GET /admin/me`
  - `role` 必须为 `管理员`
  - 最终跳转 `/admin/review`

### 前端文案约束

- 页面按钮可以写“获取验证码”
- 不要写“短信验证码登录”
- 因为后端当前并不是手机号短信接口

## 5.2 用户端壳层

### 左侧菜单固定项

- 商品浏览
- 发布
- 我的

### 推荐布局

- 左侧固定宽度菜单栏
- 右侧为主内容区
- 顶部可展示当前用户名与退出按钮

## 5.3 管理端壳层

### 左侧菜单固定项

- 审核
- 我的

### 推荐布局

- 左侧固定菜单
- 右侧内容区
- 顶部显示管理员信息与退出按钮

## 5.4 用户端商品浏览页 `/user/shop`

### 页面职责

- 查看商品列表
- 按分类切换
- 按关键词搜索
- 查看商品详情
- 举报商品

### 页面结构

- 顶部筛选区：
  - 分类切换
  - 搜索输入框
- 中间列表区：
  - 商品卡片或列表行
- 右侧详情抽屉：
  - 点击“查看”后打开
- 举报弹窗：
  - 点击“举报”后打开

### 默认数据策略

- 因为没有“全部商品”接口
- 页面初次进入时默认使用分类查询
- 默认分类固定为：`二手书`

### 列表项展示字段

- 名称
- 标签列表
- 价格
- 主图
- 相对发布时间
- 描述摘要

### 列表项操作

- 查看
- 举报

### 商品详情抽屉规则

点击“查看”后：

- 调用 `GET /shop/products/{id}`
- 展示：
  - 商品名称
  - 分类
  - 多张图片
  - 描述
  - 价格
  - 微信
  - 地址

注意：

- 详情接口本身不返回标签
- 如果需要展示标签，应使用列表项已有 `tags`

### 举报交互

- 举报理由使用文本域
- 提交调用 `POST /user/report`
- 举报成功后提示成功
- 不需要刷新当前列表

## 5.5 用户端发布页 `/user/publish`

### 页面职责

- 创建新商品

### 表单字段

- `category`
- `name`
- `imageUrls`
- `description`
- `price`
- `wechat`
- `address`
- `tags`

### UI 规则

- `imageUrls` 必须做成“多条 URL 输入”
- 不做本地文件上传
- 支持添加/删除多条图片 URL
- `tags` 输入可以用标签输入体验，但最终提交前必须拼成空格分隔字符串

### 提交成功后行为

- 提示“发布成功”
- 跳转到 `/user/me`

## 5.6 用户端我的页 `/user/me`

### 页面职责

- 展示个人信息
- 展示当前用户发布过的商品
- 编辑当前用户的商品

### 页面结构

- 顶部：个人信息卡片
- 下方：我的商品列表
- 右侧：编辑抽屉

### 进入页面时请求

- `GET /user/me`
- `GET /user/product/mine`

### 我的商品列表字段

- 商品名称
- 标签
- 主图
- 描述
- 状态

### 编辑规则

编辑抽屉使用 `PUT /user/product/{id}`，这是全量更新接口。

必须提交：

- `category`
- `name`
- `imageUrls`
- `description`
- `price`
- `wechat`
- `address`
- `tags`
- `status`

但当前后端没有“我的商品完整详情”接口，因此编辑时按以下规则处理：

#### 已上架商品

可先调用：

- `GET /shop/products/{id}`

将以下字段预填：

- `category`
- `imageUrls`
- `description`
- `price`
- `wechat`
- `address`

再结合 `GET /user/product/mine` 的当前列表项补齐：

- `name`
- `tags`
- `status`

#### 已下架 / 审核中商品

当前没有完整详情接口可读，无法完整预填。

前端必须采用以下策略：

- 使用 mine 列表已知字段先预填：
  - `name`
  - `description`
  - `tags`
  - `status`
  - `imageUrls` 仅可先放入主图 `imageUrl` 作为默认第一项
- 以下字段要求用户手动补填：
  - `category`
  - `price`
  - `wechat`
  - `address`
  - 如有需要，补全全部 `imageUrls`

### 状态字段规则

更新接口只允许前端提交：

- `已上架`
- `已下架`

即使当前商品状态显示为 `审核中`，提交时也不能传 `审核中`。

## 5.7 管理端审核页 `/admin/review`

### 页面职责

- 查看举报列表
- 对举报执行上架/下架处理

### 页面结构

- 顶部：筛选说明或状态提示
- 主体：举报表格

### 页面初始请求

- `GET /admin/reports`

### 列表展示字段

- 举报 ID
- 举报人 ID
- 商品 ID
- 卖家 ID
- 举报原因
- 举报状态
- 商品名称
- 商品状态
- 创建时间
- 更新时间

注意：

- 当前后端只返回 ID，不返回举报人昵称或卖家昵称
- 前端不要假设存在姓名映射接口

### 操作按钮

- 上架
- 下架

### 提交规则

点击操作后调用：

- `PUT /admin/reports/{id}`

请求体为：

```json
{
  "productStatus": "已下架"
}
```

如果该记录已处理：

- 前端建议禁用按钮
- 或点击后接收后端 `report already processed`

### 操作成功后行为

- 重新请求 `GET /admin/reports`
- 刷新表格

## 5.8 管理端我的页 `/admin/me`

### 页面职责

- 展示管理员个人信息

### 页面初始请求

- `GET /admin/me`

### 展示字段

- `id`
- `username`
- `role`

---

## 6. API 契约

## 6.1 基础访问配置

### 后端基础信息

- 默认服务地址：`http://localhost:8080`
- 无全局上下文前缀

### 前端推荐配置

- 使用统一 API 基础路径配置，例如：
  - `VITE_API_BASE_URL=http://localhost:8080`
- 开发时可使用 Vite 代理，也可直接使用完整 baseURL

## 6.2 统一返回结构

正常业务接口统一返回：

```ts
interface ApiResult<T> {
  code: number
  msg: string | null
  data: T | null
}
```

规则：

- `code = 1`：成功
- `code = 0`：业务失败

注意：

- `401` 和 `403` 来自拦截器，通常不是 `ApiResult`
- 所以前端请求封装必须同时处理：
  - HTTP 状态码
  - `ApiResult.code`

## 6.3 统一类型定义

以下为推荐给 AI 使用的前端类型定义。

```ts
type Role = '用户端' | '管理员'

type Category = '二手书' | '闲置物品' | '电子产品' | '日用品'

type ProductStatus = '已上架' | '已下架' | '审核中'

type EditableProductStatus = '已上架' | '已下架'

type ReportStatus = '待处理' | '已处理'

interface LoginForm {
  wechat: string
  code: string
}

interface UserProfile {
  id: number
  username: string
  role: Role
}

interface ProductListItem {
  id: number
  name: string
  tags: string[]
  price: number
  imageUrl: string | null
  relativeTime: string
  description: string
}

interface ProductDetail {
  id: number
  name: string
  category: Category
  imageUrls: string[]
  description: string
  price: number
  wechat: string | null
  address: string | null
}

interface MyProductItem {
  id: number
  name: string
  tags: string[]
  imageUrl: string | null
  description: string
  status: ProductStatus
}

interface ProductCreatePayload {
  category: Category
  name: string
  imageUrls: string[]
  description: string
  price: number
  wechat?: string
  address?: string
  tags?: string
}

interface ProductUpdatePayload extends ProductCreatePayload {
  status: EditableProductStatus
}

interface ReportCreatePayload {
  productId: number
  reason: string
}

interface AdminReportRecord {
  id: number
  userId: number
  productId: number
  sellerId: number
  reason: string
  status: ReportStatus
  createTime: string
  updateTime: string
  productName: string | null
  productStatus: ProductStatus | null
}

interface ReportHandlePayload {
  productStatus: EditableProductStatus
}
```

## 6.4 会话状态结构

推荐本地保存结构如下：

```ts
interface SessionState {
  token: string
  role: Role
  userId: number
  username: string
}
```

推荐本地存储键名：

- `dbks_token`
- `dbks_role`
- `dbks_user`

## 6.5 接口定义总表

### 认证与个人信息

#### 1. `POST /user/code`

- 用途：发送验证码
- 鉴权：否
- 传参位置：query 或表单参数
- 参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `wechat` | `string` | 是 | 微信号/登录标识 |

- 注意：
  - 这不是 JSON body 接口
  - 推荐 Axios 用 `params` 或 `URLSearchParams`
- 成功返回：`ApiResult<null>`
- 常见失败：
  - `wechat cannot be blank`
- 前端触发场景：
  - 登录页点击“获取验证码”

#### 2. `POST /user/login`

- 用途：用户端登录
- 鉴权：否
- body：

```json
{
  "wechat": "wechat_001",
  "code": "123456"
}
```

- 成功返回：`ApiResult<string>`，`data` 为 token
- 常见失败：
  - `wechat cannot be blank`
  - `code cannot be blank`
  - `verification code error`
  - `create user failed`
  - `forbidden`
- 前端触发场景：
  - 登录页处于用户端模式时点击登录

#### 3. `POST /admin/login`

- 用途：管理端登录
- 鉴权：否
- body：

```json
{
  "wechat": "admin_001",
  "code": "123456"
}
```

- 成功返回：`ApiResult<string>`，`data` 为 token
- 常见失败：
  - `wechat cannot be blank`
  - `code cannot be blank`
  - `verification code error`
  - `user not found`
  - `forbidden`
- 前端触发场景：
  - 登录页处于管理端模式时点击登录

#### 4. `GET /user/me`

- 用途：获取当前用户信息
- 鉴权：是
- 请求头：

```http
authorization: <token>
```

- 成功返回：

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "id": 1,
    "username": "alice",
    "role": "用户端"
  }
}
```

- 前端触发场景：
  - 用户端登录成功后的身份确认
  - 用户端页面刷新后的身份恢复
  - 用户端“我的”页显示个人信息

#### 5. `GET /admin/me`

- 用途：获取当前管理员信息
- 鉴权：是，且必须是管理员
- 请求头：

```http
authorization: <token>
```

- 成功返回结构同 `GET /user/me`
- 常见失败：
  - `401`
  - `403`
- 前端触发场景：
  - 管理端登录成功后的身份确认
  - 管理端页面刷新后的身份恢复
  - 管理端“我的”页显示个人信息

### 商品浏览

#### 6. `GET /shop/products/category`

- 用途：按分类查询商品列表
- 鉴权：否
- query：

| 参数名 | 类型 | 必填 | 合法值 |
| --- | --- | --- | --- |
| `category` | `string` | 是 | `二手书` / `闲置物品` / `电子产品` / `日用品` |

- 成功返回：`ApiResult<ProductListItem[]>`
- 常见失败：
  - `invalid category`
- 前端触发场景：
  - 用户端商品浏览页首次进入
  - 切换分类标签

#### 7. `GET /shop/products/search`

- 用途：按关键词搜索商品
- 鉴权：否
- query：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | `string` | 是 | 匹配商品名称与标签名称 |

- 成功返回：`ApiResult<ProductListItem[]>`
- 常见失败：
  - `keyword cannot be blank`
- 前端触发场景：
  - 用户端商品浏览页点击搜索

#### 8. `GET /shop/products/{id}`

- 用途：获取商品详情
- 鉴权：否
- path：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `number` | 是 | 商品 ID |

- 成功返回：`ApiResult<ProductDetail>`
- 常见失败：
  - `invalid product id`
  - `product not found`
  - `save browse history failed`
- 前端触发场景：
  - 商品浏览页点击查看详情
  - 用户端我的页编辑已上架商品时，用于补齐部分字段

### 用户端商品管理

#### 9. `GET /user/product/mine`

- 用途：获取当前用户发布的商品列表
- 鉴权：是
- 成功返回：`ApiResult<MyProductItem[]>`
- 常见失败：
  - `user not logged in`
  - `401`
- 前端触发场景：
  - 用户端“我的”页初始化

#### 10. `POST /user/product`

- 用途：发布商品
- 鉴权：是
- body：

```json
{
  "category": "电子产品",
  "name": "keyboard",
  "imageUrls": [
    "https://img.example.com/1.jpg"
  ],
  "description": "desc",
  "price": 199.0,
  "wechat": "wx_001",
  "address": "dorm",
  "tags": "tag1 tag2"
}
```

- 成功返回：`ApiResult<number>`，`data` 为商品 ID
- 常见失败：
  - `request body cannot be null`
  - `invalid category`
  - `name cannot be blank`
  - `description cannot be blank`
  - `price must be greater than 0`
  - `imageUrls cannot be empty`
  - `user not logged in`
  - `create product failed`
- 前端触发场景：
  - 用户端发布页提交

#### 11. `PUT /user/product/{id}`

- 用途：更新商品
- 鉴权：是
- path：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `number` | 是 | 商品 ID |

- body：

```json
{
  "category": "电子产品",
  "name": "keyboard",
  "imageUrls": [
    "https://img.example.com/1.jpg"
  ],
  "description": "desc",
  "price": 199.0,
  "wechat": "wx_001",
  "address": "dorm",
  "tags": "tag1 tag2",
  "status": "已下架"
}
```

- 注意：
  - `status` 只允许 `已上架` 或 `已下架`
  - 这是全量更新接口，不是补丁接口
- 成功返回：`ApiResult<null>`
- 常见失败：
  - `invalid product id`
  - `request body cannot be null`
  - `invalid status`
  - `invalid category`
  - `name cannot be blank`
  - `description cannot be blank`
  - `price must be greater than 0`
  - `imageUrls cannot be empty`
  - `user not logged in`
  - `product not found`
  - `update product failed`
- 前端触发场景：
  - 用户端“我的”页编辑抽屉提交

### 举报与审核

#### 12. `POST /user/report`

- 用途：举报商品
- 鉴权：是
- body：

```json
{
  "productId": 9,
  "reason": "fake item"
}
```

- 成功返回：`ApiResult<number>`，`data` 为举报记录 ID
- 常见失败：
  - `request body cannot be null`
  - `invalid product id`
  - `reason cannot be blank`
  - `user not logged in`
  - `product not found`
  - `product is off shelf`
  - `product status cannot be reported`
  - `duplicate pending report`
  - `create report failed`
  - `update product status failed`
- 前端触发场景：
  - 用户端商品浏览页点击举报并提交

#### 13. `GET /admin/reports`

- 用途：获取举报列表
- 鉴权：是，且必须管理员
- 成功返回：`ApiResult<AdminReportRecord[]>`
- 常见失败：
  - `401`
  - `403`
  - `user not logged in`
  - `forbidden`
- 前端触发场景：
  - 管理端审核页初始化

#### 14. `PUT /admin/reports/{id}`

- 用途：处理举报并设置商品状态
- 鉴权：是，且必须管理员
- path：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `number` | 是 | 举报记录 ID |

- body：

```json
{
  "productStatus": "已下架"
}
```

- 注意：
  - `productStatus` 只允许：
    - `已上架`
    - `已下架`
- 成功返回：`ApiResult<null>`
- 常见失败：
  - `invalid report id`
  - `request body cannot be null`
  - `invalid product status`
  - `report not found`
  - `report already processed`
  - `product not found`
  - `update product status failed`
  - `update report status failed`
- 前端触发场景：
  - 管理端审核页点击“上架”或“下架”

---

## 7. 前端状态管理与鉴权策略

## 7.1 登录完成流程

### 用户端

1. 输入 `wechat`
2. 点击“获取验证码”，调 `POST /user/code`
3. 输入 `code`
4. 点击登录，调 `POST /user/login`
5. 保存返回 `token`
6. 立刻调用 `GET /user/me`
7. 校验 `role === 用户端`
8. 保存本地会话
9. 跳转 `/user/shop`

### 管理端

1. 输入 `wechat`
2. 点击“获取验证码”，调 `POST /user/code`
3. 输入 `code`
4. 点击登录，调 `POST /admin/login`
5. 保存返回 `token`
6. 立刻调用 `GET /admin/me`
7. 校验 `role === 管理员`
8. 保存本地会话
9. 跳转 `/admin/review`

## 7.2 页面刷新恢复

页面刷新后：

- 如果本地存在 token 和 role
- 则允许继续进入当前角色页面
- 同时可在应用初始化时调用对应 `/me` 做一次校验
- 若 `/me` 失败：
  - 清空本地登录态
  - 跳转 `/login`

## 7.3 请求拦截器

Axios 请求拦截器行为：

- 若本地有 token，则自动加请求头：

```http
authorization: <token>
```

Axios 响应拦截器行为：

- `401`：
  - 清空本地登录态
  - 跳转 `/login`
  - 提示“登录已失效，请重新登录”
- `403`：
  - 提示“无权限访问该页面”
- HTTP `200` 且 `code = 0`：
  - 按业务失败处理
  - 展示 `msg`

## 7.4 退出登录

当前后端没有退出登录接口。

前端退出登录只做本地操作：

- 清空 token
- 清空 role
- 清空 user 信息
- 跳转 `/login`

---

## 8. 视觉与交互规范

## 8.1 视觉方向

视觉方向固定为：

- 校园二手交易平台
- 明亮浅底
- 中性灰为基础色
- 深灰文字
- 绿色作为唯一主强调色
- 左侧导航稳定
- 主内容区高可读性
- 不做营销首页

### 禁止事项

- 不要保留默认 Vue 欢迎页视觉
- 不要使用紫色主色
- 不要做大面积无意义卡片堆叠
- 不要做营销风 hero
- 不要做与后台产品无关的炫技装饰

## 8.2 交互方向

- 登录模式切换要清晰
- 商品详情抽屉从右侧滑入
- 编辑抽屉从右侧滑入
- 举报使用轻量弹窗
- 提交成功/失败要有明确反馈
- 菜单切换稳定、克制

## 8.3 推荐组件

推荐优先使用 Element Plus 组件：

- `ElContainer`
- `ElAside`
- `ElHeader`
- `ElMain`
- `ElMenu`
- `ElTabs`
- `ElForm`
- `ElInput`
- `ElSelect`
- `ElButton`
- `ElCard`
- `ElDrawer`
- `ElDialog`
- `ElTable`
- `ElTag`
- `ElDescriptions`
- `ElMessage`
- `ElEmpty`

---

## 9. AI 实现约束

AI 在生成 `dbksf` 前端时必须遵守以下约束：

- 不要臆造后端接口
- 不要把 `wechat` 改成手机号字段
- 不要把验证码流程写成短信专用文案
- 不要实现文件上传
- 不要假设商品编辑前一定能拿到完整商品详情
- 不要把后端接口地址当成前端路由
- 不要把前端路由当成后端接口
- 不要引入 Pinia 作为必选方案
- 不要依赖分页
- 不要假设登录接口返回角色信息

## 9.1 推荐目录结构

```text
src/
  api/
    auth.js
    shop.js
    user.js
    admin.js
  router/
    index.js
  layouts/
    UserLayout.vue
    AdminLayout.vue
  views/
    LoginView.vue
    user/
      UserShopView.vue
      UserPublishView.vue
      UserMeView.vue
    admin/
      AdminReviewView.vue
      AdminMeView.vue
  components/
    ProductList.vue
    ProductDetailDrawer.vue
    ProductEditDrawer.vue
    ReportDialog.vue
    ImageUrlListInput.vue
  composables/
    useSession.js
    useAuthGuard.js
  utils/
    request.js
    storage.js
    format.js
```

## 9.2 推荐实现顺序

1. 先搭路由结构
2. 再写 Axios 请求层
3. 再写登录态组合式封装
4. 再写用户端/管理端布局壳层
5. 再写登录页
6. 再写商品浏览页
7. 再写发布页
8. 再写我的页
9. 最后写管理端审核页和管理端我的页

---

## 10. 联调矩阵

## 10.1 页面首次进入调用矩阵

| 页面 | 首次进入请求 |
| --- | --- |
| `/login` | 无 |
| `/user/shop` | `GET /shop/products/category?category=二手书` |
| `/user/publish` | 无 |
| `/user/me` | `GET /user/me` + `GET /user/product/mine` |
| `/admin/review` | `GET /admin/reports` |
| `/admin/me` | `GET /admin/me` |

## 10.2 页面动作与接口矩阵

| 页面 | 用户动作 | 对应接口 | 成功后行为 |
| --- | --- | --- | --- |
| `/login` | 获取验证码 | `POST /user/code` | 提示验证码已发送 |
| `/login` | 用户端登录 | `POST /user/login` -> `GET /user/me` | 跳 `/user/shop` |
| `/login` | 管理端登录 | `POST /admin/login` -> `GET /admin/me` | 跳 `/admin/review` |
| `/user/shop` | 切换分类 | `GET /shop/products/category` | 刷新列表 |
| `/user/shop` | 搜索 | `GET /shop/products/search` | 刷新列表 |
| `/user/shop` | 查看详情 | `GET /shop/products/{id}` | 打开详情抽屉 |
| `/user/shop` | 举报商品 | `POST /user/report` | 关闭弹窗并提示成功 |
| `/user/publish` | 提交发布 | `POST /user/product` | 跳 `/user/me` |
| `/user/me` | 加载个人信息 | `GET /user/me` | 展示信息 |
| `/user/me` | 加载我的商品 | `GET /user/product/mine` | 展示列表 |
| `/user/me` | 编辑已上架商品前补齐详情 | `GET /shop/products/{id}` | 填充编辑抽屉 |
| `/user/me` | 提交编辑 | `PUT /user/product/{id}` | 关闭抽屉并刷新 mine 列表 |
| `/admin/review` | 获取举报列表 | `GET /admin/reports` | 展示表格 |
| `/admin/review` | 上架/下架处理 | `PUT /admin/reports/{id}` | 重新拉取列表 |
| `/admin/me` | 加载管理员信息 | `GET /admin/me` | 展示信息 |

---

## 11. 验收场景

AI 生成前端后，至少要满足以下验收点。

### 11.1 登录与路由

- 用户端从 `/login` 成功登录后进入 `/user/shop`
- 管理端从 `/login` 成功登录后进入 `/admin/review`
- 用户角色访问管理端路由会被拦截
- 管理员角色访问用户端路由会被拦截
- 刷新页面后仍能按本地登录态恢复界面

### 11.2 用户端功能

- 商品浏览页能按分类拉取商品
- 商品浏览页能按关键词搜索商品
- 商品浏览页能打开详情抽屉
- 商品浏览页能对商品发起举报
- 发布页能提交 `imageUrls` 字符串数组
- 我的页能展示个人信息
- 我的页能展示我的商品
- 我的页能打开编辑抽屉并提交更新

### 11.3 管理端功能

- 审核页能展示举报列表
- 审核页每条举报支持上架/下架处理
- 管理端我的页能显示管理员信息

### 11.4 异常处理

- `401` 会清空登录态并跳回 `/login`
- `403` 会显示权限提示
- HTTP `200` 但 `code = 0` 时，会展示后端 `msg`
- 表单必填项缺失时会有前端校验提示

---

## 12. 结论

这份说明书对应的是“严格基于当前后端能力”的前端实现方案。

核心原则只有三条：

1. 前端页面路由和后端接口地址必须分开写
2. 前端能力必须服从当前后端真实能力
3. 当前后端缺口必须在前端方案里显式暴露，而不是被 AI 自行脑补

如果后续新增了真实短信接口、图片上传接口、我的商品详情接口或分页接口，需要再更新本说明书。
