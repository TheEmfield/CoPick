package com.copick.auth.dto;

public record UpdateMeRequest(String nickname, String oldPassword, String newPassword) {}
