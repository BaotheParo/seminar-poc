CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS food_stalls (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    description TEXT,
    audio_url VARCHAR(500),
    image_url VARCHAR(500),
    location GEOGRAPHY(Point, 4326),
    trigger_radius INTEGER DEFAULT 15,
    min_price INTEGER,
    max_price INTEGER,
    audio_duration INTEGER,
    featured_reviews JSONB,
    rating DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE INDEX IF NOT EXISTS idx_food_stalls_location 
ON food_stalls USING GIST(location);

CREATE TABLE IF NOT EXISTS food_stall_localizations (
    id BIGSERIAL PRIMARY KEY,
    food_stall_id BIGINT NOT NULL,
    language_code VARCHAR(10) NOT NULL,         -- vi, en, ja, ko, zh
    name VARCHAR(255),
    description TEXT,
    audio_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_localization_stall FOREIGN KEY (food_stall_id) REFERENCES food_stalls(id) ON DELETE CASCADE,
    CONSTRAINT uq_stall_lang UNIQUE (food_stall_id, language_code)
);

CREATE INDEX IF NOT EXISTS idx_localization_stall_lang 
ON food_stall_localizations (food_stall_id, language_code);

INSERT INTO food_stalls (
    name, address, description, audio_url, location, trigger_radius, 
    min_price, max_price, audio_duration, featured_reviews, created_at
) VALUES
(
    'Hải Sản Tươi Sống Sáu Nở', 'Đường Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Hải Sản Tươi Sống Sáu Nở là quán hải sản nổi tiếng trên phố Vĩnh Khánh với 2 chi nhánh, được nhiều thực khách biết đến nhờ nguồn hải sản tươi sống chất lượng...', 
    '', ST_GeogFromText('POINT(106.701733 10.763697)'), 20, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Hải Sản Tươi Sống Bé Ốc', 'Đường Vĩnh Khánh,Phường 9,Quận 4,Thành Phố Hồ Chí Minh', 
    'Bé Ốc là quán hải sản tươi sống quen thuộc trên phố Vĩnh Khánh, tập trung vào các món ốc và ngao được chọn lọc kỹ...', 
    '', ST_GeogFromText('POINT(106.702011 10.763378)'), 12, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Hải Sản Hai Lữ', 'Đường Vĩnh Khánh,Phường 9,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Hải Sản Hai Lữ là quán hải sản tươi sống với thực đơn chế biến đa dạng từ nướng, hấp đến xào...', 
    '', ST_GeogFromText('POINT(106.70211 10.763263)'), 12, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Lẩu Bò Kỳ Kim', '106B Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Lẩu Bò Kỳ Kim là quán lẩu bò tươi được nhiều thực khách lựa chọn tại phố Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.702563 10.761425)'), 15, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Ốc Loan', '129 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Ốc Loan là quán ốc vỉa hè quen thuộc trên phố Vĩnh Khánh, nổi bật với thời gian mở cửa muộn đến sáng...', 
    '', ST_GeogFromText('POINT(106.702652 10.761269)'), 12, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Tiệm Phá Lấu 825', '99 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Tiệm Phá Lấu 825 là quán phá lấu và đồ nhậu bình dân quen thuộc tại khu Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.702901 10.761145)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Ốc Hồng Nhung', '38 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Ốc Hồng Nhung là quán ốc lâu năm trên phố Vĩnh Khánh, được nhiều thực khách biết đến với các món ngon...', 
    '', ST_GeogFromText('POINT(106.702956 10.761099)'), 8, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Ốc Oanh', '534 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Ốc Oanh là một trong những quán ốc – hải sản nổi tiếng bậc nhất trên phố ẩm thực Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.703294 10.760852)'), 12, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Ốc 662', '662 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Ốc 662 chuyên phục vụ ốc tươi và hải sản nướng, xào theo phong cách vỉa hè...', 
    '', ST_GeogFromText('POINT(106.703442 10.760733)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Ốc Vĩnh Khánh', '131 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Ốc Vĩnh Khánh là quán ốc truyền thống được nhiều thực khách biết đến...', 
    '', ST_GeogFromText('POINT(106.703657 10.760609)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Lẩu Dê Toàn & Ký', 'Đường Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Lẩu Dê Toàn & Ký là quán lẩu dê đặc sản được nhiều thực khách lựa chọn khi ghé phố Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.703742 10.760539)'), 8, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Lẩu Cá A Mín', '333 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Lẩu Cá A Mín là quán lẩu cá đặc sản miền Tây, nổi bật với các món signature như lẩu cá lóc...', 
    '', ST_GeogFromText('POINT(106.70416 10.760595)'), 15, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Lẩu Nướng Chilli', 'Đường Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Lẩu Nướng Chilli là quán lẩu nướng tự chọn đang được giới trẻ ưa chuộng tại phố Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.704276 10.760621)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Lẩu Mẹt Nướng 79K', '833 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Lẩu Mẹt Nướng 79K là quán lẩu nướng giá rẻ theo hình thức 79.000đ/người (ăn không giới hạn)...', 
    '', ST_GeogFromText('POINT(106.704363 10.760702)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Ốc Vân', 'Đường Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Ốc Vân là quán ốc vỉa hè điển hình trên phố Vĩnh Khánh, chuyên các món ốc tươi xào...', 
    '', ST_GeogFromText('POINT(106.704418 10.760668)'), 8, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Lẩu Nướng An An Quán', '122/27 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Lẩu Nướng An An Quán là quán lẩu nướng hải sản được nhiều thực khách yêu thích...', 
    '', ST_GeogFromText('POINT(106.704482 10.760733)'), 8, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Ốc Đêm Vĩnh Khánh', '474 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Ốc Đêm Vĩnh Khánh là quán chuyên phục vụ ăn khuya, nổi bật với các món ốc và hải sản tươi sống...', 
    '', ST_GeogFromText('POINT(106.704571 10.760781)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Nhậu Ốc Đào', '232 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Nhậu Ốc Đào là quán nhậu chuyên ốc và hải sản tươi, nổi bật với các món ốc Đào xào...', 
    '', ST_GeogFromText('POINT(106.704759 10.760911)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Cá Biển Nướng', '231B Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Cá Biển Nướng chuyên phục vụ cá biển tươi nướng với các kiểu chế biến như nướng muối ớt...', 
    '', ST_GeogFromText('POINT(106.70484 10.760968)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Nem Nướng Đặc Sản Quê Nhà', '122/45 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Nem Nướng Đặc Sản Quê Nhà là quán nem nướng lâu đời với hơn 60 năm hoạt động...', 
    '', ST_GeogFromText('POINT(106.70489 10.760826)'), 8, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Ốc Bụi Quán', 'Đường Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Ốc Bụi là quán ốc bình dân được nhiều thực khách yêu thích tại phố Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.705879 10.761205)'), 15, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Cơm Gà Xối Mỡ Cô Hai', '8/11 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Cơm Gà Xối Mỡ Cô Hai là quán cơm gà xối mỡ dân dã nổi tiếng trong khu phố ẩm thực Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.706322 10.760946)'), 12, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Lẩu Sukiyaki Thái', 'Đường Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Lẩu Sukiyaki Thái là quán lẩu kiểu Thái với hương vị cay nồng đặc trưng...', 
    '', ST_GeogFromText('POINT(106.706356 10.760927)'), 8, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Ốc Phát Quán', '46 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Ốc Phát Quán là quán ốc và hải sản vỉa hè quen thuộc trên phố Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.706552 10.760814)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Cá Lóc Nướng Thy Thy', 'Đường Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Cá Lóc Nướng Thy Thy là quán chuyên cá lóc nướng trui – món đặc sản miền Tây dân dã...', 
    '', ST_GeogFromText('POINT(106.706579 10.760836)'), 6, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Quán Nướng 79', 'Đường Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Quán Nướng 79 là quán nướng bình dân với thực đơn đồ nướng vỉa hè đa dạng...', 
    '', ST_GeogFromText('POINT(106.706642 10.760765)'), 8, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Cô Út Nướng', '24 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Cô Út Nướng là quán nướng hải sản và thịt quen thuộc trên phố Vĩnh Khánh...', 
    '', ST_GeogFromText('POINT(106.70679 10.760673)'), 10, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
),
(
    'Vú Heo Nướng Tiến Lên', '89 Vĩnh Khánh,Phường 8,Quận 4,Thành Phố Hồ Chí Minh', 
    'Vú Heo Nướng Tiến Lên là quán nhậu chuyên vú heo nướng giòn...', 
    '', ST_GeogFromText('POINT(106.707169 10.76049)'), 15, 30000, 150000, 120, '["Good food", "Nice place"]'::JSONB, CURRENT_TIMESTAMP
);
