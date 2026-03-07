# Database Setup Guide

## 🎯 Tổng Quan

Dự án hỗ trợ 2 chế độ chạy:
- **DEV Mode** (H2): Không cần cài đặt gì, chạy ngay
- **PROD Mode** (PostgreSQL): Cần cài đặt PostgreSQL

## ✅ Chế Độ Hiện Tại: DEV (H2 In-Memory)

### Ưu điểm
- ✅ Không cần cài đặt database
- ✅ Chạy ngay lập tức
- ✅ Hoàn hảo cho development và testing
- ✅ Có H2 Console để xem dữ liệu

### Nhược điểm
- ❌ Dữ liệu mất khi restart app
- ❌ Không dùng được cho production

### Cách Sử Dụng

**1. Chạy ứng dụng:**
```powershell
.\gradlew.bat bootRun
```

**2. Truy cập H2 Console:**
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:mindrevol_db`
- Username: `sa`
- Password: (để trống)

**3. Kiểm tra API:**
```bash
# API documentation
http://localhost:8080/swagger-ui.html

# Health check
http://localhost:8080/actuator/health
```

## 🔧 Chuyển Sang PostgreSQL (Production Mode)

### Bước 1: Cài Đặt PostgreSQL

**Option 1: Download từ trang chủ**
```
https://www.postgresql.org/download/windows/
```

**Option 2: Docker (Khuyến nghị)**
```powershell
# Pull PostgreSQL image
docker pull postgres:16

# Run PostgreSQL container
docker run --name mindrevol-postgres `
  -e POSTGRES_PASSWORD=1 `
  -e POSTGRES_DB=mindrevol_db `
  -p 5432:5432 `
  -d postgres:16

# Kiểm tra container đang chạy
docker ps

# Stop container
docker stop mindrevol-postgres

# Start container lại
docker start mindrevol-postgres

# Remove container (nếu cần)
docker rm -f mindrevol-postgres
```

### Bước 2: Tạo Database

**Nếu dùng Docker:**
```powershell
# Database đã được tạo tự động với tên: mindrevol_db
```

**Nếu cài PostgreSQL trực tiếp:**
```sql
-- Mở pgAdmin hoặc psql
CREATE DATABASE mindrevol_db;
```

### Bước 3: Cấu Hình Application

**Cách 1: Sửa application.properties**
```properties
# Thay đổi từ dev sang prod
spring.profiles.active=prod
```

**Cách 2: Set biến môi trường**
```powershell
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="prod"
.\gradlew.bat bootRun
```

**Cách 3: Pass argument khi chạy**
```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=prod'
```

### Bước 4: Cập Nhật Config (nếu cần)

Sửa file `application-prod.properties` nếu cần thay đổi:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mindrevol_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

### Bước 5: Chạy Migration (Liquibase)

Khi chạy ở chế độ prod, Liquibase sẽ tự động tạo tables:
```powershell
.\gradlew.bat bootRun
# Liquibase sẽ chạy các migration trong db/changelog/
```

## 📊 So Sánh Profiles

| Feature | DEV (H2) | PROD (PostgreSQL) |
|---------|----------|-------------------|
| Database | In-memory | Persistent |
| Installation | None | Required |
| Data Persistence | ❌ No | ✅ Yes |
| H2 Console | ✅ Enabled | ❌ Disabled |
| Liquibase | ❌ Disabled | ✅ Enabled |
| Redis Required | ❌ No | ✅ Yes |
| Rate Limiting | ❌ Disabled | ✅ Enabled |
| Logging Level | DEBUG | INFO/WARN |

## 🐛 Troubleshooting

### Error: "Failed to configure a DataSource"

**Nguyên nhân:** Profile không đúng hoặc PostgreSQL không chạy

**Giải pháp:**
```powershell
# 1. Kiểm tra profile hiện tại
# Trong application.properties phải có:
spring.profiles.active=dev

# 2. Hoặc chuyển về dev mode
# Sửa spring.profiles.active=prod thành spring.profiles.active=dev

# 3. Nếu dùng PostgreSQL, kiểm tra nó có chạy không
docker ps  # Nếu dùng Docker
# Hoặc
Get-Service postgresql*  # Nếu cài trực tiếp
```

### Error: "Connection refused to PostgreSQL"

**Giải pháp:**
```powershell
# Kiểm tra PostgreSQL có chạy không
docker ps | findstr postgres

# Nếu không, start lại
docker start mindrevol-postgres

# Hoặc chuyển sang dev mode
# Sửa spring.profiles.active=prod thành dev
```

### H2 Console không hiển thị tables

**Nguyên nhân:** Sai JDBC URL

**Giải pháp:**
- JDBC URL: `jdbc:h2:mem:mindrevol_db` (chính xác)
- Username: `sa`
- Password: (để trống)
- Click "Connect"

### Lỗi Liquibase

**Nếu dùng dev mode:**
```properties
# Liquibase đã tắt trong dev mode
spring.liquibase.enabled=false
```

**Nếu dùng prod mode nhưng chưa có changelog:**
```properties
# Tạm thời tắt Liquibase
spring.liquibase.enabled=false

# Hoặc dùng Hibernate tạo table tự động
spring.jpa.hibernate.ddl-auto=update
```

## 📝 Quick Commands

```powershell
# Chạy với DEV profile (H2)
.\gradlew.bat bootRun

# Chạy với PROD profile (PostgreSQL)  
.\gradlew.bat bootRun --args='--spring.profiles.active=prod'

# Clean build
.\gradlew.bat clean build

# Run tests
.\gradlew.bat test

# Check dependencies
.\gradlew.bat dependencies
```

## 🔐 Security Notes

### Development
- Default JWT secret: Chỉ dùng cho dev
- H2 Console: Exposed công khai (localhost only)
- No authentication cho H2 Console

### Production
- **PHẢI đổi JWT secret** trong application-prod.properties
- H2 Console: Disabled
- Redis rate limiting: Enabled
- Use environment variables cho passwords:
  ```powershell
  $env:DB_PASSWORD="your-secure-password"
  $env:JWT_SECRET="your-512-bit-secret"
  ```

## 🚀 Deployment Checklist

- [ ] Đổi `spring.profiles.active` sang `prod`
- [ ] Update `app.jwt.secret` với giá trị bảo mật
- [ ] Cấu hình PostgreSQL production URL
- [ ] Enable Redis và cấu hình connection
- [ ] Cập nhật CORS allowed origins
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Enable Liquibase migrations
- [ ] Configure email credentials
- [ ] Set production logging levels
- [ ] Use environment variables cho sensitive data

---
**Lưu ý:** Hiện tại app đang chạy ở **DEV mode** với H2, không cần cài đặt gì thêm!

