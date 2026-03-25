# Quy trình dịch thuật mô tả Quán ăn

Tài liệu này giải thích logic backend xử lý việc dịch mô tả quán ăn sang các ngôn ngữ khác nhau phục vụ cho tính năng audio guide.

## 1. Thành phần tham gia
- **`LocalizationService`**: Điều phối quy trình đa ngôn ngữ.
- **`TranslationService`**: Gọi API bên thứ ba để dịch văn bản.
- **`AudioService`**: Tạo file âm thanh từ văn bản đã dịch.
- **MyMemory API**: Dịch vụ dịch thuật (`api.mymemory.translated.net`).

## 2. Luồng xử lý chi tiết

### Bước 1: Thu thập dữ liệu gốc
Hệ thống ưu tiên lấy tên và mô tả từ bản ghi tiếng Việt (`vi`) trong bảng `food_stall_localizations`. Nếu không có, nó sẽ lấy trực tiếp từ bảng `food_stalls`.

### Bước 2: Dịch thuật (`TranslationService.java`)
- Hệ thống gửi request GET tới MyMemory API với cặp ngôn ngữ nguồn-đích (ví dụ: `vi|en`).
- **Xử lý phản hồi**:
  - Nếu thành công: Trả về văn bản đã dịch.
  - Nếu thất bại (API lỗi, timeout): Hệ thống sẽ **Fallback** (trả về chính văn bản gốc bằng tiếng Việt) để đảm bảo không bị lỗi luồng xử lý.
- **Giới hạn**: Do sử dụng API miễn phí, việc dịch các đoạn văn quá dài hoặc gửi quá nhiều yêu cầu cùng lúc có thể bị giới hạn (Rate limit).

### Bước 3: Tạo âm thanh (`AudioService.java`)
- Sau khi có bản dịch, hệ thống sẽ kết hợp `Tên + Mô tả`.
- Gửi sang công cụ TTS (EdgeTTS hoặc Google TTS) để tạo file `.mp3`.
- File được lưu với định dạng mới: `{stallId}_{languageCode}.mp3`.

### Bước 4: Lưu trữ (`Database`)
- Kết quả dịch và đường dẫn file audio (`audioUrl`) được lưu vào bảng `food_stall_localizations`.
- Cấu trúc bảng gồm: `food_stall_id`, `language_code`, `name`, `description`, `audio_url`.

## 3. Các ngôn ngữ hỗ trợ
Hiện tại hệ thống hỗ trợ tự động dịch sang 5 ngôn ngữ:
1. **Tiếng Việt (`vi`)** - Ngôn ngữ gốc.
2. **Tiếng Anh (`en`)**.
3. **Tiếng Nhật (`ja`)**.
4. **Tiếng Hàn (`ko`)**.
5. **Tiếng Trung (`zh`)**.

## 4. Lưu ý quan trọng
- Khi cập nhật thông tin quán (ở tiếng Việt), bạn nên kích hoạt lại quy trình `regenerate` để các ngôn ngữ khác được cập nhật bản dịch mới và audio mới tương ứng.
