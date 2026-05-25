#!/usr/bin/env bash
# ============================================================================
# 2Africa AgriOS - 登录链路 e2e 冒烟测试
# 用法: bash smoke-test.sh
# 依赖: docker compose 已经 up,后端服务已经 mvn spring-boot:run
# ============================================================================

set -u
BASE="http://localhost:8080/api"
USER="admin"
PWD="Admin@123456"

# 颜色
G="\033[0;32m"; R="\033[0;31m"; Y="\033[0;33m"; NC="\033[0m"
PASS=0; FAIL=0
pass() { echo -e "${G}[$1/8] ✓ $2${NC}"; PASS=$((PASS+1)); }
fail() { echo -e "${R}[$1/8] ✗ $2${NC}"; FAIL=$((FAIL+1)); }

echo "════════════════════════════════════════════════════════════"
echo "  2Africa AgriOS /v1/auth/login 端到端冒烟测试"
echo "════════════════════════════════════════════════════════════"

# ---------- [1] docker services ----------
if docker compose ps --status running --format json 2>/dev/null | grep -q '"State":"running"'; then
  pass 1 "docker compose 服务运行中"
else
  fail 1 "docker compose 未启动 (运行: docker compose up -d)"
  exit 1
fi

# ---------- [2] backend health ----------
HEALTH=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE/actuator/health" 2>/dev/null || echo "000")
if [ "$HEALTH" = "200" ]; then
  pass 2 "后端 /actuator/health 返回 UP"
else
  fail 2 "后端无法访问 (HTTP $HEALTH) — 检查 mvn spring-boot:run 是否启动"
  exit 1
fi

# ---------- [3] 正确登录 ----------
RESP=$(curl -sS -X POST "$BASE/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"$PWD\"}")
CODE=$(echo "$RESP" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
TOKEN=$(echo "$RESP" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [ "$CODE" = "200" ] && [ -n "$TOKEN" ]; then
  pass 3 "正确密码登录成功,拿到 accessToken (长度 ${#TOKEN})"
else
  fail 3 "登录失败: $RESP"
  exit 1
fi

# ---------- [4] 错误密码 ----------
# 注: 项目采用 HTTP 200 + body code 表达业务错误,所以检查 body 的 code 字段
WRONG_RESP=$(curl -sS -X POST "$BASE/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"wrong\"}")
WRONG_CODE=$(echo "$WRONG_RESP" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
if [ "$WRONG_CODE" != "200" ] && [ -n "$WRONG_CODE" ]; then
  pass 4 "错误密码业务码=$WRONG_CODE (非 200,符合预期)"
else
  fail 4 "错误密码本应返回非 200 业务码, 实际 code=$WRONG_CODE"
fi

# ---------- [5] 不存在用户 ----------
NOEXIST_RESP=$(curl -sS -X POST "$BASE/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"ghost","password":"x"}')
NOEXIST_CODE=$(echo "$NOEXIST_RESP" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
if [ "$NOEXIST_CODE" != "200" ] && [ -n "$NOEXIST_CODE" ]; then
  pass 5 "不存在用户业务码=$NOEXIST_CODE (非 200,符合预期)"
else
  fail 5 "本应返回非 200 业务码, 实际 code=$NOEXIST_CODE"
fi

# ---------- [6] /me with token ----------
ME=$(curl -sS "$BASE/v1/auth/me" -H "Authorization: Bearer $TOKEN")
ME_CODE=$(echo "$ME" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
if [ "$ME_CODE" = "200" ]; then
  pass 6 "/v1/auth/me 用 token 访问成功"
else
  fail 6 "/me 失败: $ME"
fi

# ---------- [7] 无 token ----------
NO_AUTH=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE/v1/auth/me")
if [ "$NO_AUTH" = "401" ] || [ "$NO_AUTH" = "403" ]; then
  pass 7 "无 token 访问返回 HTTP $NO_AUTH"
else
  fail 7 "无 token 本应返回 401/403, 实际 $NO_AUTH"
fi

# ---------- [8] last_login_at 更新 ----------
LL=$(docker exec toafrica-mysql mysql -ualberts -palberts123 toafrica_agrios -sN \
     -e "SELECT last_login_at FROM sys_user WHERE id=1" 2>/dev/null)
if [ -n "$LL" ] && [ "$LL" != "NULL" ]; then
  pass 8 "sys_user.last_login_at 已更新: $LL"
else
  fail 8 "last_login_at 未更新"
fi

echo ""
echo "════════════════════════════════════════════════════════════"
if [ $FAIL -eq 0 ]; then
  echo -e "${G}  ✅ 全部 $PASS 项通过${NC}"
  exit 0
else
  echo -e "${R}  ❌ $FAIL 项失败 / $PASS 项通过${NC}"
  exit 1
fi
