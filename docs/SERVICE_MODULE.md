# Service Module — AgriOS × Chatwoot

Sprint 40 v0.1 customer service layer for AgriOS.

## 战略定位 (2026-05 决策)

**当前** (Sprint 40 MVP): Chatwoot 仅作为 AgriOS 的一个模块, 服务 Albert's Farm 单一客户,
目的是验证业务价值。不 fork Chatwoot, 不改 Chatwoot 代码, 持续跟随上游 MIT 版本。

**未来** (条件触发): 当 AgriOS + 至少 1 个其他 vertical OS (SmartOS / TravelOS / LocalOS)
都成功接上 Chatwoot 桥接, 证明该模式可复制后, 升级为独立 **2Africa ServiceOS** 产品
(参见 `2Africa-ServiceOS-产品规划V1.0.docx`, 在 internal-docs-backup/)。

**本模块的设计原则** 已为未来留好接口:
- `service_contact_link.agrios_entity_type` 字段 vertical-agnostic
- `service_event_log` 不绑 vertical 业务语义
- Chatwoot 独立 container, 可以单独抽走

因此今天写的代码 = 未来 ServiceOS 的子集, 不需要返工。

### 部署形态决策 (2026-05-31)

| 模式 | 描述 | 当前 | 未来 |
|---|---|---|---|
| **1 · Embedded** | Chatwoot 随 vertical OS 部署 (同一 docker-compose) | ✅ Sprint 40 锁定 | 默认选项, offline-friendly |
| **2 · Hosted** | 云端 ServiceOS 多租户 Chatwoot, vertical OS 远程调用 | 暂不做 | 未来选项, 给在线场景 |
| **3 · Hybrid** | 客户安装时二选一 | 暂不做 | 终态, 通过 `agrios.chatwoot.base-url` 一行配置切换 |

**当前架构 (ChatwootClient + Webhook + 桥接表)** 是模式 1/2/3 的公共子集 — 未来升级只改 URL, 代码零改动。

## 架构

```
┌──────────────────────────────────────────────────────────┐
│  Chatwoot (MIT, 独立 stack)                              │
│  端口: 3000                                              │
│  - chatwoot-postgres (PG 16)                            │
│  - chatwoot-redis    (Redis 7)                          │
│  - chatwoot-web      (Rails web)                        │
│  - chatwoot-worker   (Sidekiq)                          │
└──────────────────────────────────────────────────────────┘
                          │ open API + webhook
                          ▼
┌──────────────────────────────────────────────────────────┐
│  AgriOS 后端 (Sprint 40c-d)                              │
│  - ChatwootClient      (REST 客户端)                     │
│  - ContactSyncService  (Customer → Contact 单向同步)     │
│  - WebhookController   (收 Chatwoot 事件)                │
│  - service_contact_link / service_event_log (桥接表)    │
└──────────────────────────────────────────────────────────┘
```

## 本地启动

```bash
cd backend
docker compose up -d chatwoot-postgres chatwoot-redis
# 等 30 秒, PG 起来再起 web (会跑 db:chatwoot_prepare 初始化 schema)
docker compose up -d chatwoot-web
docker compose logs -f chatwoot-web
# 看到 "Listening on http://0.0.0.0:3000" 后启动 worker
docker compose up -d chatwoot-worker
```

或者直接 `docker compose up -d` 一次起全部, Chatwoot 自己会按 depends_on 顺序启动 (web 内会等 DB ready 才迁移)。

## 首次登录

打开 http://localhost:3000

第一次访问会显示注册页 (因为 `ENABLE_ACCOUNT_SIGNUP=true`)。
注册:
- Account name: `AgriOS Service` (或随意)
- Your name: `Admin`
- Email: `admin@agrios.local`
- Password: 任意 ≥ 6 位

注册完进入 Chatwoot 主界面。**这个账号就是 super admin**, 之后再创建客服坐席账号。

注册完后建议把 `chatwoot.env` 里的 `ENABLE_ACCOUNT_SIGNUP=false` 关掉, 防止陌生人创建账号。

## 关键端口

| 端口 | 服务 | 用途 |
|---|---|---|
| 3000 | Chatwoot web | 客服坐席工作台 + agent 后台 |
| 3006 | AgriOS MySQL | AgriOS 主数据库 |
| 6379 | AgriOS Redis | AgriOS 缓存 (与 chatwoot-redis 不同 container) |
| 8080 | AgriOS backend | AgriOS API |
| 9000 | MinIO S3 API | AgriOS 文件存储 |
| 9001 | MinIO console | MinIO 后台 |

## 验证 Chatwoot 起来了

```bash
# 容器健康检查
docker compose ps chatwoot-web chatwoot-worker chatwoot-postgres chatwoot-redis

# 应该全部 healthy / running
# 如果 chatwoot-web 一直 starting, 看日志:
docker compose logs --tail=100 chatwoot-web

# 常见首次启动问题:
# - "PG::ConnectionBad" → postgres 还没起来, 等 30 秒重试
# - "Cannot translate" → DB schema 没初始化, 跑一次 db:chatwoot_prepare
# - "Sidekiq is not running" → 单独起 worker
```

## Sprint 40c 已完成 — Customer 同步 Chatwoot Contact

### 拿 Chatwoot API Token

1. 浏览器打开 http://localhost:3000, 用 admin 登录
2. 右下角头像 → **Profile Settings**
3. 找到 **Access Token** 一栏 — 一长串, 复制
4. 把这个 token 写到环境变量

### 配置 AgriOS 后端连 Chatwoot

修改 `backend/docker-compose.yml` 的 `backend` 服务环境变量, 或者用 shell 临时注入:

```powershell
# 方式 A: 临时 (终端关闭就失效)
$env:CHATWOOT_API_TOKEN = "你刚才复制的 token"
cd "C:\Claude Project\Albert's Farm\backend"
docker compose up -d backend

# 方式 B: 持久 - 编辑 docker-compose.yml 把 CHATWOOT_API_TOKEN 写死
# (生产环境用 .env 文件 + ${CHATWOOT_API_TOKEN} 占位符)
```

### 验证连通

```bash
# 用 admin 账号登录 AgriOS 拿 JWT (替换成你的 admin 账号密码)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

# 探活: 看 Chatwoot 是不是可达
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/service/health | jq

# 预期:
# {
#   "code": 200,
#   "data": {
#     "enabled": true,
#     "reachable": true,
#     "baseUrl": "http://chatwoot-web:3000",
#     "accountId": 1
#   }
# }
```

### 推一个客户进 Chatwoot

```bash
# 把 Customer.id=1 推过去
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/service/sync-customer/1 | jq

# 预期返回 syncStatus=ok + chatwootContactId=...
# 然后到 Chatwoot UI Contacts 页面应该能看到这个客户
```

### 批量推全部活跃客户

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/service/sync-customers-all | jq

# 预期: { total: 25, ok: 25, failed: 0 }
```

## 同步出问题怎么排查

| 现象 | 原因 | 排查 |
|---|---|---|
| `enabled=false` | 后端没收到 `CHATWOOT_API_TOKEN` | `docker exec toafrica-backend env \| grep CHATWOOT` |
| `enabled=true, reachable=false` | Token 错了 / Chatwoot 没起 | 在 Chatwoot UI 重新生成 token; `docker compose logs chatwoot-web` |
| `syncStatus=error` | Chatwoot API 拒了 | 看 `service_event_log` 表 `error_message` 列 |
| 推完 Chatwoot 看不到 | account_id 不对 | 默认 1; Chatwoot UI 看 URL `/app/accounts/{N}` 那个 N |

```sql
-- 看最近 20 条 sync 事件
SELECT event_type, result, error_message, created_at, agrios_entity_id
  FROM service_event_log
  ORDER BY id DESC LIMIT 20;

-- 看所有链接状态
SELECT agrios_entity_id, chatwoot_contact_id, sync_status, sync_error, last_synced_at
  FROM service_contact_link;
```

## Sprint 40d 已完成 — Chatwoot Webhook 反向接入

AgriOS 后端暴露一个公开端点接 Chatwoot 推送过来的事件 (对话创建/消息到达/对话解决/联系人增改), 全部写到 `service_event_log` 留痕。v0.1 只存证, Sprint 41 加业务联动。

### 在 Chatwoot 后台配置 Webhook

1. 浏览器开 http://localhost:3000 → 左下角齿轮 ⚙️ **Settings**
2. 左侧 **Integrations** → **Webhooks** → 右上角 **Add new webhook**
3. 填两个字段:
   - **Endpoint URL**: `http://toafrica-backend:8080/api/v1/service/webhook/chatwoot` (容器网络)
     - 如果 Chatwoot 不能解析容器名, 用宿主机 IP: `http://host.docker.internal:8080/api/v1/service/webhook/chatwoot`
   - **Subscriptions**: 勾这些 (最少集):
     - ✅ Conversation Created
     - ✅ Conversation Updated
     - ✅ Conversation Status Changed
     - ✅ Message Created
     - ✅ Contact Created
4. 保存

### 验证 webhook 端点可达 (不需要 Chatwoot, 直接 curl)

```powershell
# 探活 GET — 应该返回 ok=true
Invoke-RestMethod http://localhost:8080/api/v1/service/webhook/chatwoot | ConvertTo-Json

# 模拟 POST — 用我们假造的 message_created 事件
$fakeEvent = @{
  event = "message_created"
  id = 999
  content = "Hello from test"
  conversation = @{ id = 1 }
  contact = @{ id = 1 }
  account = @{ id = 1 }
  updated_at = "2026-05-31T12:00:00Z"
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method POST -ContentType "application/json" `
  -Body $fakeEvent `
  -Uri http://localhost:8080/api/v1/service/webhook/chatwoot | ConvertTo-Json
```

**预期**: `status=ok, event=message_created, logId=N`。

第二次粘贴同样 body 再 POST 一次, 应该看到 `status=duplicate` (幂等保护)。

### 查看 webhook 日志

```sql
SELECT id, event_type, agrios_entity_id, chatwoot_conversation_id, chatwoot_message_id,
       result, created_at
  FROM service_event_log
  WHERE event_type LIKE 'webhook.%'
  ORDER BY id DESC LIMIT 10;
```

或者直接看 backend 日志:

```powershell
docker compose logs --tail=30 backend | Select-String "Chatwoot webhook"
```

### 生产 — 加 HMAC 验签

```powershell
# 1. 生成 32 字节随机密钥
$secret = -join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })
Write-Host "Webhook secret: $secret"

# 2. 同时设置到 AgriOS 后端和 Chatwoot
$env:CHATWOOT_WEBHOOK_SECRET = $secret
docker compose up -d backend
# 3. Chatwoot 后台 → Webhooks → 编辑你刚建的那条 → HMAC 字段粘贴同一个值
```

设了密钥后, 没带正确签名的 POST 会被立刻拒绝 (返回 `status=ignored, reason=signature_mismatch`)。

## Sprint 40f 已完成 — Email Inbox + Claude AI Agent

### Part 1 · 配 Email Inbox (IMAP + SMTP)

> 用 Gmail + App Password 是最快上手方式。生产环境推荐企业邮箱 (Workspace / Outlook 365)。

#### 1. 准备 Gmail App Password

1. Gmail 账号必须开启两步验证
2. 打开 https://myaccount.google.com/apppasswords
3. 选 "Mail" + "Other (custom name)" 比如 "Chatwoot AgriOS"
4. Google 给你一个 16 位 App Password — 复制保存 (只显示一次)

#### 2. 在 Chatwoot 后台加 Email Inbox

1. 浏览器 http://localhost:3000 → Settings ⚙️ → **Inboxes** → **Add Inbox**
2. 选 **Email** 类型
3. **Inbox 配置**:
   - Inbox Name: `Albert's Farm Support`
   - Email channel address: `support@albertsfarm.example` (你的对外收信邮箱)
4. **IMAP** (收件):
   - Enable IMAP: ON
   - Address: `imap.gmail.com`
   - Port: `993`
   - Login: 你的 Gmail 完整地址
   - Password: 上面复制的 16 位 App Password
   - Enable SSL: ON
5. **SMTP** (发件):
   - Enable SMTP: ON
   - Address: `smtp.gmail.com`
   - Port: `587`
   - Login: 同 IMAP
   - Password: 同 App Password
   - SSL: STARTTLS
6. 保存
7. 接下来 Chatwoot 让你"Add agents to this inbox" — 选你自己

#### 3. 测试 Email Inbox

从另一个邮箱给 `support@albertsfarm.example` 发条邮件 (或者用 Gmail 给自己发也行)。1-2 分钟内, Chatwoot **Conversations** 页面应该出现新对话, Inbox 标记 "Albert's Farm Support"。

如果一直没出现, 看 Chatwoot 日志:
```bash
docker compose logs --tail=50 chatwoot-worker | grep -i "imap\|inbox"
```

### Part 2 · 配 AI Agent (Claude 或 OpenAI 二选一)

AgriOS 后端支持两种 LLM 提供商, 通过 `AI_AGENT_PROVIDER` 环境变量切换:
- `claude` — Anthropic Claude (Haiku / Sonnet / Opus)
- `openai` — OpenAI 官方 API, 或任何 OpenAI-compatible 网关 (Azure / Groq / OpenRouter / Together / Ollama / vLLM)

#### 路线 A · 用 Claude (默认)

1. https://console.anthropic.com → Settings → API Keys → **Create Key**
2. 复制 key (sk-ant-...)
3. 设环境变量:

```powershell
$env:AI_AGENT_ENABLED = "true"
$env:AI_AGENT_PROVIDER = "claude"
$env:ANTHROPIC_API_KEY = "sk-ant-..."
$env:AI_AGENT_MODEL = "claude-haiku-4-5"   # 可选, 默认就是它

# 同时保留之前已经设的 Chatwoot token
$env:CHATWOOT_API_TOKEN = "yjRErkEvDhPPfD9oNYgXFhTd"

cd "C:\Claude Project\Albert's Farm\backend"
docker compose up -d backend
Start-Sleep -Seconds 20
docker exec toafrica-backend env | Select-String "AI_AGENT|ANTHROPIC"
```

#### 路线 B · 用 OpenAI (如果你已经有 OpenAI 账号)

1. https://platform.openai.com/api-keys → **Create new secret key**
2. 复制 key (sk-...)
3. 设环境变量:

```powershell
$env:AI_AGENT_ENABLED = "true"
$env:AI_AGENT_PROVIDER = "openai"
$env:OPENAI_API_KEY = "sk-..."
$env:AI_AGENT_MODEL = "gpt-4o-mini"   # 性价比最高的 OpenAI 模型

# 同时保留之前已经设的 Chatwoot token
$env:CHATWOOT_API_TOKEN = "yjRErkEvDhPPfD9oNYgXFhTd"

cd "C:\Claude Project\Albert's Farm\backend"
docker compose up -d backend
Start-Sleep -Seconds 20
docker exec toafrica-backend env | Select-String "AI_AGENT|OPENAI"
```

#### 路线 C · 用 OpenAI 兼容代理 (Azure / OpenRouter / Groq / Ollama)

OpenAI API 接口被很多服务商兼容, 配 base-url 就能走它们:

```powershell
$env:AI_AGENT_ENABLED = "true"
$env:AI_AGENT_PROVIDER = "openai"

# Azure OpenAI 示例:
$env:OPENAI_API_KEY = "你的 Azure key"
$env:OPENAI_BASE_URL = "https://YOUR-RESOURCE.openai.azure.com/openai/deployments/YOUR-DEPLOYMENT"
$env:AI_AGENT_MODEL = "gpt-4o"

# OpenRouter 示例 (可以接 100+ 模型, 包括 Claude/Llama/Gemini):
$env:OPENAI_API_KEY = "你的 OpenRouter key (sk-or-...)"
$env:OPENAI_BASE_URL = "https://openrouter.ai/api"
$env:AI_AGENT_MODEL = "anthropic/claude-haiku-4-5"

# Ollama 本地大模型示例 (零成本但慢):
$env:OPENAI_API_KEY = "ollama"   # Ollama 不校验 key, 任意值
$env:OPENAI_BASE_URL = "http://host.docker.internal:11434"
$env:AI_AGENT_MODEL = "llama3.1:8b"

cd "C:\Claude Project\Albert's Farm\backend"
docker compose up -d backend
```

#### 切换 provider 是 0 改代码

代码层面用 `LlmRouter` 路由 — 重启时根据 `AI_AGENT_PROVIDER` 选实现。切换:
1. 改 env 变量 `AI_AGENT_PROVIDER` 和对应的 key
2. `docker compose up -d backend`
3. 完事

#### 3. AI Agent 模式 — 私信 vs 公开回复

`AI_AGENT_REPLY_PUBLIC=false` (默认):
- AI 把回复写成 **Chatwoot 私信** (private note), 只有客服看得到
- 客服可以看、编辑、抄到正式回复栏发出
- "Human in the loop", 安全
- v0.1 推荐这个模式

`AI_AGENT_REPLY_PUBLIC=true`:
- AI 直接以**外发消息**身份回复给客户
- 全自动, 但有翻车风险
- 适合熟悉 prompt 调教后再切

切换:
```powershell
$env:AI_AGENT_REPLY_PUBLIC = "true"
docker compose up -d backend
```

#### 4. 测试 — 客户发条消息看 AI 回复

最简单: 在 Chatwoot UI 里给 Joy Food 发一条假"客户消息":

1. http://localhost:3000 → Contacts → Joy Food → **New conversation**
2. 选你刚建的 "Albert's Farm Support" inbox
3. 发一条: `Hi, do you have fresh tomatoes available this week?`
4. **关键**: Chatwoot 默认把你发的消息标成 outgoing。需要换"客户视角":
   - 或者用上一步真的从外部邮箱发邮件进来 (更真实)
   - 或者用 API 模拟 incoming message:
   ```powershell
   $cwHeader = @{ "api_access_token" = "yjRErkEvDhPPfD9oNYgXFhTd"; "Content-Type" = "application/json" }
   $msgBody = @{ content = "Hi, do you have fresh tomatoes available this week?"; message_type = "incoming" } | ConvertTo-Json
   # 替换 CONV_ID 为你刚建的对话 id (从 URL 看)
   Invoke-RestMethod -Method POST -Headers $cwHeader -Body $msgBody `
     -Uri http://localhost:3000/api/v1/accounts/1/conversations/CONV_ID/messages
   ```

#### 5. 验证 AI 反应

看 AgriOS backend 日志:
```powershell
docker compose logs --tail=30 backend | Select-String "AiAgent|Claude"
```

预期看到:
```
[Chatwoot webhook] persisted event=message_created ...
[Claude] ok in 800ms
[AiAgent] conversation=1 replied (public=false, 234 chars)
```

回 Chatwoot UI, 那条对话里应该多了一条**黄色私信**:
> 🤖 AI suggested reply (review before sending):
>
> Hello! Thank you for reaching out to Albert's Farm. We do have fresh tomatoes available this week. Let me confirm exact quantities and pricing with our team — could you please let me know how much you'd like to order?

看 `service_event_log` 审计:
```sql
SELECT event_type, result, JSON_EXTRACT(payload, '$.preview') AS preview, created_at
  FROM service_event_log
  WHERE event_type IN ('webhook.message_created', 'ai.reply')
  ORDER BY id DESC LIMIT 10;
```

### 排查

| 现象 | 原因 | 修法 |
|---|---|---|
| `[AiAgent] disabled — skipping` | env 没设 / 设错了 | 重设 `AI_AGENT_ENABLED=true` + key, 重启 |
| `Claude API HTTP 401` | API key 失效 | 重新 console.anthropic.com 拿新 key |
| `Claude API HTTP 429` | rate limit | 等 60 秒重试, 或升 plan |
| AI 回复语气不对 | system prompt 默认是英文 + Albert's Farm | 改 `agrios.chatwoot.ai-agent.system-prompt` |
| AI 回复变成公开发了 | `AI_AGENT_REPLY_PUBLIC=true` | 改回 false 重启 |

### 调 Prompt (可选)

system prompt 默认在 `application.yml` 里, 但太长不好维护。改用环境变量:

```powershell
$env:CHATWOOT_AI_AGENT_SYSTEM_PROMPT = @"
You are a sales assistant for Albert's Farm, a Kenyan farm in Kiambu County.
We grow tomatoes, kale, spinach, capsicum, and avocados.
Reply in 2-3 short sentences. Always end by asking what quantity they need.
If they ask about prices or delivery, say a sales rep will confirm and follow up within 1 hour.
"@
docker compose up -d backend
```

## 之后做什么 (Sprint 40g)

- **40g**: Albert's Farm 试点上线 + 整库 commit + 打 v3.1.0-rc1 tag

## 关闭服务

```bash
# 停止但保留数据
docker compose stop chatwoot-web chatwoot-worker chatwoot-postgres chatwoot-redis

# 完全删除 (包括聊天历史 — 谨慎!)
docker compose down -v
```

## 生产环境注意

- `SECRET_KEY_BASE` 必须改成 64 字节随机串 (`openssl rand -base64 64`)
- `POSTGRES_PASSWORD` / `REDIS` 密码必须改
- `ENABLE_ACCOUNT_SIGNUP=false` (关闭公开注册)
- `FORCE_SSL=true` 配合 nginx + Let's Encrypt
- `ACTIVE_STORAGE_SERVICE=amazon` + 配 S3/MinIO 凭证
- SMTP 配真实邮箱出站

把生产配置写到 `chatwoot.env.prod` (已在 .gitignore), 部署时 `--env-file chatwoot.env.prod` 启动。
