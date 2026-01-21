# Audio Guide System (POC Version)

## 📖 Giới thiệu (Overview)
Dự án **Proof of Concept (POC)** xây dựng hệ thống Backend cho ứng dụng thuyết minh tự động dựa trên vị trí (Audio Guide).
Mục tiêu chính của dự án là chứng minh năng lực **Tư duy thuật toán (Algorithmic Thinking)** và kỹ năng xử lý Logic "thủ công" thay vì phụ thuộc hoàn toàn vào các thư viện có sẵn (như PostGIS).

### 💡 Điểm đặc biệt (Key Highlight)
Dự án **KHÔNG** sử dụng extension `PostGIS` của PostgreSQL để tính khoảng cách. Thay vào đó:
1.  Lưu tọa độ (`lat`, `lng`) dưới dạng số thực (`Double`) thuần túy.
2.  Xử lý logic tìm kiếm không gian (Spatial Search) ngay tại tầng Application (Service Layer).
3.  Tự cài đặt thuật toán **Haversine** bằng Java thuần để tính khoảng cách đường chim bay giữa người dùng và địa điểm.

---

## 🛠 Tech Stack
-   **Language:** Java 17
-   **Framework:** Spring Boot 3.x
    -   Spring Web
    -   Spring Data JPA
-   **Database:** PostgreSQL (Containerized via Docker)
-   **DevOps:** Docker Compose

---

## 🚀 Hướng dẫn Cài đặt & Chạy (Setup & Run)

### 1. Chuẩn bị (Prerequisites)
-   Java JDK 17+
-   Docker Desktop (để chạy Database)
-   IntelliJ IDEA / Eclipse (Khuyên dùng)

### 2. Khởi động Database
Dự án sử dụng PostgreSQL chạy trên Docker (Port **5433** để tránh đụng độ với các services khác).
```bash
docker-compose up -d
```
*Lưu ý: Username/Password mặc định là `postgres`/`password`.*

### 3. Chạy Ứng dụng (Backend)
Có 2 cách để chạy:

**Cách 1: Dùng Script tự động (Windows)**
Double-click vào file `run_app.bat` trong thư mục gốc. Script sẽ tự tìm Maven và chạy ứng dụng.

**Cách 2: Dùng IDE**
1.  Mở project bằng IntelliJ IDEA.
2.  Mở file `src/main/java/com/example/audioguide/AudioGuideApplication.java`.
3.  Nhấn nút **Run**.

Sau khi chạy thành công, Server sẽ lắng nghe tại: `http://localhost:8080`.

---

## 📡 API Documentation
API chính phục vụ tính năng "Tìm địa điểm thuyết minh lân cận".

### 📍 Get Nearby Stores
Lọc danh sách các địa điểm nằm trong bán kính **1km (1000m)** so với vị trí người dùng.

-   **URL:** `GET /api/v1/stores/nearby`
-   **Query Params:**
    -   `lat`: Vĩ độ hiện tại của user (ví dụ: `10.759917`)
    -   `lng`: Kinh độ hiện tại của user (ví dụ: `106.682258`)
-   **Response Example:**
```json
[
    {
        "id": 1,
        "name": "Trường Đại học Sài Gòn",
        "lat": 10.759917,
        "lng": 106.682258,
        "audioPath": "/media/dh_sai_gon.mp3"
    },
    {
        "id": 2,
        "name": "Cơm Tấm Cổng Sau",
        "lat": 10.7605,
        "lng": 106.683,
        "audioPath": "/media/com_tam.mp3"
    }
]
```

---

## 📂 Project Structure
```
com.example.audioguide
├── core
│   ├── config          # Cấu hình hệ thống (WebConfig...)
│   └── utils           # Các class tiện ích (GeoUtils - Haversine Logic)
├── modules
│   └── store           # Module Quản lý địa điểm
│       ├── controller  # API Layer
│       ├── entity      # Database Entity
│       ├── repository  # Data Access Layer
│       └── service     # Business Logic (Nơi chứa vòng lặp filter khoảng cách)
└── AudioGuideApplication.java
```

---

## 🧪 Test Data (Mock)
Dữ liệu mẫu được tự động nạp từ file `src/main/resources/data.sql` khi khởi động App.
Các địa điểm mẫu xoay quanh khu vực **Đại học Sài Gòn (Quận 5)** và **Chợ Bến Thành (Quận 1)**.
