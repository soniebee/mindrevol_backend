package com.mindrevol.core.modules.user.repository;

import com.mindrevol.core.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByHandle(String handle);

    boolean existsByEmail(String email);

    boolean existsByHandle(String handle);

    /**
     * Tìm kiếm user theo handle hoặc fullname
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.handle) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.fullname) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchByHandleOrFullname(@Param("query") String query, Pageable pageable);

    /**
     * Tìm user theo provider và providerId
     */
    Optional<User> findByAuthProvider(String authProvider);
}


