#!/bin/bash

# ===================================
# 初始化脚本 - Chrome Extension Backend
# 在服务器上首次运行此脚本自动设置权限和目录
# ===================================

echo "初始化部署环境..."

# 设置脚本执行权限
chmod +x deploy.sh
chmod +x rollback.sh
chmod +x health-check.sh
chmod +x monitor-continuous.sh 2>/dev/null || true

echo "✓ 脚本权限已设置"

# 创建所需目录
mkdir -p logs uploads backups
chmod 755 logs uploads backups

echo "✓ 目录已创建"

# 创建 .env（如果不存在）
if [ ! -f ".env" ]; then
    if [ -f ".env.production" ]; then
        cp .env.production .env
        chmod 600 .env
        echo "✓ 已从 .env.production 创建 .env"
        echo ""
        echo "⚠️  请立即编辑 .env 文件，修改以下必要参数："
        echo "   DB_PASSWORD=你的数据库密码"
        echo "   LLM_API_KEY=你的API密钥"
        echo ""
        echo "编辑命令：vim .env"
    else
        echo "❌ .env.production 不存在"
        exit 1
    fi
fi

# 检查关键文件
echo ""
echo "检查关键文件..."
files=(
    "pom.xml"
    "Dockerfile"
    "docker-compose.yml"

    "src/main/resources/schema.sql"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "❌ $file 不存在"
    fi
done

# 检查 Docker
echo ""
echo "检查容器环境..."
if command -v docker &> /dev/null; then
    echo "✓ Docker 已安装：$(docker --version)"
else
    echo "❌ Docker 未安装"
    exit 1
fi

if command -v docker-compose &> /dev/null; then
    echo "✓ Docker Compose 已安装：$(docker-compose --version)"
else
    echo "❌ Docker Compose 未安装"
    exit 1
fi

# 提示下一步
echo ""
echo "===== 初始化完成 ====="
echo ""
echo "后续步骤："
echo "1. 编辑 .env 文件，填入实际的数据库密码和 API 密钥"
echo ""
echo "   sudo vim .env"
echo ""
echo "2. 执行部署"
echo ""
echo "   bash deploy.sh"
echo ""
echo "3. 验证部署"
echo ""
echo "   docker-compose ps"
echo "   curl http://localhost:8000/api/v1/health"
echo ""
