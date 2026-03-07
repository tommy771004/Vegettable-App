#!/usr/bin/env bash
# =============================================================================
# test-api.sh — 後端 API 全端點健康測試腳本
#
# 使用方式：
#   chmod +x scripts/test-api.sh
#   ./scripts/test-api.sh [BASE_URL]
#
# 範例：
#   ./scripts/test-api.sh https://your-backend.run.app
#
# 回傳碼：
#   0 = 所有測試通過
#   1 = 有端點異常（細節見輸出）
# =============================================================================

set -euo pipefail

BASE="${1:-https://ais-dev-gyv3my74fwisdg5piudwph-424197195798.asia-east1.run.app}"
API="$BASE/api/produce"
PASS=0; FAIL=0; WARN=0
DEVICE_ID="test-ci-$(date +%s)"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'; BOLD='\033[1m'

header() { echo -e "\n${BOLD}══════════════════════════════════════════${NC}\n$1\n${BOLD}══════════════════════════════════════════${NC}"; }
ok()   { echo -e "  ${GREEN}✓ PASS${NC}  $1"; ((PASS++)); }
fail() { echo -e "  ${RED}✗ FAIL${NC}  $1"; ((FAIL++)); }
warn() { echo -e "  ${YELLOW}⚠ WARN${NC}  $1"; ((WARN++)); }

# 輔助：執行 curl，回傳 "HTTP_CODE BODY"
api_get() {
    local url="$1"
    local token="${2:-}"
    local auth=""
    [[ -n "$token" ]] && auth="-H 'Authorization: Bearer $token'"

    resp=$(curl -s -w "\n%{http_code}" --max-time 15 \
        ${auth:+-H "Authorization: Bearer $token"} \
        "$url" 2>/dev/null) || { echo "000 "; return; }

    code=$(echo "$resp" | tail -1)
    body=$(echo "$resp" | head -1)
    echo "$code $body"
}

api_post() {
    local url="$1"
    local data="$2"
    local token="${3:-}"

    resp=$(curl -s -w "\n%{http_code}" --max-time 15 \
        -X POST \
        -H "Content-Type: application/json" \
        ${token:+-H "Authorization: Bearer $token"} \
        -d "$data" \
        "$url" 2>/dev/null) || { echo "000 "; return; }

    code=$(echo "$resp" | tail -1)
    body=$(echo "$resp" | head -1)
    echo "$code $body"
}

check_json_field() {
    local body="$1"; local field="$2"
    echo "$body" | grep -q "\"$field\"" && return 0 || return 1
}

# =============================================================================
header "🔐 Step 1: JWT 認證"
# =============================================================================

result=$(api_post "$BASE/auth/token" "{\"deviceId\":\"$DEVICE_ID\"}")
code=$(echo "$result" | cut -d' ' -f1)
body=$(echo "$result" | cut -d' ' -f2-)

if [[ "$code" == "200" ]] && check_json_field "$body" "token"; then
    JWT=$(echo "$body" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    ok "POST /auth/token → JWT 取得成功（前 30 字）: ${JWT:0:30}..."
else
    fail "POST /auth/token → HTTP $code，無法繼續需要 JWT 的測試"
    JWT=""
    warn "部分測試將在無 JWT 的情況下執行（預期 401）"
fi

# =============================================================================
header "📊 Step 2: 批發價格查詢"
# =============================================================================

# 2a. 無關鍵字（第一頁）
result=$(api_get "$API/daily-prices?keyword=&page=1&pageSize=20" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
body=$(echo "$result" | cut -d' ' -f2-)
if [[ "$code" == "200" ]] && check_json_field "$body" "data"; then
    count=$(echo "$body" | grep -o '"id"' | wc -l)
    ok "GET /daily-prices (全部) → HTTP $code，取得 $count 筆"
else
    fail "GET /daily-prices (全部) → HTTP $code | $body"
fi

# 2b. 有關鍵字搜尋
result=$(api_get "$API/daily-prices?keyword=%E9%AB%98%E9%BA%97%E8%8F%9C&page=1&pageSize=5" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
body=$(echo "$result" | cut -d' ' -f2-)
if [[ "$code" == "200" ]]; then
    ok "GET /daily-prices?keyword=高麗菜 → HTTP $code"
else
    fail "GET /daily-prices?keyword=高麗菜 → HTTP $code | $body"
fi

# 2c. 分頁：第 2 頁
result=$(api_get "$API/daily-prices?keyword=&page=2&pageSize=20" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
if [[ "$code" == "200" ]]; then
    ok "GET /daily-prices?page=2 → HTTP $code"
else
    warn "GET /daily-prices?page=2 → HTTP $code（可能資料不足兩頁）"
fi

# =============================================================================
header "⚠️  Step 3: 異常價格偵測"
# =============================================================================

result=$(api_get "$API/anomalies" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
if [[ "$code" == "200" ]]; then
    ok "GET /anomalies → HTTP $code"
elif [[ "$code" == "401" ]]; then
    [[ -z "$JWT" ]] && warn "GET /anomalies → 401（需要 JWT，已跳過）" || fail "GET /anomalies → 401（JWT 無效）"
else
    fail "GET /anomalies → HTTP $code"
fi

# =============================================================================
header "🏆 Step 4: 今日交易量前 10 名"
# =============================================================================

result=$(api_get "$API/top-volume" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
body=$(echo "$result" | cut -d' ' -f2-)
if [[ "$code" == "200" ]]; then
    count=$(echo "$body" | grep -o '"id"' | wc -l)
    ok "GET /top-volume → HTTP $code，取得 $count 筆"
    [[ $count -gt 10 ]] && warn "top-volume 回傳 $count 筆（預期 ≤10）"
else
    fail "GET /top-volume → HTTP $code | $body"
fi

# =============================================================================
header "📈 Step 5: 歷史價格（需先取得 produceId）"
# =============================================================================

# 從 daily-prices 取得第一筆 produceId
result=$(api_get "$API/daily-prices?keyword=&page=1&pageSize=1" "$JWT")
body=$(echo "$result" | cut -d' ' -f2-)
PRODUCE_ID=$(echo "$body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

if [[ -n "$PRODUCE_ID" ]]; then
    result=$(api_get "$API/history/$PRODUCE_ID" "$JWT")
    code=$(echo "$result" | cut -d' ' -f1)
    if [[ "$code" == "200" ]]; then
        ok "GET /history/$PRODUCE_ID → HTTP $code"
    else
        fail "GET /history/$PRODUCE_ID → HTTP $code"
    fi

    result=$(api_get "$API/forecast/$PRODUCE_ID" "$JWT")
    code=$(echo "$result" | cut -d' ' -f1)
    if [[ "$code" == "200" ]]; then
        ok "GET /forecast/$PRODUCE_ID → HTTP $code"
    else
        fail "GET /forecast/$PRODUCE_ID → HTTP $code"
    fi
else
    warn "無法從 daily-prices 取得 produceId，跳過歷史/預測測試"
fi

# =============================================================================
header "❤️  Step 6: 收藏管理"
# =============================================================================

# 6a. 查看收藏
result=$(api_get "$API/favorites" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
if [[ "$code" == "200" ]]; then
    ok "GET /favorites → HTTP $code"
elif [[ "$code" == "401" ]]; then
    [[ -z "$JWT" ]] && warn "GET /favorites → 401（需要 JWT）" || fail "GET /favorites → 401（JWT 無效）"
else
    fail "GET /favorites → HTTP $code"
fi

# 6b. 新增收藏（若有 produceId 與 JWT）
if [[ -n "$JWT" && -n "$PRODUCE_ID" ]]; then
    result=$(api_post "$API/favorites" "{\"produceId\":\"$PRODUCE_ID\",\"targetPrice\":30.0}" "$JWT")
    code=$(echo "$result" | cut -d' ' -f1)
    if [[ "$code" == "200" || "$code" == "201" ]]; then
        ok "POST /favorites → HTTP $code（新增/更新收藏）"

        # 6c. 修改目標價
        upd=$(curl -s -w "\n%{http_code}" --max-time 15 \
            -X PUT -H "Content-Type: application/json" \
            -H "Authorization: Bearer $JWT" \
            -d '{"targetPrice":25.0}' \
            "$API/favorites/$PRODUCE_ID" 2>/dev/null)
        upd_code=$(echo "$upd" | tail -1)
        if [[ "$upd_code" == "200" ]]; then
            ok "PUT /favorites/$PRODUCE_ID → HTTP $upd_code（更新目標價）"
        else
            warn "PUT /favorites/$PRODUCE_ID → HTTP $upd_code"
        fi

        # 6d. 刪除收藏
        del=$(curl -s -w "\n%{http_code}" --max-time 15 \
            -X DELETE \
            -H "Authorization: Bearer $JWT" \
            "$API/favorites/$PRODUCE_ID" 2>/dev/null)
        del_code=$(echo "$del" | tail -1)
        if [[ "$del_code" == "200" ]]; then
            ok "DELETE /favorites/$PRODUCE_ID → HTTP $del_code"
        else
            warn "DELETE /favorites/$PRODUCE_ID → HTTP $del_code"
        fi
    else
        fail "POST /favorites → HTTP $code"
    fi
fi

# =============================================================================
header "🌿 Step 7: 當季農產品"
# =============================================================================

result=$(api_get "$API/seasonal" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
if [[ "$code" == "200" ]]; then
    ok "GET /seasonal → HTTP $code"
else
    fail "GET /seasonal → HTTP $code"
fi

# =============================================================================
header "🌧  Step 8: 天氣/颱風警報"
# =============================================================================

result=$(api_get "$API/weather-alerts" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
if [[ "$code" == "200" ]]; then
    ok "GET /weather-alerts → HTTP $code"
else
    fail "GET /weather-alerts → HTTP $code"
fi

# =============================================================================
header "🍳 Step 9: 省錢食譜推薦"
# =============================================================================

result=$(api_get "$API/budget-recipes" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
if [[ "$code" == "200" ]]; then
    ok "GET /budget-recipes → HTTP $code"
else
    fail "GET /budget-recipes → HTTP $code"
fi

# =============================================================================
header "👤 Step 10: 使用者貢獻統計"
# =============================================================================

result=$(api_get "$API/user-stats" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
body=$(echo "$result" | cut -d' ' -f2-)
if [[ "$code" == "200" ]] && check_json_field "$body" "contributionPoints"; then
    ok "GET /user-stats → HTTP $code（含 contributionPoints 欄位）"
elif [[ "$code" == "200" ]]; then
    warn "GET /user-stats → HTTP $code，但 body 格式異常: ${body:0:80}"
else
    fail "GET /user-stats → HTTP $code"
fi

# =============================================================================
header "🏪 Step 11: 市場比價"
# =============================================================================

CROP_NAME="%E9%AB%98%E9%BA%97%E8%8F%9C"  # 高麗菜 URL encoded
result=$(api_get "$API/compare/$CROP_NAME" "$JWT")
code=$(echo "$result" | cut -d' ' -f1)
body=$(echo "$result" | cut -d' ' -f2-)
if [[ "$code" == "200" ]]; then
    count=$(echo "$body" | grep -o '"marketName"' | wc -l)
    ok "GET /compare/高麗菜 → HTTP $code，$count 個市場"
else
    fail "GET /compare/高麗菜 → HTTP $code | ${body:0:80}"
fi

# =============================================================================
header "📣 Step 12: 社群物價回報"
# =============================================================================

if [[ -n "$JWT" ]]; then
    NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    result=$(api_post "$API/community-price" \
        "{\"produceId\":\"LA1\",\"cropName\":\"高麗菜\",\"marketName\":\"台北一市\",\"retailPrice\":28.5,\"priceType\":\"零售\",\"reportedAt\":\"$NOW\"}" \
        "$JWT")
    code=$(echo "$result" | cut -d' ' -f1)
    if [[ "$code" == "200" || "$code" == "201" ]]; then
        ok "POST /community-price → HTTP $code（回報成功）"
    else
        fail "POST /community-price → HTTP $code"
    fi
else
    warn "POST /community-price → 跳過（無 JWT）"
fi

# =============================================================================
header "📋 測試總結"
# =============================================================================
TOTAL=$((PASS + FAIL + WARN))
echo ""
echo -e "  總計 ${BOLD}$TOTAL${NC} 個測試"
echo -e "  ${GREEN}✓ 通過：$PASS${NC}"
echo -e "  ${RED}✗ 失敗：$FAIL${NC}"
echo -e "  ${YELLOW}⚠ 警告：$WARN${NC}"
echo ""

if [[ $FAIL -gt 0 ]]; then
    echo -e "${RED}❌ 有 $FAIL 個端點異常，請檢查後端日誌${NC}"
    exit 1
else
    echo -e "${GREEN}✅ 所有必要端點正常${NC}"
    exit 0
fi
