package com.mindrevol.core.modules.user.dto.request;

import com.mindrevol.core.modules.user.entity.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullname;

    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Handle can only contain letters, numbers, dots, and underscores")
    @Size(min = 3, max = 30, message = "Handle must be between 3 and 30 characters")
    private String handle;

    @Size(max = 150, message = "Bio must be at most 150 characters")
    private String bio;

    @Size(max = 255, message = "Invalid website")
    private String website;

    private String avatarUrl;
    
    private String timezone;

    // --- Bổ sung ---
    @Past(message = "Invalid date of birth")
    private LocalDate dateOfBirth;

    private Gender gender;
}