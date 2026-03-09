package com.mindrevol.core.modules.user.mapper;

import com.mindrevol.core.modules.user.dto.request.UpdateProfileDto;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.core.modules.user.dto.response.UserPublicResponse;
import com.mindrevol.core.modules.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class để chuyển đổi giữa Entities và DTOs trong module User
 */
@Component
public class UserMapper {

    /**
     * Chuyển đổi từ User entity sang UserProfileResponse
     */
    public UserProfileResponse toUserProfileResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .handle(user.getHandle())
                .fullname(user.getFullname())
                .dateOfBirth(user.getDateOfBirth())
                .bio(user.getBio())
                .website(user.getWebsite())
                .avatarUrl(user.getAvatarUrl())
                .timezone(user.getTimezone())
                .gender(user.getGender())
                .authProvider(user.getAuthProvider())
                .status(user.getStatus())
                .accountType(user.getAccountType())
                .subscriptionExpiryDate(user.getSubscriptionExpiryDate())
                .points(user.getPoints())
                .isPremium(user.isPremium())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi từ User entity sang UserPublicResponse (thông tin công khai)
     */
    public UserPublicResponse toUserPublicResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserPublicResponse.builder()
                .id(user.getId())
                .handle(user.getHandle())
                .fullname(user.getFullname())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .website(user.getWebsite())
                .build();
    }

    /**
     * Cập nhật User entity từ UpdateProfileDto
     */
    public void updateUserFromDto(User user, UpdateProfileDto dto) {
        if (dto == null || user == null) {
            return;
        }

        if (dto.getFullname() != null) {
            user.setFullname(dto.getFullname());
        }
        if (dto.getDateOfBirth() != null) {
            user.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        if (dto.getWebsite() != null) {
            user.setWebsite(dto.getWebsite());
        }
        if (dto.getTimezone() != null) {
            user.setTimezone(dto.getTimezone());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
    }
}

