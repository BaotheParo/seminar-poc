
DROP TABLE IF EXISTS food_stalls CASCADE;

CREATE TABLE food_stalls (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    audio_url VARCHAR(500),
    image_url VARCHAR(500),
    location GEOGRAPHY(Point, 4326),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_food_stalls_location ON food_stalls USING GIST(location);

INSERT INTO food_stalls (name, description, audio_url, image_url, location) VALUES
(
    'Phở Bát Đàn',
    'Quán phở truyền thống nổi tiếng tại phố cổ Hà Nội, được thành lập từ năm 1979. Nước dùng được ninh từ xương bò trong nhiều giờ, tạo nên hương vị đậm đà khó quên.',
    'https://example.com/audio/pho-bat-dan.mp3',
    'https://example.com/images/pho-bat-dan.jpg',
    ST_GeogFromText('POINT(105.8542 21.0285)')
),
(
    'Bún Chả Hàng Mành',
    'Bún chả Hà Nội chính gốc với thịt nướng thơm phức, nước mắm chua ngọt hài hòa. Địa điểm ăn uống yêu thích của người dân địa phương và du khách.',
    'https://example.com/audio/bun-cha-hang-manh.mp3',
    'https://example.com/images/bun-cha.jpg',
    ST_GeogFromText('POINT(105.8520 21.0310)')
),
(
    'Bánh Mì Phố Hàng Cá',
    'Bánh mì Việt Nam với nhân đa dạng: pate, chả lụa, thịt nguội, rau thơm và gia vị đặc trưng. Vỏ bánh giòn rụm, nhân đầy đặn.',
    'https://example.com/audio/banh-mi-hang-ca.mp3',
    'https://example.com/images/banh-mi.jpg',
    ST_GeogFromText('POINT(105.8498 21.0295)')
),
(
    'Cà Phê Giảng',
    'Cà phê trứng nổi tiếng Hà Nội - món đồ uống độc đáo với lớp kem trứng béo ngậy, thơm ngon trên nền cà phê đen đậm đà.',
    'https://example.com/audio/ca-phe-giang.mp3',
    'https://example.com/images/ca-phe-trung.jpg',
    ST_GeogFromText('POINT(105.8510 21.0330)')
),
(
    'Chả Cá Lã Vọng',
    'Món ăn đặc sản Hà Nội với cá lăng tươi được ướp nghệ, nướng vàng, ăn kèm bún, rau thơm và mắm tôm. Hơn 100 năm lịch sử.',
    'https://example.com/audio/cha-ca-la-vong.mp3',
    'https://example.com/images/cha-ca.jpg',
    ST_GeogFromText('POINT(105.8525 21.0340)')
);

SELECT 'Migration completed successfully!' as status;
SELECT COUNT(*) as total_records FROM food_stalls;