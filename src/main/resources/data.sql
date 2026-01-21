-- File này sẽ được Spring Boot tự động chạy khi khởi động (do config spring.jpa.hibernate.ddl-auto=update)

-- 1. Đại học Sài Gòn (Điểm mốc trung tâm)
-- Lat: 10.759917, Lng: 106.682258
INSERT INTO stores (name, lat, lng, audio_path) 
VALUES ('Trường Đại học Sài Gòn', 10.759917, 106.682258, '/media/dh_sai_gon.mp3');

-- 2. Quán Cơm Tấm (Gần cổng sau, cách ~200m)
-- Lat: 10.760500, Lng: 106.683000
INSERT INTO stores (name, lat, lng, audio_path) 
VALUES ('Cơm Tấm Cổng Sau', 10.760500, 106.683000, '/media/com_tam.mp3');

-- 3. Bãi giữ xe (Gần đó, cách ~100m)
-- Lat: 10.759500, Lng: 106.682000
INSERT INTO stores (name, lat, lng, audio_path) 
VALUES ('Bãi giữ xe sinh viên', 10.759500, 106.682000, '/media/bai_xe.mp3');

-- 4. HighLand Coffee (Xa hơn chút, ~800m - Vẫn trong bán kính 1km)
-- Lat: 10.765000, Lng: 106.680000
INSERT INTO stores (name, lat, lng, audio_path) 
VALUES ('Highland Coffee Nguyễn Trãi', 10.765000, 106.680000, '/media/highland.mp3');

-- 5. Chợ Bến Thành (Xa > 1km, để test trường hợp KHÔNG HIỆN)
-- Lat: 10.772109, Lng: 106.698270 (Cách khoảng ~2.5km)
INSERT INTO stores (name, lat, lng, audio_path) 
VALUES ('Chợ Bến Thành', 10.772109, 106.698270, '/media/cho_ben_thanh.mp3');
