# Audio System - Guide & Architecture

## Tổng quan

Audio trong hệ thống **Street Voice** hoạt động theo mô hình **filename-based** — DB chỉ lưu tên file, URL đầy đủ được build tự động bởi backend khi trả về response.

```
DB: audio_url = 'oc_oanh.mp3'
                 ↓ FoodStallService.resolveAudioUrl()
API: audioUrl = 'http://192.168.31.216:8080/audio/oc_oanh.mp3'
```

---

## 1. Setup nhanh cho Team

### Bước 1 — Cấu hình `.env`
```bash
cp .env.example .env
# Sửa IP của bạn:
AUDIO_BASE_URL=http://192.168.x.x:8080
```

### Bước 2 — Đặt file audio vào thư mục `./audio/`
```
street-voice-backend/
  audio/
    oc_oanh.mp3      ← file thật
    ten_quan_khac.mp3
```

### Bước 3 — Chạy ứng dụng
```bash
docker-compose up --build -d
```

---

## 2. API Endpoints

### Lấy thông tin audio của một quán
```
GET /api/v1/stalls/{id}/audio
```

**Response (có audio):**
```json
{
  "id": 8,
  "name": "Quán Ốc Oanh",
  "audioUrl": "http://192.168.31.216:8080/audio/oc_oanh.mp3",
  "audioDuration": 120
}
```

**Response (chưa có audio):**
```json
{
  "id": 1,
  "name": "Hải Sản Tươi Sống Sáu Nở",
  "audioUrl": "",
  "message": "Quan nay chua co audio. Vui long goi API /sync truoc."
}
```

### Phát trực tiếp file audio
```
GET /audio/{filename}
```
Ví dụ: `GET http://192.168.31.216:8080/audio/oc_oanh.mp3`
- Trả về file MP3, hỗ trợ `Range` header để mobile seek được
- Cache 1 giờ (`Cache-Control: max-age=3600`)

### Danh sách tất cả quán (có `audioUrl`)
```
GET /api/v1/stalls
```
Trường `audioUrl` trong mỗi quán đã là URL đầy đủ, sẵn sàng để phát.

### Gen audio tự động cho tất cả quán
```
GET /api/v1/stalls/sync?lat=10.762622&lng=106.700174&radius=2000
```
Với những quán có `audioUrl` rỗng, backend sẽ tự gen audio qua Google TTS và điền vào DB.

---

## 3. Cập nhật audio_url cho một quán cụ thể

Nếu bạn đã có file MP3 và muốn gán thủ công:

**Cách 1 — qua API (khuyến nghị):**
```bash
curl -X PUT http://localhost:8080/api/v1/stalls/8 \
  -H "Content-Type: application/json" \
  -d '{"audioUrl": "8_vi.mp3"}'
```

**Cách 2 — trực tiếp DB:**
```sql
UPDATE food_stalls SET audio_url = '8_vi.mp3' WHERE id = 8;
```

---

## 4. Cấu hình hệ thống

### `application.yaml`
```yaml
app:
  audio:
    local-path: /app/audio          # Đường dẫn bên trong container
    base-url: ${AUDIO_BASE_URL:http://localhost:8080}  # Fallback nếu không có env
```

### `docker-compose.yml`
```yaml
volumes:
  - ./audio:/app/audio   # Mount thư mục audio local vào container
environment:
  - AUDIO_BASE_URL=${AUDIO_BASE_URL}
```

### `WebConfig.java`
Đăng ký resource handler:
```java
registry.addResourceHandler("/audio/**")
        .addResourceLocations("file:/app/audio/")
        .setCachePeriod(3600);
```
CORS cho phép mọi thiết bị trong mạng LAN (`192.168.*.*`).

---

## 5. Quy ước đặt tên file audio

| Quán | Filename |
|------|----------|
| Quán Ốc Oanh | `oc_oanh.mp3` |
| Hải Sản Sáu Nở | `hai_san_sau_no.mp3` |
| (gen tự động) | `<hash>_vi.mp3` |

---

## 6. Troubleshooting

### `audioUrl` trả về `null` hoặc rỗng
→ Quán chưa có audio. Gọi `/sync` hoặc update thủ công.

### HTTP 500 khi gọi `/audio/file.mp3`
→ Kiểm tra file có tồn tại trong thư mục `./audio/` chưa.
→ Kiểm tra Docker mount: `docker exec street-voice-backend ls /app/audio`.

### Thiết bị mobile trong LAN không kết nối được
→ Đảm bảo `AUDIO_BASE_URL` đang dùng IP thật của máy host (không phải `localhost`).
→ Kiểm tra Firewall Windows có chặn port 8080 không.

### CORS error từ FE
→ IP của FE phải nằm trong dải `192.168.*.*`. Kiểm tra `WebConfig.allowedOriginPatterns`.