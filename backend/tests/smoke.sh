#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8000/api}"
PASS=0
FAIL=0

CAT_ID=""
SUP_ID=""
PROD_ID=""

cleanup() {
    if [[ -n "$PROD_ID" ]]; then
        curl -s -X DELETE "$BASE_URL/products.php?id=$PROD_ID" >/dev/null || true
    fi
    if [[ -n "$SUP_ID" ]]; then
        curl -s -X DELETE "$BASE_URL/suppliers.php?id=$SUP_ID" >/dev/null || true
    fi
    if [[ -n "$CAT_ID" ]]; then
        curl -s -X DELETE "$BASE_URL/categories.php?id=$CAT_ID" >/dev/null || true
    fi
}
trap cleanup EXIT

request() {
    local method="$1"
    local path="$2"
    local body="${3:-}"

    if [[ -n "$body" ]]; then
        curl -fsS -X "$method" "$BASE_URL/$path" \
            -H 'Content-Type: application/json' \
            -d "$body"
    else
        curl -fsS -X "$method" "$BASE_URL/$path"
    fi
}

status_code() {
    local method="$1"
    local path="$2"
    local body="${3:-}"

    if [[ -n "$body" ]]; then
        curl -s -o /dev/null -w '%{http_code}' -X "$method" "$BASE_URL/$path" \
            -H 'Content-Type: application/json' \
            -d "$body"
    else
        curl -s -o /dev/null -w '%{http_code}' -X "$method" "$BASE_URL/$path"
    fi
}

json_field() {
    python3 -c '
import json
import sys

data = json.load(sys.stdin)
for key in sys.argv[1].split("."):
    if isinstance(data, list):
        data = data[int(key)]
    else:
        data = data[key]

if isinstance(data, bool):
    print("true" if data else "false")
elif data is None:
    print("null")
else:
    print(data)
' "$1"
}

check() {
    local label="$1"
    shift

    if "$@"; then
        echo "PASS: $label"
        PASS=$((PASS + 1))
    else
        echo "FAIL: $label"
        FAIL=$((FAIL + 1))
    fi
}

HEALTH=$(request GET health.php)
check "health returns ok" test "$(echo "$HEALTH" | json_field status)" = "ok"
check "health database connected" test "$(echo "$HEALTH" | json_field database)" = "connected"

DASH=$(request GET dashboard.php)
check "dashboard has total_products" test -n "$(echo "$DASH" | json_field total_products)"
check "dashboard has low_stock_count" test -n "$(echo "$DASH" | json_field low_stock_count)"

STAMP=$(date +%s)
CAT_NAME="Test Kategori $STAMP"
SUP_NAME="Test Supplier $STAMP"
PROD_NAME="TestProduk$STAMP"
UPDATED_NAME="${PROD_NAME}Updated"

CAT_JSON=$(request POST categories.php "{\"name\":\"$CAT_NAME\"}")
CAT_ID=$(echo "$CAT_JSON" | json_field id)
check "create category returns id" test -n "$CAT_ID"

SUP_JSON=$(request POST suppliers.php "{\"name\":\"$SUP_NAME\",\"phone\":\"0812000000\",\"address\":\"Test\"}")
SUP_ID=$(echo "$SUP_JSON" | json_field id)
check "create supplier returns id" test -n "$SUP_ID"

PROD_JSON=$(request POST products.php "{\"name\":\"$PROD_NAME\",\"category_id\":$CAT_ID,\"supplier_id\":$SUP_ID,\"stock\":2,\"min_stock\":5,\"price\":1500}")
PROD_ID=$(echo "$PROD_JSON" | json_field id)
check "create product returns id" test -n "$PROD_ID"

PRODUCTS=$(request GET products.php)
check "list products contains new product" grep -q "$PROD_NAME" <<<"$PRODUCTS"

FILTERED=$(request GET "products.php?category_id=$CAT_ID&supplier_id=$SUP_ID&stock_min=1&stock_max=10&search=$PROD_NAME")
check "combined filter returns product" grep -q "$PROD_NAME" <<<"$FILTERED"

LOW=$(request GET "products.php?low_stock=1")
check "low stock filter returns product" grep -q "$PROD_NAME" <<<"$LOW"

UPDATED=$(request PUT "products.php?id=$PROD_ID" "{\"name\":\"$UPDATED_NAME\",\"stock\":8}")
check "update product returns message" test "$(echo "$UPDATED" | json_field message)" = "Produk berhasil diperbarui"

UPDATED_LIST=$(request GET "products.php?search=$UPDATED_NAME")
check "updated product is searchable" grep -q "$UPDATED_NAME" <<<"$UPDATED_LIST"

check "duplicate category returns 409" test "$(status_code POST categories.php "{\"name\":\"$CAT_NAME\"}")" = "409"
check "negative stock returns 422" test "$(status_code POST products.php "{\"name\":\"Negative $STAMP\",\"stock\":-1}")" = "422"
check "invalid json returns 400" test "$(status_code POST categories.php '{bad')" = "400"
check "missing category id returns 404" test "$(status_code DELETE 'categories.php?id=999999')" = "404"

DEL_CAT=$(request DELETE "categories.php?id=$CAT_ID")
check "delete category returns message" test "$(echo "$DEL_CAT" | json_field message)" = "Kategori berhasil dihapus"
CAT_ID=""

AFTER_CAT=$(request GET "products.php?search=$UPDATED_NAME")
check "product survives category delete" grep -q "$UPDATED_NAME" <<<"$AFTER_CAT"
check "product category_id is null after category delete" grep -q '"category_id":null' <<<"$AFTER_CAT"

DEL_PROD=$(request DELETE "products.php?id=$PROD_ID")
check "delete product returns message" test "$(echo "$DEL_PROD" | json_field message)" = "Produk berhasil dihapus"
PROD_ID=""

DEL_SUP=$(request DELETE "suppliers.php?id=$SUP_ID")
check "delete supplier returns message" test "$(echo "$DEL_SUP" | json_field message)" = "Supplier berhasil dihapus"
SUP_ID=""

echo
echo "Passed: $PASS, Failed: $FAIL"
if [[ "$FAIL" -gt 0 ]]; then
    exit 1
fi
