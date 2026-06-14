package com.example.demo.dto;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String idToken;
    private String role;
}
