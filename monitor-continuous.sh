#!/bin/bash

# ===================================
# 持续监控脚本 - Chrome Extension Backend
# 后台运行，定期检查服务状态
# ===================================

LOG_FILE="monitor.log"
CHECK_INTERVAL=60  # 每60秒检查一次

log_timestamp() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')]" "$@"
}

while true; do
    {
        log_timestamp "=== 监控检查 ==="
        
        # 检查容器状态
        log_timestamp "容器状态检查"
        if docker-compose ps | grep -q "chrome-ext-mysql.*Up.*healthy"; then
            log_timestamp "✓ MySQL 正常"
        else
            log_timestamp "✗ MySQL 异常"
        fi
        
        if docker-compose ps | grep -q "chrome-ext-backend.*Up.*healthy"; then
            log_timestamp "✓ Backend 正常"
        else
            log_timestamp "✗ Backend 异常"
        fi
        

        # 检查健康端点
        log_timestamp "API 健康检查"
        if curl -sf http://localhost:8000/api/v1/health > /dev/null 2>&1; then
            log_timestamp "✓ API 响应正常"
        else
            log_timestamp "✗ API 异常"
        fi
        
        # 检查资源使用
        log_timestamp "资源使用情况"
        docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | tail -n +2
        
        # 检查磁盘
        log_timestamp "磁盘空间"
        df -h | awk 'NR==2 {print $0}'
        
        echo ""
    } >> "$LOG_FILE" 2>&1
    
    # 等待后再检查
    sleep $CHECK_INTERVAL
done
