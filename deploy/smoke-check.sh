#!/usr/bin/env bash
# Checks a running tracker from the outside. Usage: deploy/smoke-check.sh [base-url]
# Fails if an answer is wrong or slower than MAX_SECONDS (default 3).

base="${1:-http://localhost:8080}"
base="${base%/}"
max="${MAX_SECONDS:-3}"
failed=0

# check <name> <expected status> <path>
check() {
    local name="$1" expected="$2" path="$3" result status seconds
    result=$(curl -s -o /dev/null -w '%{http_code} %{time_total}' --max-time 30 "$base$path") || result="000 0"
    status="${result% *}"
    seconds="${result#* }"
    if [ "$status" != "$expected" ]; then
        echo "FAIL  $name: expected $expected, got $status"
        failed=1
    elif awk -v s="$seconds" -v m="$max" 'BEGIN { exit !(s > m) }'; then
        echo "FAIL  $name: took ${seconds}s (limit ${max}s)"
        failed=1
    else
        echo "ok    $name (${seconds}s)"
    fi
}

check "sign-in page loads" 200 /login.html
check "signed-out page request goes to sign-in" 302 /
check "signed-out API request is refused" 401 /api/me

exit $failed
