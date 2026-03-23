# 📡 API Testing Guide - Postman

Hướng dẫn chi tiết để test tất cả các API endpoints của Street Voice Backend với Postman.

## 🚀 Base URL

```
http://localhost:8080
```

---

## 📋 Danh sách API Endpoints

### 1. **GET** - Lấy tất cả food stalls

**Endpoint:** `GET /api/v1/stalls`

**Mô tả:** Lấy danh sách tất cả các quán ăn trong database

**Request:**
```http
GET http://localhost:8080/api/v1/stalls
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Phở Bát Đàn",
    "description": "Quán phở truyền thống nổi tiếng...",
    "audioUrl": "https://example.com/audio/pho-bat-dan.mp3",
    "imageUrl": "https://example.com/images/pho-bat-dan.jpg",
    "latitude": 21.0285,
    "longitude": 105.8542
  },
  {
    "id": 2,
    "name": "Bún Chả Hàng Mành",
    "description": "Bún chả Hà Nội chính gốc...",
    "audioUrl": "https://example.com/audio/bun-cha-hang-manh.mp3",
    "imageUrl": "https://example.com/images/bun-cha.jpg",
    "latitude": 21.031,
    "longitude": 105.852
  }
]
```

---

### 2. **GET** - Lấy food stall theo ID

**Endpoint:** `GET /api/v1/stalls/{id}`

**Mô tả:** Lấy thông tin chi tiết của một quán ăn theo ID

**Request:**
```http
GET http://localhost:8080/api/v1/stalls/1
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Phở Bát Đàn",
  "description": "Quán phở truyền thống nổi tiếng tại phố cổ Hà Nội...",
  "audioUrl": "https://example.com/audio/pho-bat-dan.mp3",
  "imageUrl": "https://example.com/images/pho-bat-dan.jpg",
  "latitude": 21.0285,
  "longitude": 105.8542
}
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-01-18T20:00:00+07:00",
  "status": 404,
  "error": "Not Found",
  "message": "Food stall not found with id: 999",
  "path": "/api/v1/stalls/999"
}
```

---

### 3. **GET** - Tìm quán gần nhất (PostGIS)

**Endpoint:** `GET /api/v1/stalls/nearby?lat={latitude}&lon={longitude}`

**Mô tả:** Tìm quán ăn gần nhất dựa trên tọa độ GPS (sử dụng PostGIS ST_Distance)

**Query Parameters:**
- `lat` (required): Vĩ độ (-90 đến 90)
- `lon` (required): Kinh độ (-180 đến 180)

**Request:**
```http
GET http://localhost:8080/api/v1/stalls/nearby?lat=21.0285&lon=105.8542
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

**Response (400 Bad Request) - Tọa độ không hợp lệ:**
```json
{
  "timestamp": "2026-01-18T20:00:00+07:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: {lat=Latitude must be between -90 and 90}",
  "path": "/api/v1/stalls/nearby"
}
```

---

### 4. **POST** - Tạo food stall mới

**Endpoint:** `POST /api/v1/stalls`

**Mô tả:** Tạo một quán ăn mới trong database

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Bánh Cuốn Thanh Vân",
  "description": "Bánh cuốn nóng hổi, nhân thịt băm và nấm mèo thơm ngon",
  "audioUrl": "https://example.com/audio/banh-cuon.mp3",
  "imageUrl": "https://example.com/images/banh-cuon.jpg",
  "latitude": 21.0320,
  "longitude": 105.8500
}
```

**Response (201 Created):**
```json
{
  "id": 6,
  "name": "Bánh Cuốn Thanh Vân",
  "description": "Bánh cuốn nóng hổi, nhân thịt băm và nấm mèo thơm ngon",
  "audioUrl": "https://example.com/audio/banh-cuon.mp3",
  "imageUrl": "https://example.com/images/banh-cuon.jpg",
  "latitude": 21.032,
  "longitude": 105.85
}
```

**Response (400 Bad Request) - Thiếu thông tin bắt buộc:**
```json
{
  "timestamp": "2026-01-18T20:00:00+07:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: {name=Name is required}",
  "path": "/api/v1/stalls"
}
```

---

### 5. **PUT** - Cập nhật food stall

**Endpoint:** `PUT /api/v1/stalls/{id}`

**Mô tả:** Cập nhật thông tin của một quán ăn (tất cả các field đều optional)

**Headers:**
```
Content-Type: application/json
```

**Request:**
```http
PUT http://localhost:8080/api/v1/stalls/1
```

**Request Body (Cập nhật một số field):**
```json
{
  "description": "Quán phở truyền thống nổi tiếng nhất phố cổ Hà Nội, được thành lập từ năm 1979",
  "audioUrl": "https://example.com/audio/pho-bat-dan-updated.mp3"
}
```

**Request Body (Cập nhật tất cả):**
```json
{
  "name": "Phở Bát Đàn (Chi nhánh mới)",
  "description": "Quán phở truyền thống...",
  "audioUrl": "https://example.com/audio/pho-new.mp3",
  "imageUrl": "https://example.com/images/pho-new.jpg",
  "latitude": 21.0290,
  "longitude": 105.8545
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Phở Bát Đàn (Chi nhánh mới)",
  "description": "Quán phở truyền thống...",
  "audioUrl": "https://example.com/audio/pho-new.mp3",
  "imageUrl": "https://example.com/images/pho-new.jpg",
  "latitude": 21.029,
  "longitude": 105.8545
}
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-01-18T20:00:00+07:00",
  "status": 404,
  "error": "Not Found",
  "message": "Food stall not found with id: 999",
  "path": "/api/v1/stalls/999"
}
```

---

### 6. **DELETE** - Xóa food stall

**Endpoint:** `DELETE /api/v1/stalls/{id}`

**Mô tả:** Xóa một quán ăn khỏi database

**Request:**
```http
DELETE http://localhost:8080/api/v1/stalls/6
```

**Response (204 No Content):**
```
(No response body)
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-01-18T20:00:00+07:00",
  "status": 404,
  "error": "Not Found",
  "message": "Food stall not found with id: 999",
  "path": "/api/v1/stalls/999"
}
```

---

### 7. **GET** - Pack-Info (Offline Mode)

**Endpoint:** `GET /api/v1/stalls/pack-info`

**Mô tả:** Lấy thông tin dung lượng file audio dựa trên ngôn ngữ, kèm thời gian cập nhật để App Mobile đối chiếu bộ nhớ (Offline Mode).

**Query Parameters:**
- `lang` (optional, default="vi"): Mã ngôn ngữ (VD: "vi", "en")

**Request:**
```http
GET http://localhost:8080/api/v1/stalls/pack-info?lang=vi
```

**Response (200 OK):**
```json
{
    "language":  "vi",
    "totalFiles":  28,
    "totalSizeBytes":  11468800,
    "estimatedSizeMb":  10.9375,
    "lastUpdated":  "2026-03-22T16:27:32.692661"
}
```

---

### 8. **POST** - Batch Analytics Sync

**Endpoint:** `POST /api/v1/analytics/track/batch`

**Mô tả:** Đồng bộ hàng loạt event (nghe audio, chuyển vùng) khi App Mobile offline và có mạng trở lại. Giúp tối ưu truy vấn Database thay vì gọi API lẻ tẻ.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
[
  {
    "deviceId": "DEVICE_01",
    "stallId": 1,
    "action": "ENTER_REGION"
  },
  {
    "deviceId": "DEVICE_01",
    "stallId": 1,
    "action": "AUTO_PLAY",
    "duration": 45
  }
]
```

**Response (200 OK):**
```json
{
    "status":  "success",
    "message":  "Batch events received",
    "count":  2
}
```

---

## 🧪 Test Scenarios với Postman

### Scenario 1: CRUD Flow hoàn chỉnh

1. **GET** `/api/v1/stalls` - Xem tất cả quán (5 quán mẫu)
2. **POST** `/api/v1/stalls` - Tạo quán mới (id: 6)
3. **GET** `/api/v1/stalls/6` - Xem chi tiết quán vừa tạo
4. **PUT** `/api/v1/stalls/6` - Cập nhật thông tin
5. **DELETE** `/api/v1/stalls/6` - Xóa quán
6. **GET** `/api/v1/stalls/6` - Verify đã xóa (404)

### Scenario 2: Test PostGIS Nearby

1. **GET** `/api/v1/stalls/nearby?lat=21.0285&lon=105.8542` - Gần Phở Bát Đàn
2. **GET** `/api/v1/stalls/nearby?lat=21.0310&lon=105.8520` - Gần Bún Chả
3. **GET** `/api/v1/stalls/nearby?lat=21.0330&lon=105.8510` - Gần Cà Phê Giảng

### Scenario 3: Test Validation

1. **POST** với `name` trống → 400 Bad Request
2. **POST** với `latitude: 999` → 400 Bad Request
3. **GET** `/api/v1/stalls/nearby?lat=abc&lon=105` → 400 Bad Request
4. **GET** `/api/v1/stalls/999` → 404 Not Found

---

## 📦 Import Postman Collection

Tạo file `Street_Voice_API.postman_collection.json` (xem file riêng) và import vào Postman:

1. Mở Postman
2. Click **Import** button
3. Chọn file JSON
4. Collection sẽ có sẵn tất cả 6 endpoints

---

## 🔧 Environment Variables (Optional)

Tạo Postman Environment với biến:

```json
{
  "base_url": "http://localhost:8080",
  "stall_id": "1"
}
```

Sử dụng trong requests: `{{base_url}}/api/v1/stalls/{{stall_id}}`

---

## ✅ Expected Results

| Endpoint | Method | Status Code | Response Type |
|----------|--------|-------------|---------------|
| `/api/v1/stalls` | GET | 200 | Array of objects |
| `/api/v1/stalls/{id}` | GET | 200 / 404 | Object / Error |
| `/api/v1/stalls/nearby` | GET | 200 / 400 / 404 | Object / Error |
| `/api/v1/stalls` | POST | 201 / 400 | Object / Error |
| `/api/v1/stalls/{id}` | PUT | 200 / 404 | Object / Error |
| `/api/v1/stalls/{id}` | DELETE | 204 / 404 | Empty / Error |
| `/api/v1/stalls/pack-info` | GET | 200 | Object |
| `/api/v1/analytics/track/batch` | POST | 200 / 400 | Object / Error |

---

## 🐛 Common Issues

### Issue 1: Connection refused
**Solution:** Đảm bảo Spring Boot app đang chạy: `./mvnw spring-boot:run`

### Issue 2: 404 on all endpoints
**Solution:** Kiểm tra base URL có đúng `http://localhost:8080` không

### Issue 3: 500 Internal Server Error
**Solution:** Kiểm tra database connection và PostGIS extension đã được kích hoạt

### Issue 4: Validation errors
**Solution:** Kiểm tra request body format và required fields
