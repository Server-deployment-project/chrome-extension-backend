#!/bin/bash

# ===================================
# 自动部署脚本 - Chrome Extension Backend
# 功能：构建镜像、启动容器、执行健康检查
# ===================================

set -e  # 任何命令失败立即退出

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# ===== 第一步：检查环境 =====
log_info "开始部署流程..."

# 检查 Docker
if ! command -v docker &> /dev/null; then
    log_error "Docker 未安装"
    exit 1
fi
log_info "Docker 版本：$(docker --version)"

# 检查 Docker Compose
if ! command -v docker-compose &> /dev/null; then
    log_error "Docker Compose 未安装"
    exit 1
fi
log_info "Docker Compose 版本：$(docker-compose --version)"

# 检查必要文件
if [ ! -f ".env" ]; then
    if [ ! -f ".env.production" ]; then
        log_error ".env 或 .env.production 文件不存在"
        exit 1
    fi
    log_warn ".env 不存在，复制 .env.production"
    cp .env.production .env
    chmod 600 .env
fi

if [ ! -f "Dockerfile" ]; then
    log_error "Dockerfile 不存在"
    exit 1
fi

if [ ! -f "docker-compose.yml" ]; then
    log_error "docker-compose.yml 不存在"
    exit 1
fi

# ===== 第二步：加载环境变量 =====
log_info "加载环境变量..."
export $(cat .env | grep -v '^#' | xargs)

if [ -z "$APP_VERSION" ]; then
    log_warn "未设置 APP_VERSION，使用默认值 1.0.0"
    APP_VERSION="1.0.0"
fi

log_info "应用版本：$APP_VERSION"

# ===== 第三步：代码更新（如果使用 Git） =====
if [ -d ".git" ]; then
    log_info "更新代码..."
    git pull origin main 2>/dev/null || log_warn "Git 更新失败，继续使用本地代码"
fi

# ===== 第四步：检查端口占用 =====
log_info "检查端口占用..."
if command -v ss &> /dev/null; then
    if ss -tulpn 2>/dev/null | grep -q ":${APP_PORT:-8000} "; then
        log_error "端口 ${APP_PORT:-8000} 已被占用"
        exit 1
    fi
elif command -v netstat &> /dev/null; then
    if netstat -tulpn 2>/dev/null | grep -q ":${APP_PORT:-8000} "; then
        log_error "端口 ${APP_PORT:-8000} 已被占用"
        exit 1
    fi
fi
log_info "端口 ${APP_PORT:-8000} 可用"

# ===== 第五步：构建镜像 =====
log_info "构建 Docker 镜像..."
if docker build -t chrome-extension-backend:${APP_VERSION} .; then
    log_info "镜像构建成功"
else
    log_error "镜像构建失败"
    exit 1
fi

# ===== 第六步：停止旧容器 =====
log_info "停止现有容器..."
docker-compose down 2>/dev/null || log_warn "没有运行的容器"

# ===== 第七步：启动新容器 =====
log_info "启动新容器..."
if docker-compose up -d; then
    log_info "容器启动成功"
else
    log_error "容器启动失败"
    exit 1
fi

# ===== 第八步：等待容器就绪 =====
log_info "等待服务就绪（最多60秒）..."
max_attempts=12
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if docker-compose ps | grep -q "backend.*Up.*healthy"; then
        log_info "后端服务已就绪"
        break
    fi
    
    attempt=$((attempt + 1))
    if [ $attempt -eq $max_attempts ]; then
        log_warn "后端服务未标记为健康，继续检查..."
        break
    fi
    
    sleep 5
done

# ===== 第九步：健康检查 =====
log_info "执行健康检查..."
health_check_passed=false
health_attempts=0
max_health_attempts=5

while [ $health_attempts -lt $max_health_attempts ]; do
    if curl -sf http://localhost:${APP_PORT:-8000}/api/v1/health > /dev/null 2>&1; then
        log_info "健康检查通过"
        health_check_passed=true
        break
    fi
    
    health_attempts=$((health_attempts + 1))
    if [ $health_attempts -lt $max_health_attempts ]; then
        sleep 5
    fi
done

if ! $health_check_passed; then
    log_warn "健康检查失败，检查容器日志..."
    docker-compose logs --tail=50 backend
fi

# ===== 第十步：显示部署状态 =====
log_info "===== 部署状态 ====="
docker-compose ps

# ===== 第十一步：记录部署 =====
log_info "记录部署信息..."
cat >> deploy-log.md << EOF

## 部署 $(date '+%Y-%m-%d %H:%M:%S')

- **版本**: $APP_VERSION
- **部署状态**: 成功
- **应用端口**: ${APP_PORT:-8000}
- **健康检查**: $([ "$health_check_passed" = true ] && echo "通过" || echo "失败")

EOF

log_info "部署完成！"
log_info "应用访问地址：http://localhost:${APP_PORT:-8000}"
log_info "API 文档：http://localhost:${APP_PORT:-8000}/swagger-ui.html"

exit 0
