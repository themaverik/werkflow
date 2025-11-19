# ================================================================
# WERKFLOW ENTERPRISE PLATFORM - MULTI-STAGE DOCKERFILE
# ================================================================
# This Dockerfile builds all services and frontends for the
# werkflow enterprise platform in a single, optimized build.
# ================================================================

# ================================================================
# STAGE 1: Backend Base - Build werkflow-delegates first
# ================================================================
FROM maven:3.9-eclipse-temurin-21 AS backend-base

WORKDIR /build

# Copy and build werkflow-delegates first (required by other services)
COPY shared/delegates/pom.xml shared/delegates/pom.xml
COPY shared/delegates/src shared/delegates/src

# Build and install werkflow-delegates to local maven repo
RUN cd shared/delegates && mvn clean install -DskipTests -B

# Now copy service poms
COPY services/hr/pom.xml services/hr/pom.xml
COPY services/engine/pom.xml services/engine/pom.xml
COPY services/admin/pom.xml services/admin/pom.xml
COPY services/inventory/pom.xml services/inventory/pom.xml
COPY services/finance/pom.xml services/finance/pom.xml
COPY services/procurement/pom.xml services/procurement/pom.xml

# Download dependencies for all services (werkflow-delegates now available locally)
RUN cd services/hr && mvn dependency:go-offline -B
RUN cd services/engine && mvn dependency:go-offline -B
RUN cd services/admin && mvn dependency:go-offline -B
RUN cd services/inventory && mvn dependency:go-offline -B
RUN cd services/finance && mvn dependency:go-offline -B
RUN cd services/procurement && mvn dependency:go-offline -B

# ================================================================
# STAGE 2: HR Service Build
# ================================================================
FROM backend-base AS hr-service-build

WORKDIR /build/services/hr

# Copy source code
COPY services/hr/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 3: Engine Service Build
# ================================================================
FROM backend-base AS engine-service-build

WORKDIR /build/services/engine

# Copy source code
COPY services/engine/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 4: Admin Service Build
# ================================================================
FROM backend-base AS admin-service-build

WORKDIR /build/services/admin

# Copy source code
COPY services/admin/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 5: Frontend Base - Node.js dependencies
# ================================================================
FROM node:20-alpine AS frontend-base

WORKDIR /build

# Install pnpm for better monorepo support
RUN npm install -g pnpm

# ================================================================
# STAGE 6: Admin Portal Build
# ================================================================
FROM frontend-base AS admin-portal-build

WORKDIR /build/frontends/admin-portal

# Copy package files
COPY frontends/admin-portal/package.json frontends/admin-portal/package-lock.json* ./

# Install dependencies
RUN npm install --omit=dev && npm cache clean --force

# Copy source code
COPY frontends/admin-portal/ ./

# Build the application
RUN npm run build

# ================================================================
# STAGE 7: HR Portal Build
# ================================================================
FROM frontend-base AS hr-portal-build

WORKDIR /build/frontends/hr-portal

# Copy package files
COPY frontends/hr-portal/package.json frontends/hr-portal/package-lock.json* ./

# Install dependencies
RUN npm install --omit=dev && npm cache clean --force

# Copy source code
COPY frontends/hr-portal/ ./

# Build the application
RUN npm run build

# ================================================================
# STAGE 8: HR Service Runtime
# ================================================================
FROM eclipse-temurin:21-jre AS hr-service

# Add non-root user
RUN groupadd -r werkflow && useradd -r -g werkflow werkflow

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
# STAGE 9: Engine Service Runtime
# ================================================================
FROM eclipse-temurin:21-jre AS engine-service

# Add non-root user
RUN groupadd -r werkflow && useradd -r -g werkflow werkflow

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
# STAGE 10: Admin Service Runtime
# ================================================================
FROM eclipse-temurin:21-jre AS admin-service

# Add non-root user
RUN groupadd -r werkflow && useradd -r -g werkflow werkflow

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
# STAGE 11: Inventory Service Build
# ================================================================
FROM backend-base AS inventory-service-build

WORKDIR /build/services/inventory

# Copy source code
COPY services/inventory/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 12: Inventory Service Runtime
# ================================================================
FROM eclipse-temurin:21-jre AS inventory-service

# Add non-root user
RUN groupadd -r werkflow && useradd -r -g werkflow werkflow

WORKDIR /app

# Copy JAR from build stage
COPY --from=inventory-service-build /build/services/inventory/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs && \
    chown -R werkflow:werkflow /app

USER werkflow

EXPOSE 8086

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "-jar", \
    "app.jar"]

# ================================================================
# STAGE 13: Finance Service Build
# ================================================================
FROM backend-base AS finance-service-build

WORKDIR /build/services/finance

# Copy source code
COPY services/finance/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 14: Finance Service Runtime
# ================================================================
FROM eclipse-temurin:21-jre AS finance-service

# Add non-root user
RUN groupadd -r werkflow && useradd -r -g werkflow werkflow

WORKDIR /app

# Copy JAR from build stage
COPY --from=finance-service-build /build/services/finance/target/*.jar app.jar

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
# STAGE 15: Procurement Service Build
# ================================================================
FROM backend-base AS procurement-service-build

WORKDIR /build/services/procurement

# Copy source code
COPY services/procurement/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 16: Procurement Service Runtime
# ================================================================
FROM eclipse-temurin:21-jre AS procurement-service

# Add non-root user
RUN groupadd -r werkflow && useradd -r -g werkflow werkflow

WORKDIR /app

# Copy JAR from build stage
COPY --from=procurement-service-build /build/services/procurement/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs && \
    chown -R werkflow:werkflow /app

USER werkflow

EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "-jar", \
    "app.jar"]

# ================================================================
# STAGE 17: Admin Portal Runtime
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
# STAGE 18: HR Portal Runtime
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
