# ---- ビルドステージ ----
FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app

# 依存定義ファイルをコピー（キャッシュ活用）
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# ソースコードをコピーしてビルド
COPY src/ src/
RUN gradle bootJar --no-daemon

# ---- 実行ステージ ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# タイムゾーンを日本時間に設定
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Tokyo /etc/localtime && \
    echo "Asia/Tokyo" > /etc/timezone && \
    apk del tzdata

# ビルド成果物をコピー
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
