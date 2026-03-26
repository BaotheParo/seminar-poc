# Street Voice Backend 🎙️

Backend API cho ứng dụng "Phố ẩm thực" - Hệ thống hướng dẫn âm thanh đa ngôn ngữ dựa trên vị trí địa lý.

## 🚀 Công nghệ sử dụng

| Thành phần | Chi tiết |
|-----------|---------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.2.2 |
| Build | Maven |
| Database | PostgreSQL 14+ với PostGIS extension |
| ORM | Hibernate Spatial + JPA |
| Async | Spring `@Async` + ThreadPoolTaskExecutor |
| TTS | edge-tts (Microsoft Edge Neural TTS) |
| Translation | MyMemory Translation API |
| Docs | Swagger UI (SpringDoc OpenAPI) |
| Container | Docker + Docker Compose |

---

## ⚙️ Cài đặt & Chạy

### Cách 1: Docker (Khuyến nghị cho Frontend)

```bash
# Tạo/cập nhật file .env
echo "AUDIO_BASE_URL=http://localhost:8080" > .env
echo "VIETMAP_API_KEY_SERVICES=<your-key>" >> .env

# Build & chạy toàn bộ stack
docker-compose down -v
docker-compose up -d --build
```

- Backend: `http://localhost:8080`
- DB: `localhost:5432` (user: `postgres`, pass: `password`, db: `street_voice_db`)
- Flyway tự động migrate schema, `DatabaseSeeder` tự động nạp 28 quán ăn mẫu khi DB trống.

### Cách 2: Chạy thủ công (Dev)

```bash
# Yêu cầu: PostgreSQL đang chạy local, DB 'street_voice_db' đã tồn tại
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password=<your-pg-password>"
```

### Swagger UI

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

---

## 📡 API Endpoints

### Public — Food Stalls (`/api/v1/stalls`)

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/v1/stalls` | Lấy tất cả quán ăn, hỗ trợ `?lang=` |
| `GET` | `/api/v1/stalls/search` | Tìm kiếm + lọc + phân trang |
| `GET` | `/api/v1/stalls/{id}` | Chi tiết 1 quán, hỗ trợ `?lang=` |
| `GET` | `/api/v1/stalls/nearby` | Quán gần nhất theo lat/lon |
| `GET` | `/api/v1/stalls/geofence` | Quán trong vùng geofence (tối đa 5) |
| `GET` | `/api/v1/stalls/sync` | Đồng bộ dữ liệu offline cho mobile |
| `GET` | `/api/v1/stalls/pack-info` | Thông tin pack audio offline |
| `GET` | `/api/v1/stalls/audio/download-pack` | Tải ZIP audio theo ngôn ngữ |
| `POST` | `/api/v1/stalls` | Tạo quán mới (kích hoạt dịch tự động) |
| `PUT` | `/api/v1/stalls/{id}` | Cập nhật quán |
| `DELETE` | `/api/v1/stalls/{id}` | Xóa quán |
| `POST` | `/api/v1/stalls/{id}/audio/generate` | Tạo audio on-demand theo lang |
| `POST` | `/api/v1/stalls/{id}/audio/generate-all` | Tạo audio cho tất cả ngôn ngữ |

#### Tham số `?lang=` (hỗ trợ đa ngôn ngữ)

Các API `GET /api/v1/stalls` và `GET /api/v1/stalls/{id}` đều hỗ trợ tham số `lang`:

| Giá trị | Ngôn ngữ |
|---------|---------|
| `vi` (default) | Tiếng Việt |
| `en` | Tiếng Anh |
| `ja` | Tiếng Nhật |
| `ko` | Tiếng Hàn |
| `zh` | Tiếng Trung |

**Ví dụ:**
```
GET /api/v1/stalls?lang=en
GET /api/v1/stalls/29?lang=ja
```

**Response có thêm 2 field quan trọng:**
```json
{
  "id": 1,
  "name": "Pho Ba Dan",
  "usedLanguage": "en",
  "localizationStatus": null
}
```

| Field | Giá trị | Ý nghĩa |
|-------|---------|---------|
| `usedLanguage` | `"en"` | Ngôn ngữ thực tế đang dùng |
| `localizationStatus` | `null` | Đã có đủ bản dịch theo yêu cầu |
| `localizationStatus` | `"FALLBACK_TO_VI"` | Chưa có bản dịch, đang dùng tiếng Việt |

---

### Admin — Data Management (`/api/v1/admin`)

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/v1/admin/sync-localizations` | Đồng bộ đa ngôn ngữ cho toàn bộ quán |
| `POST` | `/api/v1/admin/sync-vietmap` | Import dữ liệu từ VietMap API |
| `PATCH` | `/api/v1/admin/stores/{id}/geofence` | Cập nhật geofence cho 1 quán |

#### `POST /api/v1/admin/sync-localizations`

Quét toàn bộ quán ăn trong DB, tìm và bổ sung bản dịch + audio cho các quán còn thiếu (chạy trong background). API trả về ngay lập tức, không cần request body.

```json
// Response (202 Accepted):
{
  "totalStalls": 28,
  "queuedForSync": 27,
  "alreadyComplete": 1,
  "message": "27 quan dang duoc dong bo da ngon ngu trong nen. Vui long doi 15-30 giay roi kiem tra lai."
}
```

---

## 🌐 Hệ thống Đa Ngôn Ngữ

### Translate-on-Create Pattern

Khi tạo quán mới (`POST /api/v1/stalls`):
1. API lưu quán vào DB và **trả về `201 Created` ngay lập tức**.
2. Một tiến trình bất đồng bộ (`@Async`) chạy ngầm:
   - `vi`: Dùng nội dung gốc, chỉ tạo TTS audio.
   - `en, ja, ko, zh`: Gọi MyMemory API để dịch, sau đó tạo TTS audio bằng `edge-tts`.
3. Mỗi ngôn ngữ xử lý độc lập — lỗi 1 ngôn ngữ không ảnh hưởng ngôn ngữ khác (fault-tolerant).
4. Kết quả lưu vào bảng `food_stall_localizations`.

### Bảng `food_stall_localizations`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | SERIAL | Primary key |
| `food_stall_id` | BIGINT FK | Liên kết với `food_stalls` |
| `language_code` | VARCHAR | `vi`, `en`, `ja`, `ko`, `zh` |
| `name` | VARCHAR | Tên quán đã dịch |
| `description` | TEXT | Mô tả đã dịch |
| `audio_url` | VARCHAR | Đường dẫn file `.mp3` |
| `created_at` | TIMESTAMP | Thời gian tạo |

---

## 🗄️ Database Schema

### Bảng `food_stalls`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | SERIAL | Primary key |
| `name` | VARCHAR(255) | Tên quán (tiếng Việt gốc) |
| `description` | TEXT | Mô tả (tiếng Việt gốc) |
| `audio_url` | VARCHAR | URL audio mặc định (vi) |
| `image_url` | VARCHAR | URL hình ảnh |
| `location` | GEOGRAPHY(Point,4326) | Tọa độ PostGIS |
| `trigger_radius` | INTEGER | Bán kính kích hoạt (mét) |
| `min_price` | INTEGER | Giá thấp nhất (VND) |
| `max_price` | INTEGER | Giá cao nhất (VND) |
| `audio_duration` | INTEGER | Thời lượng audio (giây) |
| `priority` | INTEGER | Ưu tiên geofence |
| `featured_reviews` | JSONB | Đánh giá nổi bật |
| `created_at` | TIMESTAMP | Thời gian tạo |

---

## 🏗️ Cấu trúc Project

```
src/main/java/com/foodstreet/voice/
├── StreetVoiceApplication.java
├── config/                        # AsyncConfig, AudioConfig, AudioProperties
├── controller/
│   ├── FoodStallController.java   # Public API endpoints
│   └── AdminSyncController.java   # Admin endpoints (sync, geofence)
├── dto/                           # Request/Response DTOs
│   ├── FoodStallResponse.java     # Có usedLanguage + localizationStatus
│   ├── LocalizationResponse.java
│   └── ...
├── entity/
│   ├── FoodStall.java
│   └── FoodStallLocalization.java
├── repository/
│   ├── FoodStallRepository.java
│   └── FoodStallLocalizationRepository.java
├── service/
│   ├── FoodStallService.java      # Business logic + bulk localization map
│   ├── LocalizationService.java   # Async dịch + audio, syncAllMissing
│   ├── TranslationService.java    # MyMemory API
│   ├── AudioService.java          # TTS + coalescing
│   └── VietMapSyncService.java    # Import từ VietMap
└── seeder/
    └── DatabaseSeeder.java        # Auto-seed 28 quán khi DB trống
```

---

## 🔧 Tính năng chính

✅ **PostGIS Spatial Queries** — Geofence chính xác, ưu tiên theo `priority`  
✅ **Đa ngôn ngữ** — vi / en / ja / ko / zh với Fallback về tiếng Việt  
✅ **Translate-on-Create** — Dịch bất đồng bộ ngay khi tạo quán, API vẫn trả `201` ngay  
✅ **Fault-tolerant Async** — Lỗi 1 ngôn ngữ không dừng các ngôn ngữ còn lại  
✅ **Sync All Localizations** — Một API để backfill bản dịch cho toàn bộ quán cũ  
✅ **localizationStatus** — Client biết quán nào chưa dịch (`FALLBACK_TO_VI`)  
✅ **Offline Audio Pack** — Tải ZIP audio theo ngôn ngữ cho Mobile offline  
✅ **Race Condition Prevention** — Request coalescing cho TTS  
✅ **Auto-seed** — 28 quán mẫu tự động nạp khi khởi động lần đầu  

---

## 📝 License

MIT License

## 👥 Contributors

- CTB
