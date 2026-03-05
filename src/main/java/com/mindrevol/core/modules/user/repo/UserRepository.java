package com.mindrevol.core.module.user.repo;

import com.mindrevol.core.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // Hiện tại chưa cần viết thêm hàm gì, JpaRepository đã có sẵn save(), findById()...
}