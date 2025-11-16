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

# Copy poms for all services
COPY services/hr/pom.xml services/hr/pom.xml
COPY services/engine/pom.xml services/engine/pom.xml

# Download dependencies for all services
RUN cd services/hr && mvn dependency:go-offline -B
RUN cd services/engine && mvn dependency:go-offline -B

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
# STAGE 4: Admin Service Build (Placeholder - Phase 1)
# ================================================================
FROM backend-base AS admin-service-build

# TODO: Implement in Phase 1 Week 7-8
# For now, this is a placeholder that will be populated
# when the admin service is created

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
RUN npm ci --only=production && npm cache clean --force

# Copy source code
COPY frontends/admin-portal/ ./

# Build the application
RUN npm run build

# ================================================================
# STAGE 7: HR Portal Build (Placeholder - Phase 2)
# ================================================================
FROM frontend-base AS hr-portal-build

# TODO: Implement in Phase 2 Week 9-12
# For now, this is a placeholder

# ================================================================
# STAGE 8: HR Service Runtime
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
# STAGE 9: Engine Service Runtime
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
# STAGE 10: Admin Service Runtime (Placeholder - Phase 1)
# ================================================================
FROM eclipse-temurin:17-jre-alpine AS admin-service

# TODO: Implement in Phase 1 Week 7-8

RUN addgroup -S werkflow && adduser -S werkflow -G werkflow

WORKDIR /app

# Placeholder - will copy admin JAR when built
# COPY --from=admin-service-build /build/services/admin/target/*.jar app.jar

RUN mkdir -p /app/logs && \
    chown -R werkflow:werkflow /app

USER werkflow

EXPOSE 8083

# HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#     CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# ENTRYPOINT ["java", \
#     "-Djava.security.egd=file:/dev/./urandom", \
#     "-XX:MaxRAMPercentage=75.0", \
#     "-XX:+UseContainerSupport", \
#     "-jar", \
#     "app.jar"]

# ================================================================
# STAGE 11: Admin Portal Runtime
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
# STAGE 12: HR Portal Runtime (Placeholder - Phase 2)
# ================================================================
FROM node:20-alpine AS hr-portal

# TODO: Implement in Phase 2 Week 9-12

RUN addgroup -S werkflow && adduser -S werkflow -G werkflow

WORKDIR /app

USER werkflow

EXPOSE 4001

ENV PORT=4001
ENV NODE_ENV=production

# CMD ["node", "server.js"]
