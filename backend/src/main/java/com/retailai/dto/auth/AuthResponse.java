package com.retailai.dto.auth;

public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresInMs;
    private UserSummaryDto user;

    public AuthResponse() {}

    public AuthResponse(String accessToken, long expiresInMs, UserSummaryDto user) {
        this.accessToken = accessToken;
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }

    public UserSummaryDto getUser() { return user; }
    public void setUser(UserSummaryDto user) { this.user = user; }
}
