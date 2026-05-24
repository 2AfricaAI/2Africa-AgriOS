# Albert's Farm Agri OS — Backend

> Phase 1 MVP · Spring Boot 3 + MyBatis-Plus + JWT + MySQL 8 + Redis 7

## 项目结构

```
backend/
├── docker-compose.yml          # 本地一键起 MySQL + Redis + MinIO
├── Dockerfile                  # 后端镜像构建
├── pom.xml                     # Maven 依赖
├── README.md
└── src/main/
    ├── java/com/albertsfarm/
    │   ├── AlbertsFarmApplication.java
    │   ├── common/                    # 统一响应/分页/异常
    │   │   ├── R.java
    │   │   ├── PageQuery.java
    │   │   ├── PageResult.java
    │   │   └── exception/
    │   ├── framework/                 # 框架层
    │   │   ├── config/                # MyBatis-Plus / Security / Swagger
    │   │   ├── security/              # JWT / LoginUser / SecurityUtil
    │   │   ├── annotation/            # @DataScope 数据范围
    │   │   └── aspect/                # AOP 切面
    │   ├── system/                    # 用户/角色/菜单/字典/认证
    │   │   ├── entity/  mapper/  service/  controller/
    │   ├── production/                # ✅ Plot 完整参考模块
    │   │   ├── entity/Plot.java
    │   │   ├── mapper/PlotMapper.java
    │   │   ├── service/PlotService.java
    │   │   ├── controller/PlotController.java
    │   │   └── dto/PlotDTO.java
    │   ├── harvest/                   # ⏳ 采收 + Batch
    │   ├── packhouse/                 # ⏳ 分级 + 包装 + 库存
    │   ├── sales/                     # ⏳ 客户 + 订单 + 出库
    │   └── mobile/                    # ⏳ 小程序专用接口
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        └── mapper/                    # MyBatis XML
```

## 快速开始

### 1. 启动依赖服务

```bash
cd backend
docker compose up -d
```

会自动拉起：
- **MySQL 8** :3306（库 `alberts_farm`，自动加载 `../schema.sql`）
- **Redis 7** :6379（密码 `redis123`）
- **MinIO**   :9000 / :9001（OSS 本地替代）

查看启动日志：
```bash
docker compose logs -f mysql
```

### 2. 验证数据库初始化

```bash
docker exec -it af-mysql mysql -uroot -proot123456 alberts_farm \
  -e "SHOW TABLES;"
```

预期看到 35 张表。

### 3. 启动后端

```bash
# 方式 A：本地直接跑
mvn spring-boot:run

# 方式 B：打包后运行
mvn clean package -DskipTests
java -jar target/alberts-farm-backend.jar

# 方式 C：Docker 容器化
docker build -t alberts-farm-backend .
docker run -p 8080:8080 --network host alberts-farm-backend
```

启动成功后访问：
- API Base: <http://localhost:8080/api>
- Swagger:  <http://localhost:8080/api/swagger-ui.html>
- Health:   <http://localhost:8080/api/actuator/health>

### 4. 管理员账号（已预置）

`schema.sql` 已写入真实可用的 BCrypt 哈希，直接登录即可：

| 字段 | 值 |
|------|----|
| 用户名 | `admin` |
| 密码（明文） | `Admin@123456` |
| 角色 | `SUPER_ADMIN` |
| BCrypt 哈希 | `$2b$10$GFHq9PcQS8SvCpf8pDczfuYnJUT0Nf.hBHNA3b6/7z5JPX4VC5srC` |

> 该哈希已用 Spring Security BCryptPasswordEncoder 等价算法验证通过（cost=10）。
> 生产环境上线前请通过 `UPDATE sys_user SET password=? WHERE id=1` 改为强密码。

如需重新生成（例如改新密码）：
```bash
# 任选其一
# Python
python3 -c "import bcrypt; print(bcrypt.hashpw(b'NewPwd@2026', bcrypt.gensalt(10)).decode())"

# Java
mvn -q exec:java -Dexec.mainClass=org.springframework.security.crypto.bcrypt.BCrypt
```

### 5. 测试登录

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123456"}'
```

预期返回：
```json
{
  "code": 200,
  "msg": "OK",
  "data": {
    "userId": 1,
    "username": "admin",
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "accessTokenExpiresIn": 7200,
    "roles": ["SUPER_ADMIN"],
    "permissions": []
  }
}
```

带 token 调用任意业务接口：
```bash
TOKEN="eyJhbGc..."
curl http://localhost:8080/api/v1/production/plots \
  -H "Authorization: Bearer $TOKEN"
```

### 6. 一键 e2e 冒烟测试

跑 `smoke-test.sh` 自动跑 8 项端到端测试（与沙箱里 Python 模拟版完全对应）：

```bash
bash smoke-test.sh
```

预期输出：
```
[1/8] ✓ docker compose services healthy
[2/8] ✓ 后端 /api/actuator/health 返回 UP
[3/8] ✓ 正确密码登录成功，拿到 accessToken
[4/8] ✓ 错误密码返回 401
[5/8] ✓ 不存在用户返回 401
[6/8] ✓ /api/v1/auth/me 用 token 访问成功
[7/8] ✓ 无 token 访问受保护接口返回 401
[8/8] ✓ sys_user.last_login_at 已更新

✅ 全部通过
```

## 已实现 vs 待开发

| 模块 | 状态 | 文件 |
|------|-----|-----|
| ✅ 统一响应 / 异常 / 分页 | 完成 | `common/` |
| ✅ JWT 认证 + 单点踢出 | 完成 | `framework/security/` |
| ✅ Spring Security + RBAC | 完成 | `SecurityConfig.java` |
| ✅ MyBatis-Plus + 乐观锁 + 自动填充 | 完成 | `MyBatisPlusConfig.java` |
| ✅ Swagger / OpenAPI | 完成 | 默认开启 |
| ✅ 用户登录 / 登出 / 当前用户 | 完成 | `AuthController.java` |
| ✅ **Plot 完整 CRUD（参考模板）** | 完成 | `production/` |
| ⏳ Planting Plan / Activity | 待开发 | `production/` |
| ⏳ Harvest + Batch 引擎 | 待开发 | `harvest/` |
| ⏳ Grading + Packing + Inventory | 待开发 | `packhouse/` |
| ⏳ Order + Fulfillment | 待开发 | `sales/` |
| ⏳ Trace 追溯查询 | 待开发 | 跨模块 |
| ⏳ 小程序专用接口 | 待开发 | `mobile/` |
| ⏳ 操作日志切面 | 待开发 | `framework/aspect/` |
| ⏳ 数据范围 AOP | 待开发 | `framework/aspect/` |

## 开发规范

### 新增业务模块 = 复制 `production/` 目录结构
1. 在 `entity/` 加实体类（继承自动填充字段约定）
2. 在 `mapper/` 加 Mapper 接口（继承 `BaseMapper<T>`）
3. 在 `resources/mapper/` 加 XML（复杂查询）
4. 在 `service/` 写业务逻辑（事务、状态机校验、留痕）
5. 在 `controller/` 暴露 REST + Swagger 注解 + `@PreAuthorize`
6. 在 `dto/` `vo/` 加请求 / 响应对象

### 编码规约
- 包名小写、类名 PascalCase、字段 camelCase
- 所有 Controller 方法返回 `R<T>` 或 `R<PageResult<T>>`
- 业务错误抛 `BusinessException`，由 `GlobalExceptionHandler` 统一处理
- 关键写操作必须 `@Transactional`
- 库存调整、出库等动作走 `inventory_adjust_log` 留痕
- 单元测试 ≥ 60% 行覆盖

### 提交规范
```
feat(plot): 实现地块详情统计接口
fix(inventory): 修复锁定库存并发更新乐观锁失效
docs(readme): 补充快速启动指引
```

## 风险点 & 注意事项

1. **schema.sql 路径**：docker-compose 用相对路径 `../schema.sql`，需保持目录结构
2. **JWT secret**：生产必须替换 `application.yml` 里的 `albertsfarm.jwt.secret`
3. **CORS**：生产环境收紧 `allowed-origins`，不要用通配符
4. **Redis 密码**：生产替换 `application-dev.yml` 里的密码
5. **MySQL CHECK 约束**：MySQL 8.0.16+ 强制生效，注意 `area_mu > 0` 等
6. **逻辑删除**：MyBatis-Plus 用 `deleted_at IS NULL` 区分；自定义 SQL 不要忘记加这个条件

## 排查问题

| 现象 | 原因 | 解决 |
|------|------|------|
| 启动报 `Communications link failure` | MySQL 未起来 | `docker compose ps` 确认状态 |
| 登录报 `用户名或密码错误` | admin 密码哈希未替换 | 见上文「创建管理员密码」 |
| 401 `未认证或 Token 已过期` | 没传 Authorization 或 Token 过期 | 重新登录获取新 token |
| 403 `权限不足` | 用户没有对应角色 | 给用户分配 `MANAGER` 角色 |
| 慢 SQL 告警 | 缺索引 | 检查 EXPLAIN 与 schema.sql 中索引定义 |

## 相关文档

- 产品需求：`../Albert's Farm 产品需求文档 PRD V1.0.docx`
- 技术设计：`../Albert's Farm 技术设计文档 V1.0.docx`
- 数据库：`../schema.sql`
- UI 原型：`../prototype.html`
