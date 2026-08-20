package com.skibooking.service;

public interface VerificationEmailSender {

    void sendVerificationCode(String email, String code);
}
