# ---------- 1) Build stage ----------
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Bağımlılık cache'i için önce sadece pom + src yapısını kopyalayalım
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Uygulama kodu
COPY src ./src
# (Opsiyonel) testler uzun sürüyorsa -DskipTests
RUN mvn -q -e -DskipTests package

# ---------- 2) Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy

# Güvenlik: non-root user
RUN useradd -ms /bin/bash appuser
USER appuser

WORKDIR /app
# Uygulama jar'ını kopyala (target içindeki *-SNAPSHOT veya finalName'e göre eşle)
COPY --from=build /app/target/*.jar /app/app.jar

# Timezone (opsiyonel) — loglar için TR saati istersen aktif et
ENV TZ=Europe/Istanbul

# Konfigürasyon için environment değişkenleri (Mongo & Spring)
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/fxsmart

# JVM küçük tune'lar: konteyner farkındalığı + düşük hafıza izleri
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Healthcheck en altta (ENTRYPOINT'ten hemen önce veya sonra olabilir)
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD sh -c 'wget -qO- http://127.0.0.1:${SERVER_PORT}/actuator/health | grep -q "\"status\":\"UP\"" || exit 1'


ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]