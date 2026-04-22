# 🚀 MindRevol Backend

> **Nền tảng xã hội tâm lý - Kết nối các tâm hồn yếu mệnh**

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green?style=flat-square&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## 📋 Mục Lục

- [Giới Thiệu](#-giới-thiệu)
- [Tính Năng Chính](#-tính-năng-chính)
- [Kiến Trúc Hệ Thống](#-kiến-trúc-hệ-thống)
- [Công Nghệ Sử Dụng](#-công-nghệ-sử-dụng)
- [Cài Đặt & Chạy](#-cài-đặt--chạy)
- [Cấu Hình Môi Trường](#-cấu-hình-môi-trường)
- [API Documentation](#-api-documentation)
- [Các Module Chính](#-các-module-chính)
- [Hướng Dẫn Phát Triển](#-hướng-dẫn-phát-triển)
- [Troubleshooting](#-troubleshooting)

---

## 💡 Giới Thiệu

**MindRevol** là một nền tảng xã hội tâm lý độc đáo, được thiết kế để kết nối những người cần tìm sự hỗ trợ tinh thần, chia sẻ cảm xúc và trải nghiệm của họ trong môi trường an toàn và bảo mật cao.

Backend được xây dựng bằng **Spring Boot 3.3.5** với kiến trúc modular, cung cấp các dịch vụ mạnh mẽ cho xác thực người dùng, quản lý tin nhắn real-time, hỗ trợ 2FA nâng cao và nhiều tính năng khác.

---

## ✨ Tính Năng Chính

### 🔐 Bảo Mật & Xác Thực
- ✅ **Đăng nhập đa phương thức**: Email/Password, OTP, Magic Link
- ✅ **Two-Factor Authentication (2FA)**:
  - 📧 Email OTP (6 chữ số)
  - 🔐 Authenticator App (TOTP)
  - 💾 Backup Codes (Khôi phục truy cập)
- ✅ **Social Login**: Google, Facebook, TikTok, Apple
- ✅ **JWT Token**: Với Access & Refresh Token
- ✅ **Rate Limiting**: Chống brute-force attack
- ✅ **Session Management**: Quản lý phiên đăng nhập

### 💬 Tính Năng Cộng Đồng
- 💌 **Tin nhắn thời gian thực** (WebSocket)
- 📝 **Bài viết & Check-in**: Chia sẻ trạng thái tâm trạng
- ❤️ **Tương tác**: Like, comment, react
- 👥 **Hộp cá nhân**: Tạo nhóm và chia sẻ nội dung riêng tư
- 🚶 **Journey**: Theo dõi hành trình cân bằng tâm lý
- 🎯 **Mood Tracking**: Ghi lại trạng thái cảm xúc hàng ngày

### 📱 Tính Năng Hỗ Trợ
- 📢 **Thông báo**: Push notification, WebSocket notification
- 💾 **Lưu trữ**: Upload file, ảnh với CDN Cloudinary
- ⚙️ **Cấu hình người dùng**: Settings tùy chỉnh
- 🚫 **Chặn người dùng**: Quản lý danh sách chặn

---

## 🏗️ Kiến Trúc Hệ Thống

```
mindrevol_backend/
├── src/
│   ├── main/
│   │   ├── java/com/mindrevol/core/
│   │   │   ├── MindrevolBackendCoreApplication.java
│   │   │   ├── config/              # Cấu hình ứng dụng
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── ...
│   │   │   ├── common/              # Công cụ chung
│   │   │   │   ├── exception/       # Custom exceptions
│   │   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── service/         # Shared services
│   │   │   │   └── utils/           # Utility functions
│   │   │   └── modules/             # Các module chính
│   │   │       ├── auth/            # Xác thực & đăng ký
│   │   │       ├── user/            # Quản lý người dùng
│   │   │       ├── chat/            # Tin nhắn
│   │   │       ├── notification/    # Thông báo
│   │   │       ├── feed/            # Bài viết
│   │   │       ├── box/             # Hộp cá nhân
│   │   │       ├── journey/         # Hành trình
│   │   │       ├── mood/            # Theo dõi cảm xúc
│   │   │       ├── checkin/         # Check-in trạng thái
│   │   │       ├── storage/         # Lưu trữ file
│   │   │       └── advertising/     # Quảng cáo
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/changelog/
│   └── test/
└── build.gradle
```

### Kiến Trúc Tầng

```
┌─────────────────────────────────────┐
│      REST API / WebSocket           │
├─────────────────────────────────────┤
│       Controller Layer              │
├─────────────────────────────────────┤
│       Service Layer                 │
│   ├── Business Logic                │
│   └── Strategy Patterns             │
├─────────────────────────────────────┤
│       Data Access Layer             │
│   ├── Repository (JPA)              │
│   └── Redis Cache                   │
├─────────────────────────────────────┤
│   PostgreSQL │ Redis │ Cloudinary   │
└─────────────────────────────────────┘
```

---

## 🛠️ Công Nghệ Sử Dụng

| Lĩnh Vực | Công Nghệ | Phiên Bản |
|---------|-----------|----------|
| **Framework** | Spring Boot | 3.3.5 |
| **Java** | JDK | 21 |
| **Database** | PostgreSQL | 15+ |
| **Cache** | Redis + Redisson | 7+ / 3.24.3 |
| **ORM** | Hibernate JPA | 6.5.3 |
| **Security** | Spring Security + JWT | 0.11.5 |
| **Real-time** | WebSocket | Spring Native |
| **Email** | Spring Mail | 3.3.5 |
| **Async** | Spring @Async | 3.3.5 |
| **Mapping** | MapStruct | 1.5.5 |
| **Storage** | Cloudinary API | 1.38.0 |
| **Validation** | Jakarta Validation | 3.0+ |
| **Logging** | SLF4J + Logback | Native |
| **Container** | Docker | Latest |

---

## 📦 Cài Đặt & Chạy

### Yêu Cầu Hệ Thống

- **Java JDK 21+**
- **Maven 3.8+** hoặc **Gradle 8.5+**
- **PostgreSQL 15+**
- **Redis 7+**
- **Docker & Docker Compose** (Tuỳ chọn)

### 1️⃣ Clone Repository

```bash
git clone https://github.com/your-org/mindrevol-backend.git
cd mindrevol_backend
```

### 2️⃣ Cấu Hình Biến Môi Trường

Tạo file `.env` hoặc cấu hình trong `application.properties`:

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mindrevol
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# JWT Secret
APP_JWT_SECRET=your_super_secret_jwt_key_here_at_least_32_characters

# Email
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
FACEBOOK_APP_ID=your_facebook_app_id
FACEBOOK_APP_SECRET=your_facebook_app_secret

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

### 3️⃣ Chạy Bằng Gradle

```bash
# Development Mode
./gradlew bootRun --args='--spring.profiles.active=dev'

# Production Mode
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### 4️⃣ Chạy Bằng Docker Compose

```bash
docker-compose up -d

# Xem logs
docker-compose logs -f app

# Dừng lại
docker-compose down
```

### 5️⃣ Kiểm Tra Ứng Dụng

```bash
# Health Check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## ⚙️ Cấu Hình Môi Trường

### application.properties

```properties
# ===== SERVER =====
server.port=8080
server.servlet.context-path=/

# ===== DATABASE =====
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# ===== REDIS =====
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms

# ===== SECURITY =====
app.security.max-concurrent-sessions=5
app.jwt.access-token-expiration-ms=3600000    # 1 hour
app.jwt.refresh-token-expiration-ms=2592000000 # 30 days

# ===== CORS =====
app.cors.allowed-origins=http://localhost:5173,http://localhost:3000

# ===== EMAIL =====
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Profiles

- **dev**: Development (H2 in-memory hoặc PostgreSQL)
- **prod**: Production (PostgreSQL chỉ)

---

## 📚 API Documentation

Swagger UI có sẵn tại: `http://localhost:8080/swagger-ui.html`

### Các Endpoint Chính

#### Authentication
```
POST   /api/v1/auth/login                    # Đăng nhập email/password
POST   /api/v1/auth/otp/send                # Gửi OTP qua email
POST   /api/v1/auth/otp/login               # Xác thực OTP
POST   /api/v1/auth/2fa/setup               # Thiết lập 2FA
POST   /api/v1/auth/2fa/verify              # Xác thực 2FA
GET    /api/v1/auth/2fa/backup-codes        # Lấy backup codes
```

#### Social Login
```
POST   /api/v1/auth/login/google            # Đăng nhập Google
POST   /api/v1/auth/login/facebook          # Đăng nhập Facebook
POST   /api/v1/auth/login/tiktok            # Đăng nhập TikTok
POST   /api/v1/auth/login/apple             # Đăng nhập Apple
```

#### User Management
```
GET    /api/v1/users/profile                # Lấy thông tin cá nhân
PUT    /api/v1/users/profile                # Cập nhật thông tin
POST   /api/v1/users/avatar                 # Upload ảnh đại diện
GET    /api/v1/users/{userId}               # Lấy thông tin người khác
```

#### Chat & Messages
```
GET    /api/v1/messages/conversations       # Danh sách cuộc trò chuyện
POST   /api/v1/messages/send                # Gửi tin nhắn
GET    /api/v1/messages/{conversationId}    # Lịch sử tin nhắn
WS     /ws/messages                         # WebSocket real-time
```

#### Feed & Posts
```
GET    /api/v1/feed/posts                   # Danh sách bài viết
POST   /api/v1/feed/posts                   # Tạo bài viết mới
PUT    /api/v1/feed/posts/{postId}          # Chỉnh sửa bài viết
DELETE /api/v1/feed/posts/{postId}          # Xóa bài viết
POST   /api/v1/feed/posts/{postId}/like     # Like bài viết
```

#### Notifications
```
GET    /api/v1/notifications                # Danh sách thông báo
PUT    /api/v1/notifications/{id}/read      # Đánh dấu đã đọc
WS     /ws/notifications                    # WebSocket thông báo
```

---

## 🎯 Các Module Chính

### 1️⃣ Auth Module (Xác Thực)
- Đăng nhập/đăng ký đa phương thức
- OTP via Email
- Two-Factor Authentication (2FA)
- Social Login Integration
- Session Management

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/auth/`

### 2️⃣ User Module (Quản Lý Người Dùng)
- Profile Management
- Avatar Upload
- Friendship Management
- User Settings
- User Blocking

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/user/`

### 3️⃣ Chat Module (Tin Nhắn)
- Real-time Messaging (WebSocket)
- Conversation Management
- Message History
- Message Reactions
- Typing Indicators

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/chat/`

### 4️⃣ Feed Module (Bài Viết)
- Create/Edit/Delete Posts
- Like & Comment
- Post Reactions
- Feed Timeline
- Search & Filter

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/feed/`

### 5️⃣ Notification Module (Thông Báo)
- Email Notifications
- Push Notifications
- WebSocket Real-time
- Notification Management
- Subscription System

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/notification/`

### 6️⃣ Box Module (Hộp Cá Nhân)
- Create Private Boxes
- Box Members Management
- Box Invitations
- Box Content Sharing

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/box/`

### 7️⃣ Journey Module (Hành Trình)
- Create/Share Journeys
- Journey Participants
- Journey Requests
- Progress Tracking

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/journey/`

### 8️⃣ Mood Module (Theo Dõi Cảm Xúc)
- Mood Recording
- Mood History
- Mood Analytics
- Emotion Tracking

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/mood/`

### 9️⃣ CheckIn Module (Kiểm Tra Trạng Thái)
- Daily Check-ins
- Check-in History
- Check-in Comments
- Check-in Reactions

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/checkin/`

### 🔟 Storage Module (Lưu Trữ)
- File Upload Management
- Cloudinary Integration
- File Deletion
- File Metadata

**📁 Vị trí**: `src/main/java/com/mindrevol/core/modules/storage/`

---

## 🚀 Hướng Dẫn Phát Triển

### Quy Ước Code

#### Naming Convention
```java
// Classes
public class UserService { }
public class UserRepository { }
public class UserRequest { }
public class UserResponse { }

// Methods
private void sendEmail() { }
public Optional<User> findById() { }

// Variables
private String userEmail;
private final UserRepository repository;
```

#### Folder Structure cho Module Mới
```
modules/
└── example/
    ├── controller/
    │   └── ExampleController.java
    ├── service/
    │   ├── ExampleService.java
    │   └── impl/
    │       └── ExampleServiceImpl.java
    ├── repository/
    │   └── ExampleRepository.java
    ├── entity/
    │   └── Example.java
    ├── dto/
    │   ├── request/
    │   │   └── CreateExampleRequest.java
    │   └── response/
    │       └── ExampleResponse.java
    └── README.md
```

### Logging

```java
@Slf4j
public class ExampleService {
    public void example() {
        log.info("Info message: {}", variable);
        log.warn("Warning message");
        log.error("Error message", exception);
    }
}
```

### Exception Handling

```java
// Custom Exception
throw new BadRequestException("Invalid request");
throw new ResourceNotFoundException("User not found");
throw new UnauthorizedException("Invalid credentials");

// Global Exception Handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException e) {
        return ResponseEntity.badRequest().body(
            ApiResponse.error(e.getMessage())
        );
    }
}
```

### Testing

```bash
# Chạy tất cả tests
./gradlew test

# Chạy test cụ thể
./gradlew test --tests UserServiceTest

# Xem code coverage
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

---

## 🐛 Troubleshooting

### ❌ Lỗi: `Could not resolve placeholder 'app.cors.allowed-origins'`

**Nguyên nhân**: Biến môi trường không được cấu hình

**Giải pháp**:
```properties
# Thêm vào application.properties
app.cors.allowed-origins=http://localhost:5173
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.allowed-headers=*
```

### ❌ Lỗi: `Parameter required a bean of type 'RedisTemplate' that could not be found`

**Nguyên nhân**: Redis không được cấu hình đúng

**Giải pháp**:
```java
// Trong RedisConfig.java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
```

### ❌ Lỗi: `Syntax error in SQL statement... returning id`

**Nguyên nhân**: H2 database không hỗ trợ `RETURNING` clause của PostgreSQL

**Giải pháp**:
```properties
# Sử dụng PostgreSQL thực tế, không phải H2
spring.datasource.url=jdbc:postgresql://localhost:5432/mindrevol
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### ❌ Lỗi: `Invalid Scopes: email` (Facebook Login)

**Nguyên nhân**: Facebook không cho phép scope `email` cho ứng dụng chưa xác minh

**Giải pháp**:
1. Loại bỏ scope `email` từ OAuth2 config
2. Hoặc submit ứng dụng cho Meta xác minh: https://developers.facebook.com/apps/

### ❌ OTP không được gửi

**Nguyên nhân**: Email service chưa được cấu hình

**Giải pháp**:
```properties
# Cấu hình Gmail SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password  # NOT your actual password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### ❌ WebSocket Connection Failed

**Nguyên nhân**: CORS chưa được cấu hình cho WebSocket

**Giải pháp**:
```java
// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/**")
            .setAllowedOrigins("http://localhost:5173")
            .withSockJS();
    }
}
```

---

## 📞 Support & Documentation

- **API Docs**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics

---

## 📄 License

Project này được cấp phép dưới **MIT License** - xem file [LICENSE](LICENSE) để biết thêm chi tiết.

---

## 👥 Contributors

- **Lead Developer**: Tôi
- **Backend Architect**: Team Development

---

## 🙏 Cảm Ơn

Cảm ơn các công nghệ mã nguồn mở có giúp project này thành công:
- Spring Boot Team
- PostgreSQL Community
- Redis Project
- Docker Community

---

**Made with ❤️ by MindRevol Team**

_Cập nhật lần cuối: April 2026_

