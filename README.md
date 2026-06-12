# 线上购药系统

## 项目简介

线上购药系统（online-pharmacy-system）是一个基于 Java Spring Boot 的课程实践项目。项目模拟真实医药电商与在线咨询平台，完成药品信息、购物车与订单、智能药师咨询三个核心模块。

项目满足课程要求：

- 至少两个非智能化传统功能模块；
- 至少一个借助大模型实现的智能化应用模块；
- 使用 Maven 管理依赖，不提交第三方依赖目录；
- 从 GitHub 下载 ZIP 后可直接导入并运行。

## 技术栈

- Java 17
- Spring Boot 3
- Maven
- Spring MVC
- Spring Data JPA
- H2 内存数据库
- Thymeleaf
- Bootstrap 5
- Bootstrap Icons CDN
- 大模型接口：OpenAI Chat Completions 兼容格式，默认配置为 DeepSeek 地址

## 已完成功能模块

### 传统模块一：药品信息模块

- 药品分类展示
- 药品列表展示
- 药品关键词搜索
- 药品分类筛选
- 药品详情查看
- 药品库存展示
- OTC / 处方药标签展示

### 传统模块二：购物车与订单模块

- 加入购物车
- 查看购物车
- 修改购物车数量
- 删除购物车商品
- 创建订单
- 查看订单列表
- 查看订单详情
- 取消待支付订单
- 模拟支付

### 智能化模块三：智能药师咨询模块

- 用户输入症状、用药问题或健康咨询内容
- 后端生成 AI 药师回复
- 支持配置大模型 API
- 未配置 API Key 时使用本地模拟回复，保证项目可运行
- 高风险关键词识别
- 高风险问题提示转人工药师
- 保存咨询记录
- 查看历史咨询记录
- 所有 AI 回复均包含免责声明：“AI 回答仅供参考，不替代医生诊断或执业药师审核。”

## 项目目录结构

```text
online-pharmacy-system/
├─ pom.xml
├─ README.md
├─ .gitignore
├─ docs/
│  ├─ 模块开发说明.md
│  └─ 大模型应用记录.md
├─ src/main/java/com/example/pharmacy/
│  ├─ OnlinePharmacyApplication.java
│  ├─ common/
│  ├─ controller/
│  ├─ dto/
│  ├─ entity/
│  ├─ enums/
│  ├─ repository/
│  ├─ service/
│  └─ vo/
└─ src/main/resources/
   ├─ application.yml
   ├─ data.sql
   ├─ static/
   │  ├─ css/style.css
   │  └─ img/
   └─ templates/
```

## 启动方式

进入项目目录：

```bash
cd online-pharmacy-system
```

启动项目：

```bash
mvn spring-boot:run
```

默认端口为 `8080`。如果端口被占用，可以临时通过命令行参数指定其他端口，但 README 中的默认访问地址均以 `8080` 为准。

## 页面访问地址

```text
首页：http://localhost:8080
药品列表：http://localhost:8080/medicines
购物车：http://localhost:8080/cart
订单列表：http://localhost:8080/orders
智能药师：http://localhost:8080/ai-consult
咨询记录：http://localhost:8080/ai-consult/history
H2 控制台：http://localhost:8080/h2-console
```

## H2 数据库访问方式

访问：

```text
http://localhost:8080/h2-console
```

连接信息：

```text
JDBC URL: jdbc:h2:mem:pharmacy_db
User Name: sa
Password: 留空
```

## 大模型 API Key 配置方式

项目从环境变量读取 API Key，不要把真实 API Key 写入代码、README 或配置文件。

`application.yml` 中配置如下：

```yaml
llm:
  api-url: https://api.deepseek.com/chat/completions
  api-key: ${LLM_API_KEY:}
  model: ${LLM_MODEL:deepseek-v4-flash}
```

PowerShell 临时配置示例：

```powershell
$env:LLM_API_KEY="你的API_KEY"
$env:LLM_MODEL="deepseek-v4-flash"
mvn spring-boot:run
```

如果不配置 `LLM_API_KEY`，系统会自动使用本地模拟回复，智能药师模块仍然可以正常运行和保存咨询记录。

## 主要功能测试方式

1. 打开首页，进入药品列表。
2. 在药品列表中搜索“维生素”或选择“处方药”分类。
3. 进入药品详情页，查看规格、价格、库存、OTC / 处方药标签。
4. 选择非处方药加入购物车。
5. 进入购物车，修改数量或删除商品。
6. 点击去结算，提交订单。
7. 在订单详情页模拟支付或取消待支付订单。
8. 进入智能药师页面，输入普通咨询，例如“感冒鼻塞买药前需要注意什么？”。
9. 输入高风险咨询，例如“儿童高烧并且有青霉素过敏史，可以吃抗生素吗？”，检查是否出现高风险提示和转人工药师按钮。
10. 进入咨询记录页，检查历史咨询是否保存。

## 从 GitHub 下载 ZIP 并上传课程系统

1. 打开 GitHub 仓库地址：<https://github.com/wqljj666/shixun.git>
2. 点击页面右侧或上方的 `Code` 按钮。
3. 点击 `Download ZIP`。
4. 下载完成后得到项目压缩包。
5. 将 ZIP 上传到课程系统。

## 提交前排除的目录和文件

项目提交前应排除以下内容：

- `target/`
- `.idea/`
- `.vscode/`
- `node_modules/`
- `dist/`
- `build/`
- `.gradle/`
- `*.class`
- `*.log`
- `.DS_Store`
- `.env`
- `*.iml`
- 任何真实 API Key、Token 或密钥文件

## GitHub 仓库地址

```text
https://github.com/wqljj666/shixun.git
```
