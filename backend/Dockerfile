# ============================================================================
# Multi-stage build for 2Africa AgriOS Backend
# ============================================================================

FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# ---------- Maven 镜像配置（针对非洲网络优化） ----------
# 主镜像: Maven Central (Sonatype 用 Fastly CDN, 在拉各斯/约堡/开普敦有 POP)
# 备镜像: Google Maven (Google Cloud CDN, 全球分布)
# 当主镜像超时/断连时, 自动 fallback 到备镜像
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>maven-central-fastly</id>
      <name>Maven Central via Fastly CDN</name>
      <url>https://repo1.maven.org/maven2/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>backup-repo</id>
      <repositories>
        <repository>
          <id>google-maven-mirror</id>
          <url>https://maven-central.storage-download.googleapis.com/maven2/</url>
          <releases><enabled>true</enabled></releases>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>backup-repo</activeProfile>
  </activeProfiles>
</settings>
EOF

# ---------- Maven JVM 参数: 重试 + 超时 + 连接池 ----------
# 关键: retryHandler.count=10 + readTimeout=10min, 抵御网络抖动
ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=10 \
                -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
                -Dmaven.wagon.http.pool=true \
                -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
                -Dmaven.wagon.http.connectionTimeout=120000 \
                -Dmaven.wagon.http.readTimeout=600000 \
                -Dhttp.keepAlive=true \
                -Dmaven.artifact.threads=4"

COPY pom.xml .
COPY src ./src
# 一步打包(去掉激进的 go-offline,只下载真实需要的依赖)
# -T 1C: 按 CPU 核数并行, 网络空闲时加速
# --mount=type=cache: 持久化 Maven 本地仓库, 源码改动不会触发依赖重下
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests -B -T 1C

# ============================================================================
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Dfile.encoding=UTF-8"

COPY --from=builder /build/target/agrios-backend.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
