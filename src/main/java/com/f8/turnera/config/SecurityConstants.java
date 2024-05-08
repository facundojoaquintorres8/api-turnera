package com.f8.turnera.config;

public final class SecurityConstants {

    public static final long EXPIRATION_TIME = 604_800_000; // 7 days
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_TOKEN = "Authorization";
    public static final String PERMISSIONS_KEY = "permissions";
    public static final String ORGANIZATION_KEY = "organization_id";

    private SecurityConstants() {}
}
