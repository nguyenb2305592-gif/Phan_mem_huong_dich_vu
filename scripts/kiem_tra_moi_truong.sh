#!/usr/bin/env bash
# TH1-1: kiểm tra môi trường thực hành CT230 (Linux/macOS/WSL2)
set -u
ok=0
check() {
  local name="$1"; shift
  if out=$("$@" 2>&1 | head -1); then
    printf '  [OK]  %-16s %s\n' "$name" "$out"
  else
    printf '  [THIẾU] %-14s cài đặt rồi chạy lại\n' "$name"; ok=1
  fi
}
echo "== Kiểm tra môi trường CT230 v7 =="
check "JDK"            java -version
check "Maven wrapper"  ./mvnw -v
check "Docker"         docker version --format '{{.Server.Version}}'
check "Docker Compose" docker compose version
check "Git"            git --version
jv=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ "${jv:-0}" -lt 21 ]; then echo "  [LỖI] Cần JDK 21 trở lên (đang là $jv)"; ok=1; fi
# TODO(TH1-1): cảnh báo nếu các cổng 8080 8081 8082 8083 8090 8091 8180 8761 8888 đang bận
exit $ok
