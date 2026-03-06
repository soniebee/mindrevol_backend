package com.mindrevol.core.modules.auth.repository;

import com.mindrevol.core.modules.auth.entity.RegisterTempData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho RegisterTempData lưu trữ trên Redis
 */
@Repository
public interface RegisterTempDataRepository extends CrudRepository<RegisterTempData, String> {

    /**
     * Tìm dữ liệu tạm theo email
     */
    Optional<RegisterTempData> findByEmail(String email);
}

