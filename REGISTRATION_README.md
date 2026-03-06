# Hệ thống Đăng ký với OTP - Mindrevol Backend

## Tổng quan

Hệ thống đăng ký người dùng 3 bước (Wizard) với xác thực OTP qua email. Dữ liệu đăng ký được lưu tạm trong Redis và chỉ tạo User chính thức sau khi xác thực OTP thành công.

## Kiến trúc

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Frontend  │────▶│    Backend   │────▶│    Redis    │
│   (React)   │◀────│   (Spring)   │◀────│ (Temp Data) │
└─────────────┘     └──────────────┘     └─────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  PostgreSQL  │
                    │  (User Data) │
                    └──────────────┘
```

## Flow Đăng ký

### Bước 1: Nhập Email & Handle
```
POST /api/v1/auth/register/step1
{
  "email": "user@example.com",
  "handle": "username123"
}
```

**Backend:**
- Kiểm tra email và handle có tồn tại không
- Lưu tạm vào Redis: `register:temp:{email}`
- TTL: 15 phút

### Bước 2: Nhập thông tin cá nhân & Mật khẩu
```
POST /api/v1/auth/register/step2
{
  "email": "user@example.com",
  "fullname": "Nguyễn Văn A",
  "password": "Password@123",
  "dateOfBirth": "2000-01-01",
  "gender": "MALE",
  "timezone": "Asia/Ho_Chi_Minh"
}
```

**Backend:**
- Lấy dữ liệu tạm từ Redis
- Mã hóa mật khẩu bằng BCrypt
- Tạo mã OTP 6 số ngẫu nhiên
- Gửi OTP qua email
- Cập nhật lại Redis

### Bước 3: Xác thực OTP
```
POST /api/v1/auth/register/step3
{
  "email": "user@example.com",
  "otpCode": "123456"
}
```

**Backend:**
- Kiểm tra OTP (tối đa 5 lần thử)
- Nếu đúng:
  - Tạo User trong PostgreSQL
  - Tạo JWT Token
  - Xóa dữ liệu tạm trong Redis
  - Trả về token cho Frontend

**Response:**
```json
{
  "status": 201,
  "message": "Đăng ký thành công! Chào mừng bạn đến với Mindrevol",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "uuid-here",
      "email": "user@example.com",
      "handle": "username123",
      "fullname": "Nguyễn Văn A",
      "accountType": "FREE"
    }
  }
}
```

## API Bổ sung

### 1. Kiểm tra Email/Handle có tồn tại không
```
POST /api/v1/auth/check-availability
{
  "email": "user@example.com"
}
// hoặc
{
  "handle": "username123"
}
```

**Response:**
```json
{
  "data": {
    "available": true
  },
  "message": "Có thể sử dụng"
}
```

### 2. Gửi lại OTP
```
POST /api/v1/auth/resend-otp
{
  "email": "user@example.com"
}
```

## Cấu hình

### 1. Database (PostgreSQL)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mindrevol_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### 2. Redis
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 3. Email (Gmail)
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

**Lưu ý:** Để gửi email qua Gmail, bạn cần:
1. Bật xác thực 2 bước
2. Tạo App Password tại: https://myaccount.google.com/apppasswords

### 4. JWT
```properties
app.jwt.secret=your-secret-key-256-bits
app.jwt.access-token-expiration-ms=3600000    # 1 hour
app.jwt.refresh-token-expiration-ms=86400000  # 24 hours
```

## Cấu trúc Redis

### Key Pattern
```
register:temp:{email}
```

### Data Structure (RegisterTempData)
```json
{
  "email": "user@example.com",
  "handle": "username123",
  "fullname": "Nguyễn Văn A",
  "password": "$2a$10$...",  // BCrypt hashed
  "dateOfBirth": "2000-01-01",
  "gender": "MALE",
  "timezone": "Asia/Ho_Chi_Minh",
  "otpCode": "123456",
  "otpAttempts": 0,
  "createdAt": 1709654400000
}
```

### TTL
- **15 phút** - Thời gian hết hạn của phiên đăng ký

## Validation Rules

### Email
- Không được để trống
- Phải đúng định dạng email
- Tối đa 100 ký tự
- Không được trùng với email đã tồn tại

### Handle
- Không được để trống
- Từ 3-50 ký tự
- Chỉ chứa chữ cái, số và dấu gạch dưới
- Không được trùng với handle đã tồn tại

### Password
- Không được để trống
- Từ 8-100 ký tự
- Phải chứa ít nhất:
  - 1 chữ hoa
  - 1 chữ thường
  - 1 số
  - 1 ký tự đặc biệt (@$!%*?&)

### OTP
- Phải có 6 chữ số
- Chỉ chứa số
- Tối đa 5 lần thử sai

## Security Features

1. **Password Hashing**: BCrypt với salt tự động
2. **OTP Expiration**: 15 phút
3. **Rate Limiting**: Tối đa 5 lần thử OTP
4. **JWT Authentication**: Access Token + Refresh Token
5. **Data Encryption**: HTTPS (nên bật trong production)

## Error Handling

### Common Errors

| Error | Message | Solution |
|-------|---------|----------|
| Email đã tồn tại | "Email đã được sử dụng" | Sử dụng email khác |
| Handle đã tồn tại | "Handle đã được sử dụng" | Sử dụng handle khác |
| Phiên hết hạn | "Phiên đăng ký đã hết hạn" | Đăng ký lại từ đầu |
| OTP sai | "Mã OTP không chính xác" | Nhập lại hoặc gửi lại OTP |
| Quá số lần thử | "Bạn đã nhập sai OTP quá nhiều lần" | Đăng ký lại từ đầu |

## Testing

### 1. Start Services
```bash
# Start PostgreSQL
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres

# Start Redis
docker run -d -p 6379:6379 redis

# Start Backend
./gradlew bootRun
```

### 2. Test với Postman/cURL

**Step 1:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/step1 \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","handle":"testuser"}'
```

**Step 2:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/step2 \
  -H "Content-Type: application/json" \
  -d '{
    "email":"test@example.com",
    "fullname":"Test User",
    "password":"Test@123",
    "gender":"MALE"
  }'
```

**Check email** → Get OTP code

**Step 3:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/step3 \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","otpCode":"123456"}'
```

## Frontend Integration

### React Example với Axios

```javascript
// Step 1
const step1 = async (email, handle) => {
  const response = await axios.post('/api/v1/auth/register/step1', {
    email, handle
  });
  return response.data;
};

// Step 2
const step2 = async (data) => {
  const response = await axios.post('/api/v1/auth/register/step2', data);
  return response.data;
};

// Step 3
const step3 = async (email, otpCode) => {
  const response = await axios.post('/api/v1/auth/register/step3', {
    email, otpCode
  });
  
  // Lưu token
  localStorage.setItem('accessToken', response.data.data.accessToken);
  localStorage.setItem('refreshToken', response.data.data.refreshToken);
  
  return response.data;
};

// Check availability (với debounce)
const checkEmail = debounce(async (email) => {
  const response = await axios.post('/api/v1/auth/check-availability', {
    email
  });
  return response.data.data.available;
}, 500);
```

## Tiêu chí nghiệm thu

- [x] Dữ liệu đăng ký lưu tạm vào Redis
- [x] OTP gửi về email có hiệu lực 15 phút
- [x] Nhập sai OTP báo lỗi, tối đa 5 lần
- [x] Nhập đúng OTP tạo User và trả về JWT Token
- [x] API check trùng Email/Handle
- [x] Hỗ trợ gửi lại OTP
- [x] Validation đầy đủ cho tất cả field
- [x] Password được mã hóa BCrypt

## Production Checklist

- [ ] Đổi `app.jwt.secret` thành key mạnh và bảo mật
- [ ] Cấu hình SMTP server riêng (không dùng Gmail cá nhân)
- [ ] Bật HTTPS
- [ ] Cấu hình Redis với password
- [ ] Setup monitoring và logging
- [ ] Rate limiting cho API
- [ ] Backup database định kỳ
- [ ] Environment variables cho sensitive data

## Tác giả

- **Backend Team** - Mindrevol Project
- **Module**: Authentication & Registration
- **Version**: 1.0.0

