package com.mindrevol.core.modules.auth.mapper;

import com.mindrevol.core.modules.auth.dto.request.RegisterStep1Dto;
import com.mindrevol.core.modules.auth.dto.request.RegisterStep2Dto;
import com.mindrevol.core.modules.auth.dto.request.RegisterStep3Dto;
import com.mindrevol.core.modules.auth.dto.response.RegistrationResponse;
import com.mindrevol.core.modules.auth.entity.RegisterTempData;
import com.mindrevol.core.modules.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class để chuyển đổi giữa Entities và DTOs trong module Auth
 */
@Component
public class AuthMapper {

    /**
     * Chuyển đổi từ User entity sang RegistrationResponse
     */
    public RegistrationResponse toRegistrationResponse(User user, String accessToken, String refreshToken, String message) {
        if (user == null) {
            return null;
        }

        return RegistrationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .handle(user.getHandle())
                .fullname(user.getFullname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message(message)
                .build();
    }

    /**
     * Cập nhật RegisterTempData từ RegisterStep1Dto
     */
    public void updateTempDataFromStep1(RegisterTempData tempData, RegisterStep1Dto dto) {
        if (dto == null || tempData == null) {
            return;
        }
        tempData.setEmail(dto.getEmail());
        tempData.setPassword(dto.getPassword());
    }

    /**
     * Cập nhật RegisterTempData từ RegisterStep2Dto
     */
    public void updateTempDataFromStep2(RegisterTempData tempData, RegisterStep2Dto dto) {
        if (dto == null || tempData == null) {
            return;
        }
        tempData.setHandle(dto.getHandle());
        tempData.setFullname(dto.getFullname());
    }

    /**
     * Cập nhật RegisterTempData từ RegisterStep3Dto
     */
    public void updateTempDataFromStep3(RegisterTempData tempData, RegisterStep3Dto dto) {
        if (dto == null || tempData == null) {
            return;
        }
        if (dto.getDateOfBirth() != null) {
            tempData.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getGender() != null) {
            tempData.setGender(dto.getGender());
        }
    }
}
