# Street Voice Backend 🎙️

Backend API cho ứng dụng "Phố ẩm thực" - Hệ thống hướng dẫn âm thanh dựa trên vị trí địa lý.

## 🚀 Công nghệ sử dụng

- **Java 21**
- **Spring Boot 4.0.1**
- **Maven**
- **PostgreSQL** với **PostGIS** extension
- **Hibernate Spatial**
- **Lombok**

## 📋 Yêu cầu hệ thống

- Java 21 hoặc cao hơn
- PostgreSQL 12+ với PostGIS extension
- Maven 3.6+

## ⚙️ Cài đặt

### 1. Cài đặt PostgreSQL và PostGIS

**macOS (Homebrew):**
```bash
brew install postgresql postgis
brew services start postgresql
```

**Ubuntu/Debian:**
```bash
sudo apt-get install postgresql postgresql-contrib postgis
```

### 2. Tạo database

```bash
# Kết nối PostgreSQL
psql -U root

# Tạo database
CREATE DATABASE street_voice_db;

# Thoát psql
\q
```

### 3. Khởi tạo schema

```bash
# Chạy từ thư mục gốc của project
psql -U root -d street_voice_db -f src/main/resources/db/schema.sql
```

### 4. Cấu hình database (nếu cần)

File `src/main/resources/application.yaml` đã được cấu hình với:
- Database: `street_voice_db`
- Username: `root`
- Password: (để trống)

Nếu cần thay đổi, cập nhật file này:

```yaml
spring:
  datasource:
    username: your_username
    password: your_password
```

## 🐳 Docker Setup (Full Stack)

Để chạy trọn bộ (Backend + Database) cho Frontend Dev:

### 1. Cấu hình
Đảm bảo file `.env` đã có API Key (như mục Cài đặt).

### 2. Chạy
```bash
docker-compose up --build
```
*   Backend: `http://localhost:8080`
*   Database: `localhost:5432` (User: `postgres`, Pass: `password`, DB: `street_voice_db`)

## 🏃 Chạy ứng dụng (Thủ công)

### Development mode

```bash
./mvnw spring-boot:run
```

### Build JAR file

```bash
./mvnw clean package
java -jar target/street-voice-backend-0.0.1-SNAPSHOT.jar
```

Ứng dụng sẽ chạy tại: `http://localhost:8080`

## 📖 API Documentation (Swagger UI)

Tài liệu API (Swagger UI) đã được tích hợp sẵn để giúp Frontend team và các developer khác dễ dàng xem và thử nghiệm các API.

**Cách truy cập:**
1. Đảm bảo ứng dụng đang chạy (qua Docker Compose hoặc chạy thủ công).
2. Mở trình duyệt và truy cập: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
3. Hoặc xem OpenAPI JSON data tại: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 📡 API Endpoints

### Tìm quán ăn gần nhất

**Endpoint:** `GET /api/v1/stalls/nearby`

**Query Parameters:**
- `lat` (required): Vĩ độ (-90 đến 90)
- `lon` (required): Kinh độ (-180 đến 180)

**Ví dụ:**
```bash
curl "http://localhost:8080/api/v1/stalls/nearby?lat=21.0285&lon=105.8542"
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Phở Bát Đàn",
  "description": "Quán phở truyền thống nổi tiếng...",
  "audioUrl": "https://example.com/audio/pho-bat-dan.mp3",
  "imageUrl": "https://example.com/images/pho-bat-dan.jpg",
  "latitude": 21.0285,
  "longitude": 105.8542
}
```

**Error Responses:**
- `400 Bad Request`: Tham số không hợp lệ
- `404 Not Found`: Không tìm thấy quán ăn nào gần vị trí
- `500 Internal Server Error`: Lỗi server

### Import Dữ Liệu (JSON)

**Endpoint:** `POST /api/v1/admin/import-json`

**Body:** Danh sách các quán ăn.

**Ví dụ JSON:**
```json
[
  {
    "name": "Quán Ốc Oanh",
    "address": "534 Vĩnh Khánh, Phường 8, Quận 4, TP.HCM",
    "lat": 10.7607739,
    "lng": 106.7006542,
    "description": "Quán ốc nổi tiếng...",
    "audioUrl": "https://example.com/audio/oc_oanh.mp3",
    "triggerRadius": 15,
    "minPrice": 30000,
    "maxPrice": 150000,
    "audioDuration": 120,
    "featuredReviews": ["Good food", "Nice place"]
  }
]
```

**Curl Command:**
```bash
curl -X POST -H "Content-Type: application/json; charset=utf-8" \
     -d @import_test_data.json \
     http://localhost:8080/api/v1/admin/import-json
```

## 🗄️ Database Schema

### Bảng `food_stalls`

| Cột | Kiểu dữ liệu | Mô tả |
|-----|--------------|-------|
| id | SERIAL (Integer) | Primary key, auto-increment |
| name | VARCHAR(255) | Tên quán ăn |
| description | TEXT | Mô tả chi tiết |
| audio_url | VARCHAR(500) | URL file âm thanh |
| image_url | VARCHAR(500) | URL hình ảnh quán ăn |
| location | GEOGRAPHY(Point, 4326) | Tọa độ địa lý (PostGIS) |
| trigger_radius | INTEGER | Khoảng cách kích hoạt |
| min_price | INTEGER | Giá tối thiểu |
| max_price | INTEGER | Giá tối đa |
| audio_duration | INTEGER | Thời lượng audio (giây) |
| featured_reviews | JSONB | Các đánh giá nổi bật |
| created_at | TIMESTAMP | Thời gian tạo |

### Dữ liệu mẫu

Database đã được khởi tạo với 5 quán ăn tại khu phố cổ Hà Nội:
- Phở Bát Đàn (21.0285, 105.8542)
- Bún Chả Hàng Mành (21.0310, 105.8520)
- Bánh Mì Phố Hàng Cá (21.0295, 105.8498)
- Cà Phê Giảng (21.0330, 105.8510)
- Chả Cá Lã Vọng (21.0340, 105.8525)

## 📱 Tích hợp với Flutter

```dart
import 'package:http/http.dart' as http;
import 'dart:convert';

Future<Map<String, dynamic>> findNearestStall(double lat, double lon) async {
  final url = Uri.parse('http://localhost:8080/api/v1/stalls/nearby?lat=$lat&lon=$lon');
  final response = await http.get(url);
  
  if (response.statusCode == 200) {
    return json.decode(response.body);
  } else {
    throw Exception('Failed to load nearest stall');
  }
}
```

## 🏗️ Cấu trúc project

```
src/main/java/com/foodstreet/voice/
├── StreetVoiceApplication.java       # Main application
├── controller/
│   └── FoodStallController.java      # REST API endpoints
├── dto/
│   ├── ErrorResponse.java            # Error response format
│   ├── FoodStallResponse.java        # API response DTO
│   └── NearbyRequest.java            # Request validation
├── entity/
│   └── FoodStall.java                # JPA entity
├── exception/
│   ├── GlobalExceptionHandler.java   # Centralized error handling
│   └── ResourceNotFoundException.java
├── repository/
│   └── FoodStallRepository.java      # PostGIS queries
└── service/
    └── FoodStallService.java         # Business logic
```

## 🔧 Tính năng chính

✅ **PostGIS Spatial Queries**: Tìm kiếm dựa trên khoảng cách địa lý chính xác  
✅ **JTS Geometry**: Hỗ trợ đầy đủ cho dữ liệu không gian  
✅ **Input Validation**: Kiểm tra tham số đầu vào  
✅ **Exception Handling**: Xử lý lỗi tập trung  
✅ **RESTful API**: Tuân thủ chuẩn REST  
✅ **Logging**: Ghi log chi tiết cho debugging

## 📝 License

MIT License

## 👥 Contributors

- CTB
