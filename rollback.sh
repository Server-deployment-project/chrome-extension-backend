#!/bin/bash

# ===================================
# 回滚脚本 - Chrome Extension Backend
# 功能：回滚到指定版本
# ===================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# ===== 参数处理 =====
if [ $# -eq 0 ]; then
    log_info "=== 可用版本列表 ==="
    docker images | grep chrome-extension-backend || log_error "未找到任何已部署的版本"
    echo ""
    echo "用法: bash rollback.sh <VERSION>"
    echo "示例: bash rollback.sh 1.0.0"
    exit 0
fi

TARGET_VERSION=$1

log_info "开始回滚至版本 $TARGET_VERSION..."

# ===== 检查版本是否存在 =====
if ! docker images | grep "chrome-extension-backend.*$TARGET_VERSION" > /dev/null; then
    log_error "找不到版本 $TARGET_VERSION"
    exit 1
fi

log_info "找到目标版本：$TARGET_VERSION"

# ===== 备份当前配置 =====
if [ -f ".env" ]; then
    cp .env ".env.backup.$(date +%Y%m%d_%H%M%S)"
    log_info "环境变量已备份"
fi

# ===== 备份数据库 =====
if docker-compose ps mysql 2>/dev/null | grep -q "Up"; then
    log_info "开始备份数据库..."
    backup_file="backup_before_rollback_$(date +%Y%m%d_%H%M%S).sql"
    
    if docker-compose exec -T mysql mysqldump -u root -p`grep DB_PASSWORD .env | cut -d= -f2` chrome_extension_db > "$backup_file"; then
        log_info "数据库备份成功：$backup_file"
    else
        log_error "数据库备份失败，中止回滚"
        exit 1
    fi
fi

# ===== 更新版本配置 =====
log_info "更新应用版本..."
sed -i "s/^APP_VERSION=.*/APP_VERSION=$TARGET_VERSION/" .env

# ===== 更新 docker-compose.yml 中的镜像标签 =====
# 确保使用正确的镜像版本
if [ -f "docker-compose.yml" ]; then
    log_info "更新镜像标签..."
    sed -i "s|chrome-extension-backend:.*|chrome-extension-backend:$TARGET_VERSION|g" docker-compose.yml
fi

# ===== 停止当前容器 =====
log_info "停止当前容器..."
docker-compose down

# ===== 启动旧版本 =====
log_info "启动版本 $TARGET_VERSION..."
if docker-compose up -d; then
    log_info "容器启动成功"
else
    log_error "容器启动失败，尝试恢复..."
    exit 1
fi

# ===== 等待容器就绪 =====
log_info "等待服务就绪..."
sleep 10
docker-compose ps

# ===== 健康检查 =====
log_info "执行健康检查..."
export $(cat .env | grep -v '^#' | xargs)
if curl -sf http://localhost:${APP_PORT:-8000}/api/v1/health > /dev/null 2>&1; then
    log_info "健康检查通过"
else
    log_warn "健康检查失败，请检查容器日志"
    docker-compose logs --tail=50 backend
fi

# ===== 记录回滚 =====
log_info "记录回滚信息..."
cat >> deploy-log.md << EOF

## 回滚 $(date '+%Y-%m-%d %H:%M:%S')

- **版本**: $TARGET_VERSION
- **回滚状态**: 成功
- **数据库备份**: backup_before_rollback_$(date +%Y%m%d_%H%M%S).sql

EOF

log_info "===== 回滚完成 ====="
log_info "当前版本：$TARGET_VERSION"
docker-compose ps

exit 0
