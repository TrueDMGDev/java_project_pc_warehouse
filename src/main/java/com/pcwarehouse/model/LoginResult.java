package com.pcwarehouse.model;

public record LoginResult(boolean success, UserSession session, String message) {
}
