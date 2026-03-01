# Chrome Extension Backend - Spring Boot

这是一个使用 Spring Boot WebFlux 构建的 Chrome 浏览器扩展后端服务，提供 AI 对话和图片理解功能。

## 技术栈

- **Java 17**
- **Spring Boot 3.2.2 (WebFlux)** - 响应式 Web 框架
- **MyBatis-Plus 3.5.5** - ORM 框架
- **MySQL** - 数据库
- **Maven** - 构建工具

## 项目结构

```
chrome-extension-backend/
├── src/
│   ├── main/
│   │   ├── java/com/extension/backend/
│   │   │   ├── ChromeExtensionBackendApplication.java  # 主启动类
│   │   │   ├── config/           # 配置类
│   │   │   ├── constant/         # 常量类
│   │   │   ├── controller/       # 控制器层
│   │   │   ├── dto/              # 数据传输对象
│   │   │   ├── entity/           # 实体类
│   │   │   ├── filter/           # 过滤器
│   │   │   ├── mapper/           # MyBatis Mapper
│   │   │   └── service/          # 业务逻辑层
│   │   └── resources/
│   │       ├── application.yml   # 配置文件
│   │       └── schema.sql        # 数据库初始化脚本
├── pom.xml                       # Maven 依赖配置
└── README.md
```

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 5.7+
- Maven 3.6+

### 2. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/schema.sql
```

或者直接导入：

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 3. 配置文件

编辑 `src/main/resources/application.yml`，修改数据库连接和 LLM API 配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chrome_extension_db?useSSL=false&serverTimezone=UTC
    username: root
    password: your_password

app:
  llm:
    api-key: ${LLM_API_KEY:your-api-key}
    api-base: ${LLM_API_BASE:https://api.siliconflow.cn/v1}
    default-model: ${LLM_DEFAULT_MODEL:Qwen/Qwen2.5-7B-Instruct}
    vision-model: ${LLM_VISION_MODEL:Qwen/Qwen2-VL-72B-Instruct}
```

### 4. 编译运行

```bash
# 编译项目
mvn clean package

# 运行项目
mvn spring-boot:run

# 或者运行打包后的 jar
java -jar target/chrome-extension-backend-1.0.0.jar
```

服务将在 `http://localhost:8000` 启动。

## API 接口

### 健康检查
- `GET /api/v1/health` - 服务健康检查

### 用户认证
- `POST /api/v1/register` - 用户注册
- `POST /api/v1/login` - 用户登录
- `POST /api/v1/reset-password` - 重置密码

### 用户配置
- `GET /api/v1/config` - 获取用户配置
- `POST /api/v1/config` - 更新用户配置

### AI 对话
- `POST /api/v1/chat` - 文本对话（流式响应）
- `POST /api/v1/vision` - 图片理解（流式响应）

### 历史记录
- `GET /api/v1/history` - 获取会话列表
- `GET /api/v1/history/{conversationId}` - 获取会话详情
- `DELETE /api/v1/history/{conversationId}` - 删除会话

## 认证机制

所有受保护的接口需要在请求头中携带 `X-Extension-Token`：

```
X-Extension-Token: your-token-here
```

## 环境变量

可以通过环境变量覆盖配置：

```bash
export DB_PASSWORD=your_db_password
export LLM_API_KEY=your_api_key
export LLM_API_BASE=https://api.siliconflow.cn/v1
```

## 开发说明

### 添加新的 API 接口

1. 在 `dto/` 目录下创建请求/响应 DTO
2. 在 `service/` 目录下实现业务逻辑
3. 在 `controller/` 目录下创建控制器

### 数据库变更

修改 `schema.sql` 后，需要重新执行初始化脚本或使用数据库迁移工具。

## 日志

日志配置在 `application.yml` 中，默认输出到控制台。可以通过以下配置调整日志级别：

```yaml
logging:
  level:
    com.extension.backend: DEBUG
```

## 故障排查

### 连接数据库失败
- 检查 MySQL 服务是否启动
- 确认数据库连接配置正确
- 验证用户名密码是否正确

### LLM API 调用失败
- 检查 API Key 是否有效
- 确认网络连接正常
- 查看日志中的详细错误信息

## License

MIT
