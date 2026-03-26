package com.foodstreet.voice.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserSummary {
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
}
