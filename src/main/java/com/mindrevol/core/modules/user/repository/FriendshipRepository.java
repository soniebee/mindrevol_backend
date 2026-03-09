package com.mindrevol.core.modules.user.repository;

import com.mindrevol.core.modules.user.entity.Friendship;
import com.mindrevol.core.modules.user.entity.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, String> {

    /**
     * Kiểm tra xem 2 user có quan hệ follow/friend không
     */
    boolean existsByUserIdAndFriendIdAndStatus(String userId, String friendId, FriendshipStatus status);

    /**
     * Tìm quan hệ giữa 2 user
     */
    Optional<Friendship> findByUserIdAndFriendId(String userId, String friendId);

    /**
     * Xóa quan hệ giữa 2 user
     */
    void deleteByUserIdAndFriendId(String userId, String friendId);

    /**
     * Lấy danh sách followers (những người follow user này)
     */
    @Query("SELECT f.user FROM Friendship f WHERE f.friend.id = :userId AND f.status = 'FOLLOWING'")
    Page<Object> findFollowersOfUser(@Param("userId") String userId, Pageable pageable);

    /**
     * Lấy danh sách following (những người mà user này đang follow)
     */
    @Query("SELECT f.friend FROM Friendship f WHERE f.user.id = :userId AND f.status = 'FOLLOWING'")
    Page<Object> findFollowingOfUser(@Param("userId") String userId, Pageable pageable);
}

