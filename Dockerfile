# Stage 1: Build ứng dụng bằng Maven
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper và pom.xml trước để tận dụng Docker cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B || true

# Copy mã nguồn và đóng gói
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime JRE 21 nhỏ gọn
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo non-root user để tăng tính bảo mật
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Tạo thư mục uploads và phân quyền
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

# Copy file jar từ builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
