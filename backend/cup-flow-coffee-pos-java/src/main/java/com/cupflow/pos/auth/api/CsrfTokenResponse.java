package com.cupflow.pos.auth.api;

public record CsrfTokenResponse(String headerName, String token) {}
