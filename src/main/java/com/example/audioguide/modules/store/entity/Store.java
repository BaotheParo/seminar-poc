package com.example.audioguide.modules.store.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho một quán ăn, địa điểm cần thuyết minh.
 * Mapping với bảng "stores" trong database.
 */
@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID tự tăng

    private String name; // Tên địa điểm (Ví dụ: Hồ Con Rùa)

    private Double lat; // Vĩ độ (Latitude) - Lưu Double thuần, KHÔNG dùng PostGIS
    private Double lng; // Kinh độ (Longitude)

    private String audioPath; // Đường dẫn file thuyết minh (Ví dụ: /media/ho_con_rua.mp3)
}
