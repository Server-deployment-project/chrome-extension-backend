#!/bin/bash

# ===================================
# 健康检查脚本 - Chrome Extension Backend
# 功能：检查各服务运行状态
# ===================================

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

check() {
    local name=$1
    local cmd=$2
    
    if eval "$cmd" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $name"
        return 0
    else
        echo -e "${RED}✗${NC} $name"
        return 1
    fi
}

echo -e "${BLUE}=== Chrome Extension Backend 服务检查 ===${NC}\n"

# 检查 Docker
echo "容器状态："
check "Docker 运行中" "docker ps > /dev/null"
check "docker-compose 可用" "docker-compose --version > /dev/null 2>&1"

# 检查容器
echo ""
echo "服务状态："
check "MySQL 容器运行中" "docker-compose ps mysql | grep -q Up"
check "Backend 容器运行中" "docker-compose ps backend | grep -q Up"

# 获取端口
PORT=$(grep "APP_PORT" .env 2>/dev/null | cut -d= -f2)
PORT=${PORT:-8000}

# 检查端口连接
echo ""
echo "后端服务："
check "后端监听在 $PORT 端口" "curl -s http://localhost:$PORT/api/v1/health > /dev/null"

# 检查数据库连接
echo ""
echo "数据库连接："
if docker-compose ps mysql | grep -q Up; then
    check "MySQL 数据库连接" "docker-compose exec -T mysql mysql -u root -p\$(grep DB_PASSWORD .env | cut -d= -f2) -e 'SELECT 1;' > /dev/null 2>&1"
    check "数据库初始化完成" "docker-compose exec -T mysql mysql -u root -p\$(grep DB_PASSWORD .env | cut -d= -f2) -e 'USE chrome_extension_db; SHOW TABLES;' > /dev/null 2>&1"
fi

# 显示容器资源使用
echo ""
echo "资源使用情况："
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep chrome-ext

# 显示日志概要
echo ""
echo "最近错误日志："
docker-compose logs --tail=10 backend 2>/dev/null | grep -i "error\|exception" | head -5 || echo -e "${GREEN}×${NC} 无错误日志"

echo ""
echo -e "${BLUE}=== 检查完成 ===${NC}"
