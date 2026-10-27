#!/usr/bin/env bash
# 启动 MinIO（生产建议用 systemd/docker compose，本脚本用于首次部署）
set -euo pipefail

MINIO_ROOT_USER="${MINIO_ROOT_USER:-moyunadmin}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:?请设置强密码}"
MINIO_PORT="${MINIO_PORT:-9000}"
MINIO_CONSOLE_PORT="${MINIO_CONSOLE_PORT:-9001}"
DATA_DIR="${MINIO_DATA_DIR:-/data/minio}"
BUCKET="${MINIO_BUCKET_NAME:-moyun}"

mkdir -p "$DATA_DIR"

docker run -d --name moyun-minio \
  --restart unless-stopped \
  -p "${MINIO_PORT}:9000" -p "${MINIO_CONSOLE_PORT}:9001" \
  -v "${DATA_DIR}:/data" \
  -e "MINIO_ROOT_USER=${MINIO_ROOT_USER}" \
  -e "MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}" \
  minio/minio:latest server /data --console-address ":9001"

echo "等待 MinIO 启动..."
for i in $(seq 1 30); do
  if curl -sf "http://127.0.0.1:${MINIO_PORT}/minio/health/live" >/dev/null; then
    echo "MinIO 已就绪"
    break
  fi
  sleep 2
done

# 创建桶（幂等），需要 mc 客户端
if command -v mc >/dev/null 2>&1; then
  mc alias set local "http://127.0.0.1:${MINIO_PORT}" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"
  mc mb --ignore-existing "local/${BUCKET}"
  mc anonymous set none "local/${BUCKET}" || true
  echo "bucket=${BUCKET} 已就绪"
else
  echo "未找到 mc，请手动创建 bucket：${BUCKET}"
fi

echo "控制台：http://<服务器IP>:${MINIO_CONSOLE_PORT}"
echo "请把 MINIO_ENDPOINT=http://<内网IP>:${MINIO_PORT}、MINIO_ACCESS_URL=https://oss.<域名> 写入 prod.env"
