# 项目接口梳理

本文档基于 `dbksh` 后端代码整理，当前统计到的现有接口共 `14` 个。

## 统一说明

### 1. 基础信息

- 服务端口：`8080`
- 全局上下文前缀：未发现额外 `context-path`，以下路径均为服务根路径下的相对路径
- 默认返回格式：除拦截器直接拦截外，控制器统一返回 JSON

### 2. 认证方式

- 登录成功后，接口返回一个字符串类型的 `token`
- 后续请求通过请求头传递：

```http
authorization: <token>
```

- 这里使用的是原始 token 字符串，不是 `Bearer <token>`

### 3. 鉴权规则

- 无需登录即可访问：
  - `POST /user/code`
  - `POST /user/login`
  - `POST /admin/login`
  - `/shop/**`
- 需要登录：
  - 其余 `/user/**`
- 需要管理员身份：
  - 除 `POST /admin/login` 以外的 `/admin/**`

### 4. 统一返回包装

所有正常进入控制器的方法，外层都使用 `Result<T>`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `integer` | `1` 表示成功，`0` 表示业务失败 |
| `msg` | `string \| null` | 错误信息；成功时通常为 `null` |
| `data` | `any` | 业务数据；无返回数据时通常为 `null` |

成功示例：

```json
{
  "code": 1,
  "msg": null,
  "data": "token_001"
}
```

失败示例：

```json
{
  "code": 0,
  "msg": "keyword cannot be blank",
  "data": null
}
```

### 5. 拦截器直接返回

以下情况不是 `Result<T>` 包装，而是被拦截器直接终止：

| HTTP 状态码 | 场景 | 返回体 |
| --- | --- | --- |
| `401` | 未登录访问需要登录的接口 | 通常为空 |
| `403` | 非管理员访问受保护的 `/admin/**` | 通常为空 |

---

## 一、用户端接口

### 1. 登录与用户信息

#### 1.1 `POST /user/code`

- 接口说明：发送微信登录验证码，验证码写入 Redis；开发环境下会打印到日志，便于联调。
- 是否需要登录：否
- 传入参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `wechat` | query / form | `string` | 是 | 微信号或登录标识 |

- 传入格式：`application/x-www-form-urlencoded`，也可通过 URL query 传递参数
- 返回值：`Result<Void>`
- 返回格式：JSON，对应 `Result<Void>`，成功时 `data` 为 `null`
- 常见返回：
  - 成功：`code = 1`
  - 失败：`msg = "wechat cannot be blank"`

#### 1.2 `POST /user/login`

- 接口说明：用户端登录接口，使用微信号 + 验证码登录。
- 是否需要登录：否
- 请求体参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `wechat` | body | `string` | 是 | 登录标识 |
| `code` | body | `string` | 是 | 验证码 |

- 传入格式：`application/json`

```json
{
  "wechat": "wechat_001",
  "code": "123456"
}
```

- 返回值：`Result<String>`
- 返回格式：JSON，对应 `Result<String>`，`data` 为登录 token
- 业务补充：
  - 若用户不存在，后端会自动创建新账号，角色固定为 `用户端`
  - 若该账号已存在但角色不是 `用户端`，会返回 `forbidden`
- 常见返回：
  - 成功：`data = "<token>"`
  - 失败：`msg` 可能为 `wechat cannot be blank`、`code cannot be blank`、`verification code error`、`create user failed`、`forbidden`

> 说明：测试代码里出现过 `password` 字段，但当前后端实际只校验 `wechat` 和 `code`。

#### 1.3 `GET /user/me`

- 接口说明：获取当前登录用户信息。
- 是否需要登录：是
- 传入参数：无
- 传入格式：无请求体，请求头需带 `authorization`
- 返回值：`Result<UserDTO>`
- 返回格式：JSON，对应 `Result<UserDTO>`
- `data` 字段结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `long` | 用户 ID |
| `username` | `string` | 用户名；新用户默认形如 `user_xxx` |
| `role` | `string` | 角色，常见值为 `用户端` 或 `管理员` |

---

### 2. 商城商品浏览

#### 2.1 `GET /shop/products/category`

- 接口说明：按商品分类查询已上架商品列表，按创建时间倒序返回。
- 是否需要登录：否
- 传入参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `category` | query | `string` | 是 | 商品分类 |

- 合法分类值：
  - `二手书`
  - `闲置物品`
  - `电子产品`
  - `日用品`
- 传入格式：URL query 参数，例如 `/shop/products/category?category=电子产品`
- 返回值：`Result<List<ProductListDTO>>`
- 返回格式：JSON，对应数组类型 `data`
- `data[i]` 字段结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `long` | 商品 ID |
| `name` | `string` | 商品名称 |
| `tags` | `string[]` | 商品标签列表 |
| `price` | `number` | 商品价格 |
| `imageUrl` | `string \| null` | 主图 URL，取排序最靠前的一张 |
| `relativeTime` | `string` | 相对发布时间，如 `刚刚`、`2分钟前`、`3天前` |
| `description` | `string` | 商品描述 |

- 常见返回：
  - 成功：`data` 为商品数组，可能为空数组
  - 失败：`msg = "invalid category"`

#### 2.2 `GET /shop/products/search`

- 接口说明：按关键字搜索已上架商品；会同时匹配商品名称和标签名称。
- 是否需要登录：否
- 传入参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `keyword` | query | `string` | 是 | 搜索关键字 |

- 传入格式：URL query 参数，例如 `/shop/products/search?keyword=耳机`
- 返回值：`Result<List<ProductListDTO>>`
- 返回格式：JSON，对应数组类型 `data`
- `data[i]` 字段结构：同上方 `ProductListDTO`
- 常见返回：
  - 成功：`data` 为商品数组，可能为空数组
  - 失败：`msg = "keyword cannot be blank"`

#### 2.3 `GET /shop/products/{id}`

- 接口说明：查看单个已上架商品详情。
- 是否需要登录：否；但如果请求头携带了有效 token，后端会额外记录一条浏览历史
- 路径参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | path | `long` | 是 | 商品 ID，必须大于 0 |

- 传入格式：无请求体
- 返回值：`Result<ProductDetailDTO>`
- 返回格式：JSON，对应 `Result<ProductDetailDTO>`
- `data` 字段结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `long` | 商品 ID |
| `name` | `string` | 商品名称 |
| `category` | `string` | 商品分类 |
| `imageUrls` | `string[]` | 商品详情图列表，按图片排序返回 |
| `description` | `string` | 商品描述 |
| `price` | `number` | 商品价格 |
| `wechat` | `string \| null` | 卖家微信 |
| `address` | `string \| null` | 交易地点 |

- 常见返回：
  - 成功：`data` 为商品详情对象
  - 失败：`msg` 可能为 `invalid product id`、`product not found`、`save browse history failed`

---

### 3. 我的商品管理

#### 3.1 `GET /user/product/mine`

- 接口说明：获取当前登录用户发布过的商品列表，包含商品当前状态。
- 是否需要登录：是
- 传入参数：无
- 传入格式：无请求体，请求头需带 `authorization`
- 返回值：`Result<List<MyProductListDTO>>`
- 返回格式：JSON，对应数组类型 `data`
- `data[i]` 字段结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `long` | 商品 ID |
| `name` | `string` | 商品名称 |
| `tags` | `string[]` | 标签列表 |
| `imageUrl` | `string \| null` | 主图 URL |
| `description` | `string` | 商品描述 |
| `status` | `string` | 商品状态，常见值为 `已上架`、`已下架`、`审核中` |

- 常见返回：
  - 成功：`data` 为商品数组，可能为空数组
  - 失败：`msg = "user not logged in"`，或被拦截器直接返回 `401`

#### 3.2 `POST /user/product`

- 接口说明：发布一个新商品。
- 是否需要登录：是
- 请求体参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `category` | body | `string` | 是 | 商品分类，只能是四个合法分类之一 |
| `name` | body | `string` | 是 | 商品名称 |
| `imageUrls` | body | `string[]` | 是 | 商品图片列表，至少 1 张 |
| `description` | body | `string` | 是 | 商品描述 |
| `price` | body | `number` | 是 | 商品价格，必须大于 0 |
| `wechat` | body | `string` | 否 | 联系微信；空白会被转成 `null` |
| `address` | body | `string` | 否 | 交易地点；空白会被转成 `null` |
| `tags` | body | `string` | 否 | 标签原始字符串，后端按空白字符切分，例如 `tag1 tag2 tag3` |

- 传入格式：`application/json`

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

- 返回值：`Result<Long>`
- 返回格式：JSON，对应 `Result<Long>`，`data` 为新建商品 ID
- 业务补充：
  - 新商品状态会被后端固定设置为 `已上架`
  - `tags` 会按空白拆分、去重；不存在的标签会自动创建
  - `imageUrls` 会按前端传入顺序保存，第一张会成为商品主图
- 常见返回：
  - 成功：`data = <productId>`
  - 失败：`msg` 可能为 `request body cannot be null`、`invalid category`、`name cannot be blank`、`description cannot be blank`、`price must be greater than 0`、`imageUrls cannot be empty`、`user not logged in`、`create product failed`

#### 3.3 `PUT /user/product/{id}`

- 接口说明：更新当前登录用户自己的商品。
- 是否需要登录：是
- 路径参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | path | `long` | 是 | 商品 ID，必须大于 0 |

- 请求体参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `category` | body | `string` | 是 | 商品分类 |
| `name` | body | `string` | 是 | 商品名称 |
| `imageUrls` | body | `string[]` | 是 | 商品图片列表，至少 1 张 |
| `description` | body | `string` | 是 | 商品描述 |
| `price` | body | `number` | 是 | 商品价格，必须大于 0 |
| `wechat` | body | `string` | 否 | 联系微信 |
| `address` | body | `string` | 否 | 交易地点 |
| `tags` | body | `string` | 否 | 标签字符串，按空白切分 |
| `status` | body | `string` | 是 | 商品状态，只允许 `已上架` 或 `已下架` |

- 传入格式：`application/json`

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

- 返回值：`Result<Void>`
- 返回格式：JSON，对应 `Result<Void>`，成功时 `data` 为 `null`
- 业务补充：
  - 这是“全量更新”，不是局部更新；基础字段仍然都要传
  - 只能修改自己的商品，其他人的商品会返回 `product not found`
  - 更新时会先删除旧图片和旧标签，再按新内容重建
- 常见返回：
  - 成功：`code = 1`
  - 失败：`msg` 可能为 `invalid product id`、`request body cannot be null`、`invalid status`、`invalid category`、`name cannot be blank`、`description cannot be blank`、`price must be greater than 0`、`imageUrls cannot be empty`、`user not logged in`、`product not found`、`update product failed`

---

### 4. 举报接口

#### 4.1 `POST /user/report`

- 接口说明：对商品发起举报。
- 是否需要登录：是
- 请求体参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `productId` | body | `long` | 是 | 被举报商品 ID，必须大于 0 |
| `reason` | body | `string` | 是 | 举报原因，空白不允许 |

- 传入格式：`application/json`

```json
{
  "productId": 9,
  "reason": "fake item"
}
```

- 返回值：`Result<Long>`
- 返回格式：JSON，对应 `Result<Long>`，`data` 为举报记录 ID
- 业务补充：
  - 只有状态为 `已上架` 或 `审核中` 的商品可以举报
  - 同一个用户对同一商品不能重复提交“待处理”举报
  - 如果商品当前是 `已上架`，首次举报成功后商品会自动转为 `审核中`
- 常见返回：
  - 成功：`data = <reportId>`
  - 失败：`msg` 可能为 `request body cannot be null`、`invalid product id`、`reason cannot be blank`、`user not logged in`、`product not found`、`product is off shelf`、`product status cannot be reported`、`duplicate pending report`、`create report failed`、`update product status failed`

---

## 二、管理端接口

### 1. 登录与管理员信息

#### 1.1 `POST /admin/login`

- 接口说明：管理端登录接口，使用微信号 + 验证码登录。
- 是否需要登录：否
- 请求体参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `wechat` | body | `string` | 是 | 登录标识 |
| `code` | body | `string` | 是 | 验证码 |

- 传入格式：`application/json`

```json
{
  "wechat": "admin_001",
  "code": "123456"
}
```

- 返回值：`Result<String>`
- 返回格式：JSON，对应 `Result<String>`，`data` 为登录 token
- 业务补充：
  - 该账号必须已存在
  - 该账号角色必须是 `管理员`
  - 不会自动创建管理员账号
  - 如果普通用户误走此接口，会返回 `forbidden`
- 常见返回：
  - 成功：`data = "<token>"`
  - 失败：`msg` 可能为 `wechat cannot be blank`、`code cannot be blank`、`verification code error`、`user not found`、`forbidden`

> 说明：当前项目没有单独的管理员验证码发送接口，管理员登录前同样通过 `POST /user/code` 获取验证码。

#### 1.2 `GET /admin/me`

- 接口说明：获取当前管理员自己的账户信息。
- 是否需要登录：是，且必须是管理员
- 传入参数：无
- 传入格式：无请求体，请求头需带 `authorization`
- 返回值：`Result<UserDTO>`
- 返回格式：JSON，对应 `Result<UserDTO>`
- `data` 字段结构：同上文 `GET /user/me`
- 常见返回：
  - 成功：`data` 为管理员信息
  - 失败：可能被拦截器直接返回 `401` 或 `403`

---

### 2. 举报管理

#### 2.1 `GET /admin/reports`

- 接口说明：管理员查看所有举报记录。
- 是否需要登录：是，且必须是管理员
- 传入参数：无
- 传入格式：无请求体，请求头需带 `authorization`
- 返回值：`Result<List<AdminReportDTO>>`
- 返回格式：JSON，对应数组类型 `data`
- 排序规则：
  - `待处理` 的举报优先
  - 同状态下按 `createTime` 倒序
- `data[i]` 字段结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `long` | 举报记录 ID |
| `userId` | `long` | 举报人 ID |
| `productId` | `long` | 商品 ID |
| `sellerId` | `long` | 卖家 ID |
| `reason` | `string` | 举报原因 |
| `status` | `string` | 举报状态，常见值为 `待处理`、`已处理` |
| `createTime` | `datetime` | 举报创建时间，代码类型为 `LocalDateTime` |
| `updateTime` | `datetime` | 举报更新时间，代码类型为 `LocalDateTime` |
| `productName` | `string \| null` | 商品名称 |
| `productStatus` | `string \| null` | 商品状态，常见值为 `已上架`、`已下架`、`审核中` |

- 常见返回：
  - 成功：`data` 为举报数组，可能为空数组
  - 失败：可能被拦截器直接返回 `401` 或 `403`；服务层也会返回 `msg = "user not logged in"` 或 `msg = "forbidden"`

#### 2.2 `PUT /admin/reports/{id}`

- 接口说明：管理员处理某条举报，并决定商品最终状态。
- 是否需要登录：是，且必须是管理员
- 路径参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | path | `long` | 是 | 举报记录 ID，必须大于 0 |

- 请求体参数：

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `productStatus` | body | `string` | 是 | 处理后商品状态，只允许 `已上架` 或 `已下架` |

- 传入格式：`application/json`

```json
{
  "productStatus": "已下架"
}
```

- 返回值：`Result<Void>`
- 返回格式：JSON，对应 `Result<Void>`，成功时 `data` 为 `null`
- 业务补充：
  - 如果该举报已经是 `已处理`，会直接返回失败
  - 成功处理后，会把该商品所有 `待处理` 举报统一更新为 `已处理`
  - 如果商品当前状态和传入状态不同，会同步更新商品状态
- 常见返回：
  - 成功：`code = 1`
  - 失败：`msg` 可能为 `invalid report id`、`request body cannot be null`、`invalid product status`、`report not found`、`report already processed`、`product not found`、`update product status failed`、`update report status failed`

---

## 三、补充结论

### 1. 当前接口分布

- 用户端：
  - 登录与用户：`/user/code`、`/user/login`、`/user/me`
  - 商城商品：`/shop/products/category`、`/shop/products/search`、`/shop/products/{id}`
  - 我的商品：`/user/product/mine`、`POST /user/product`、`PUT /user/product/{id}`
  - 举报：`POST /user/report`
- 管理端：
  - 登录与管理员信息：`POST /admin/login`、`GET /admin/me`
  - 举报管理：`GET /admin/reports`、`PUT /admin/reports/{id}`

### 2. 代码中值得注意的现状

- 登录入口已严格分流：
  - `POST /user/login` 只允许 `用户端`
  - `POST /admin/login` 只允许 `管理员`
- `PageResult` 已定义，但目前这些接口都没有使用分页返回
- 登录 token 的请求头名称固定为 `authorization`
