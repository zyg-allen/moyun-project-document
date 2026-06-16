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

check_db() {
  if ! command -v mysql &>/dev/null; then
    echo "⚠️  未找到 mysql 命令，请先安装 MySQL 客户端"
    return 1
  fi
  if ! mysql -u root -p123456 -e "USE moyun-db; SHOW TABLES;" &>/dev/null; then
    echo "⚠️  数据库 'moyun-db' 不存在或密码不对（当前默认 root/123456）"
    echo "   请先执行: ./scripts/dev.sh db-init"
    return 1
  fi
  echo "✅ 数据库 moyun-db 连接正常"
  return 0
}

case "$cmd" in
  status)
    print_header
    echo "📂 项目结构:"
    echo "  - moyun-server    : Spring Boot 3 后端 (Java 21)"
    echo "  - moyun-admin-vue : 后台管理 (Vue 3 + Element Plus)"
    echo "  - moyun-portal    : 用户前台 (Vue 3 + TypeScript + Tailwind)"
    echo ""
    echo "🚀 快速命令（建议执行顺序）:"
    echo "  1) ./scripts/dev.sh db-init  # 创建数据库 moyun-db & 执行 SQL（首次必跑）"
    echo "  2) ./scripts/dev.sh install  # 安装前端 npm 依赖"
    echo "  3) ./scripts/dev.sh server   # 启动后端 (http://localhost:8080)"
    echo "  4) ./scripts/dev.sh portal   # 启动门户前台 (http://localhost:3000)"
    echo "  5) ./scripts/dev.sh admin    # 启动后台管理 (http://localhost:80)"
    echo ""
    echo "其它命令:"
    echo "  ./scripts/dev.sh all        # 同时启动三个服务（后台模式）"
    echo "  ./scripts/dev.sh stop       # 停止所有开发服务"
    echo "  ./scripts/dev.sh reset      # 清理 node_modules 和构建产物"
    echo ""
    echo "🔐 默认账号:"
    echo "  - 后台管理: admin / admin123"
    echo "  - 数据库:   root / 123456 (库名: moyun-db)"
    echo ""
    echo "🌐 端口约定:"
    echo "  - 后端 API:  http://localhost:8080"
    echo "  - 门户 Portal: http://localhost:3000"
    echo "  - 后台 Admin:  http://localhost:80"
    echo "  - Swagger Doc: http://localhost:8080/doc.html"
    ;;

  db-init)
    print_header
    echo "🗄️  初始化数据库 moyun-db..."
    echo "   注：需本机安装 MySQL 8.0+ 且 root 密码为 123456"
    echo ""

    # 创建数据库
    echo "→ 创建数据库 moyun-db..."
    mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS moyun-db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" || {
      echo "❌ 数据库连接失败，请确认 MySQL 服务运行且 root 密码为 123456"
      echo "   如需修改密码，请编辑 moyun-server/src/main/resources/application-local.yaml"
      exit 1
    }
    echo "✅ 数据库 moyun-db 创建完成（或已存在）"

    # 执行基础初始化脚本
    echo ""
    echo "→ 执行基础初始化脚本（6 个）..."
    SQL_BASE="moyun-server/src/main/resources/sql"
    for f in 01_moyun_init.sql 02_workflow_init.sql 03_portal_init.sql 04_cms_menu_init.sql; do
      if [ -f "$SQL_BASE/$f" ]; then
        echo "   · 执行 $f ..."
        mysql -u root -p123456 moyun-db < "$SQL_BASE/$f" 2>/dev/null || echo "     ⚠️  $f 执行时忽略部分错误（可能表已存在）"
      else
        echo "   · 跳过 $f（文件不存在）"
      fi
    done
    echo "✅ 基础表完成"

    # 执行面试空间扩展模块脚本
    echo ""
    echo "→ 执行面试空间模块脚本（2 个）..."
    SQL_PORTAL="docs/sql"
    if [ -f "$SQL_PORTAL/portal_module_ddl_v2.sql" ]; then
      echo "   · 执行 portal_module_ddl_v2.sql (面试表 DDL)..."
      mysql -u root -p123456 moyun-db < "$SQL_PORTAL/portal_module_ddl_v2.sql"
      echo "   · 执行 portal_module_init_v2.sql (面试初始数据)..."
      mysql -u root -p123456 moyun-db < "$SQL_PORTAL/portal_module_init_v2.sql"
      echo "✅ 面试空间模块完成"
    else
      echo "⚠️  $SQL_PORTAL/portal_module_ddl_v2.sql 不存在，跳过面试模块"
    fi

    echo ""
    echo "🎉 数据库初始化完成！"
    echo "   验证命令: mysql -u root -p123456 moyun-db -e 'SHOW TABLES;'"
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
    check_db || exit 1
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
