package com.foodstreet.voice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Payload for first admin registration")
public class RegisterRequest {
    @Schema(example = "admin")
    @NotBlank
    @Size(min = 3, max = 100)
    private String username;

    @Schema(example = "admin@streetvoice.local")
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Schema(example = "Admin@12345")
    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @Schema(example = "ADMIN", description = "User role (ADMIN or RESTAURANT_OWNER)")
    private String role;
}
