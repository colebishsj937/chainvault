cd backend/code
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.5-tem"
export PATH="$JAVA_HOME/bin:$PATH"

# MySQL（3307 端口）
cd ../docker && docker compose up -d
docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < ../code/sql/init.sql

# 编译 & 启动（8080 被占用时可改端口）
cd ../code && mvn install -DskipTests
cd chainvault-gateway && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
cd chainvault-admin && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
# 接口文档（前端对接）
# 见 docs/API.md


RPC_PROXY_ENABLED=true RPC_PROXY_HOST=127.0.0.1 RPC_PROXY_PORT=7897 mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082