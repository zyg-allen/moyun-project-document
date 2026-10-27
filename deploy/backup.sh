#!/usr/bin/env bash
# 每日备份：MySQL 全量 + MinIO 数据目录同步；保留 14 天
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-/data/backup}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-moyun-db2}"
MYSQL_USER="${MYSQL_USER:?}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?}"
MINIO_DATA_DIR="${MINIO_DATA_DIR:-/data/minio}"
KEEP_DAYS="${KEEP_DAYS:-14}"

STAMP=$(date +%F_%H%M)
DEST="${BACKUP_ROOT}/${STAMP}"
mkdir -p "$DEST"

echo "[1/3] MySQL dump..."
mysqldump --single-transaction --routines --triggers \
  -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
  "$MYSQL_DB" | gzip > "${DEST}/mysql-${MYSQL_DB}.sql.gz"

echo "[2/3] MinIO 数据同步..."
if [ -d "$MINIO_DATA_DIR" ]; then
  tar -czf "${DEST}/minio-data.tar.gz" -C "$(dirname "$MINIO_DATA_DIR")" "$(basename "$MINIO_DATA_DIR")"
else
  echo "跳过：$MINIO_DATA_DIR 不存在"
fi

echo "[3/3] 校验与清理..."
gzip -t "${DEST}/mysql-${MYSQL_DB}.sql.gz"
find "$BACKUP_ROOT" -maxdepth 1 -type d -mtime +"$KEEP_DAYS" -exec rm -rf {} +

echo "备份完成：${DEST}"
echo "建议：将 ${BACKUP_ROOT} 同步到异地对象存储（rclone/ossutil），并每月做一次恢复演练"
