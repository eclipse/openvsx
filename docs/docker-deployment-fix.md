# Docker 部署问题修复文档

## 问题描述

在使用 Docker Compose 部署 OpenVSX 服务时遇到以下问题：
1. Server 服务启动失败（Exit 127）
2. Elasticsearch 健康检查不通过
3. 服务间依赖关系不正确

## 解决方案

### 1. 修改 docker-compose.yml 配置

主要修改包括：

```yaml
# 1. 更改 server 服务配置
server:
  image: openjdk:17-bullseye  # 使用完整版本的基础镜像
  working_dir: /app
  volumes:
    - ./server:/app
    - /etc/localtime:/etc/localtime:ro
  command: >
    bash -c '
      apt-get update && 
      apt-get install -y curl findutils &&
      scripts/generate-properties.sh --docker && 
      ./gradlew assemble && 
      ./gradlew runServer'
  ports:
    - "0.0.0.0:8080:8080"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - ELASTICSEARCH_HOST=elasticsearch
    - POSTGRES_HOST=postgres
  depends_on:
    elasticsearch:
      condition: service_healthy
    postgres:
      condition: service_started
  healthcheck:
    test: "curl --fail --silent localhost:8081/actuator/health | grep UP || exit 1"
    interval: 30s
    timeout: 10s
    retries: 5
    start_period: 40s

# 2. 优化 elasticsearch 服务配置
elasticsearch:
  image: elasticsearch:8.7.1
  environment:
    - xpack.security.enabled=false
    - xpack.ml.enabled=false
    - discovery.type=single-node
    - bootstrap.memory_lock=true
    - cluster.routing.allocation.disk.threshold_enabled=false
    - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
  volumes:
    - /etc/localtime:/etc/localtime:ro
  ports:
    - 9200:9200
    - 9300:9300
  ulimits:
    memlock:
      soft: -1
      hard: -1
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:9200/_cluster/health"]
    interval: 30s
    timeout: 10s
    retries: 5
    start_period: 40s
```

### 2. 主要改进点

1. Server 服务：
   - 使用 `openjdk:17-bullseye` 替代 `openjdk:17`，确保基础工具可用
   - 添加必要的工具安装（curl, findutils）
   - 添加环境变量配置
   - 优化健康检查参数
   - 改进服务依赖配置

2. Elasticsearch 服务：
   - 添加内存限制配置
   - 优化健康检查配置
   - 添加 ulimits 配置解决内存锁定问题

## 验证步骤

1. 检查服务状态：
```bash
docker-compose ps
```
确保所有服务状态正常：
- elasticsearch: Up (healthy)
- postgres: Up
- server: Up
- webui: Up

2. 验证 Elasticsearch：
```bash
curl http://localhost:9200/_cluster/health
```
应返回集群健康状态信息。

3. 验证 Server API：
```bash
curl http://localhost:8080/user
```
应返回 `{"error":"Not logged in."}` 表示服务正常。

4. 验证 Web UI：
访问 `http://YOUR_SERVER_IP:3000` 确保界面可以正常加载。

## 访问服务

服务启动后可通过以下地址访问：

1. Web UI 界面：
```
http://YOUR_SERVER_IP:3000
```

2. API 服务：
```
http://YOUR_SERVER_IP:8080
```

## 故障排查

如果服务未正常启动，可以通过以下命令查看日志：

```bash
# 查看所有服务状态
docker-compose ps

# 查看特定服务日志
docker-compose logs server
docker-compose logs elasticsearch

# 实时查看日志
docker-compose logs -f server
```

## 注意事项

1. 首次启动时，服务可能需要几分钟时间完成初始化
2. Elasticsearch 需要足够的内存来运行，建议至少 2GB 可用内存
3. 确保服务器防火墙允许访问相应端口（3000, 8080, 9200） 