package com.yongyongthon.be.common.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationToken {
    private String accessToken;
    private String refreshToken;
}
