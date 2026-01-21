package com.example.audioguide.modules.store.repository;

import com.example.audioguide.modules.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Interface giao tiếp với Database thông qua JPA.
 * Mặc định đã có sẵn các hàm: findAll(), findById(), save(), delete()...
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    // Không cần viết thêm query gì cả, vì chúng ta sẽ lọc (filter) ở tầng Service
    // (Java Code)
}
