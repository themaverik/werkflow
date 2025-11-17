# ================================================================
# WERKFLOW ENTERPRISE PLATFORM - MULTI-STAGE DOCKERFILE
# ================================================================
# This Dockerfile builds all services and frontends for the
# werkflow enterprise platform in a single, optimized build.
# ================================================================

# ================================================================
# STAGE 1: Backend Base - Common Maven dependencies
# ================================================================
FROM maven:3.9-eclipse-temurin-17 AS backend-base

WORKDIR /build

# Copy poms for shared libraries and services
COPY shared/delegates/pom.xml shared/delegates/pom.xml
COPY services/hr/pom.xml services/hr/pom.xml
COPY services/engine/pom.xml services/engine/pom.xml
COPY services/admin/pom.xml services/admin/pom.xml
COPY services/inventory/pom.xml services/inventory/pom.xml

# Download dependencies for shared libraries
RUN cd shared/delegates && mvn dependency:go-offline -B

# Download dependencies for all services
RUN cd services/hr && mvn dependency:go-offline -B
RUN cd services/engine && mvn dependency:go-offline -B
RUN cd services/admin && mvn dependency:go-offline -B
RUN cd services/inventory && mvn dependency:go-offline -B

# ================================================================
# STAGE 2: Delegates Library Build
# ================================================================
FROM backend-base AS delegates-build

WORKDIR /build/shared/delegates

# Copy source code
COPY shared/delegates/src ./src

# Build and install to local maven repo
RUN mvn clean install -DskipTests -B

# ================================================================
# STAGE 3: HR Service Build
# ================================================================
FROM backend-base AS hr-service-build

WORKDIR /build/services/hr

# Copy source code
COPY services/hr/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 4: Engine Service Build
# ================================================================
FROM backend-base AS engine-service-build

# Copy delegates library from delegates-build stage
COPY --from=delegates-build /root/.m2/repository/com/werkflow/werkflow-delegates /root/.m2/repository/com/werkflow/werkflow-delegates

WORKDIR /build/services/engine

# Copy source code
COPY services/engine/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 5: Admin Service Build
# ================================================================
FROM backend-base AS admin-service-build

WORKDIR /build/services/admin

# Copy source code
COPY services/admin/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 6: Frontend Base - Node.js dependencies
# ================================================================
FROM node:20-alpine AS frontend-base

WORKDIR /build

# Install pnpm for better monorepo support
RUN npm install -g pnpm

# ================================================================
# STAGE 7: Admin Portal Build
# ================================================================
FROM frontend-base AS admin-portal-build

WORKDIR /build/frontends/admin-portal

# Copy package files
COPY frontends/admin-portal/package.json frontends/admin-portal/package-lock.json* ./

# Install dependencies
RUN npm ci --only=production && npm cache clean --force

# Copy source code
COPY frontends/admin-portal/ ./

# Build the application
RUN npm run build

# ================================================================
# STAGE 8: HR Portal Build
# ================================================================
FROM frontend-base AS hr-portal-build

WORKDIR /build/frontends/hr-portal

# Copy package files
COPY frontends/hr-portal/package.json frontends/hr-portal/package-lock.json* ./

# Install dependencies
RUN npm ci --only=production && npm cache clean --force

# Copy source code
COPY frontends/hr-portal/ ./

# Build the application
RUN npm run build

# ================================================================
# STAGE 9: HR Service Runtime
# ================================================================
FROM eclipse-temurin:17-jre-alpine AS hr-service

# Add non-root user
RUN addgroup -S werkflow && adduser -S werkflow -G werkflow

WORKDIR /app

# Copy JAR from build stage
COPY --from=hr-service-build /build/services/hr/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs /app/hr-documents && \
    chown -R werkflow:werkflow /app

USER werkflow

EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "-jar", \
    "app.jar"]

# ================================================================
# STAGE 10: Engine Service Runtime
# ================================================================
FROM eclipse-temurin:17-jre-alpine AS engine-service

# Add non-root user
RUN addgroup -S werkflow && adduser -S werkflow -G werkflow

WORKDIR /app

# Copy JAR from build stage
COPY --from=engine-service-build /build/services/engine/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs /app/process-definitions && \
    chown -R werkflow:werkflow /app

USER werkflow

EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "-jar", \
    "app.jar"]

# ================================================================
# STAGE 11: Admin Service Runtime
# ================================================================
FROM eclipse-temurin:17-jre-alpine AS admin-service

# Add non-root user
RUN addgroup -S werkflow && adduser -S werkflow -G werkflow

WORKDIR /app

# Copy JAR from build stage
COPY --from=admin-service-build /build/services/admin/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs && \
    chown -R werkflow:werkflow /app

USER werkflow

EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "-jar", \
    "app.jar"]

# ================================================================
# STAGE 12: Inventory Service Build
# ================================================================
FROM backend-base AS inventory-service-build

WORKDIR /build/services/inventory

# Copy source code
COPY services/inventory/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 13: Inventory Service Runtime
# ================================================================
FROM eclipse-temurin:17-jre-alpine AS inventory-service

# Add non-root user
RUN addgroup -S werkflow && adduser -S werkflow -G werkflow

WORKDIR /app

# Copy JAR from build stage
COPY --from=inventory-service-build /build/services/inventory/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs && \
    chown -R werkflow:werkflow /app

USER werkflow

EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "-jar", \
    "app.jar"]

# ================================================================
# STAGE 14: Admin Portal Runtime
# ================================================================
FROM node:20-alpine AS admin-portal

# Add non-root user
RUN addgroup -S werkflow && adduser -S werkflow -G werkflow

WORKDIR /app

# Copy built application
COPY --from=admin-portal-build --chown=werkflow:werkflow /build/frontends/admin-portal/.next/standalone ./
COPY --from=admin-portal-build --chown=werkflow:werkflow /build/frontends/admin-portal/.next/static ./.next/static
COPY --from=admin-portal-build --chown=werkflow:werkflow /build/frontends/admin-portal/public ./public

USER werkflow

EXPOSE 4000

ENV PORT=4000
ENV NODE_ENV=production

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:4000/api/health || exit 1

CMD ["node", "server.js"]

# ================================================================
# STAGE 15: HR Portal Runtime
# ================================================================
FROM node:20-alpine AS hr-portal

# Add non-root user
RUN addgroup -S werkflow && adduser -S werkflow -G werkflow

WORKDIR /app

# Copy built application
COPY --from=hr-portal-build --chown=werkflow:werkflow /build/frontends/hr-portal/.next/standalone ./
COPY --from=hr-portal-build --chown=werkflow:werkflow /build/frontends/hr-portal/.next/static ./.next/static
COPY --from=hr-portal-build --chown=werkflow:werkflow /build/frontends/hr-portal/public ./public

USER werkflow

EXPOSE 4001

ENV PORT=4001
ENV NODE_ENV=production

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:4001/api/health || exit 1

CMD ["node", "server.js"]
