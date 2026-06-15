#!/usr/bin/env bash
# 墨韵·智库 - 统一开发环境启动脚本
# 用法: ./scripts/dev.sh [portal|admin|server|all|status]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ROOT_DIR"

cmd="${1:-status}"

print_header() {
  echo "==============================================="
  echo "  墨韵·智库 (Moyun Wisdom) - 开发环境"
  echo "  命令: $cmd"
  echo "==============================================="
}

case "$cmd" in
  status)
    print_header
    echo "📂 项目结构:"
    echo "  - moyun-server    : Spring Boot 3 后端 (Java 21)"
    echo "  - moyun-admin-vue : 后台管理 (Vue 3 + Element Plus)"
    echo "  - moyun-portal    : 用户前台 (Vue 3 + TypeScript + Tailwind)"
    echo ""
    echo "🚀 快速命令:"
    echo "  ./scripts/dev.sh install  # 安装所有前端依赖"
    echo "  ./scripts/dev.sh portal   # 启动前台 (3000)"
    echo "  ./scripts/dev.sh admin    # 启动后台 (80)"
    echo "  ./scripts/dev.sh server   # 启动后端 (8080)"
    echo "  ./scripts/dev.sh all      # 同时启动三个项目"
    echo "  ./scripts/dev.sh stop     # 停止所有项目"
    echo "  ./scripts/dev.sh reset    # 清理并重置环境"
    echo ""
    echo "🌐 端口约定:"
    echo "  - 后端 API:  http://localhost:8080"
    echo "  - 前台 Portal: http://localhost:3000"
    echo "  - 后台 Admin:  http://localhost:80"
    ;;

  install)
    print_header
    echo "📦 安装前台依赖..."
    (cd moyun-portal && npm install --no-audit --no-fund)
    echo "📦 安装后台依赖..."
    (cd moyun-admin-vue && npm install --no-audit --no-fund)
    echo "✅ 所有依赖安装完成"
    ;;

  portal)
    print_header
    echo "🎨 启动用户前台 (端口: 3000)..."
    cd moyun-portal && npm run dev -- --host 0.0.0.0 --port 3000
    ;;

  admin)
    print_header
    echo "🔧 启动后台管理 (端口: 80)..."
    cd moyun-admin-vue && npm run dev -- --host 0.0.0.0 --port 80
    ;;

  server)
    print_header
    echo "☕ 启动后端服务 (需 Java 21 + Maven)..."
    cd moyun-server && mvn spring-boot:run
    ;;

  all)
    print_header
    echo "🚀 同时启动三个项目 (后台模式)..."
    mkdir -p logs
    (cd moyun-portal && nohup npm run dev -- --host 0.0.0.0 --port 3000 > ../logs/portal.log 2>&1 & echo "Portal PID: $!")
    (cd moyun-admin-vue && nohup npm run dev -- --host 0.0.0.0 --port 80 > ../logs/admin.log 2>&1 & echo "Admin PID: $!")
    echo "📝 前后端已后台启动，日志在 ./logs/"
    echo "   后端请单独执行: ./scripts/dev.sh server"
    ;;

  stop)
    print_header
    echo "🛑 停止所有开发服务..."
    pkill -f "vite" 2>/dev/null && echo "  ✓ Vite 已停止" || echo "  - Vite 未运行"
    pkill -f "spring-boot:run" 2>/dev/null && echo "  ✓ Spring Boot 已停止" || echo "  - Spring Boot 未运行"
    pkill -f "java.*moyun" 2>/dev/null && echo "  ✓ Java 进程已停止" || echo "  - Java 未运行"
    echo "✅ 完成"
    ;;

  reset)
    print_header
    echo "🧹 清理 node_modules 和构建产物..."
    rm -rf moyun-portal/node_modules moyun-portal/dist
    rm -rf moyun-admin-vue/node_modules moyun-admin-vue/dist
    rm -rf moyun-server/target
    echo "✅ 清理完成，可执行 ./scripts/dev.sh install 重新安装"
    ;;

  *)
    echo "未知命令: $cmd"
    echo "可用: status | install | portal | admin | server | all | stop | reset"
    exit 1
    ;;
esac
