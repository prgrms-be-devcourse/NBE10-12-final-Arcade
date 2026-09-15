package com.back.domain.member.member.service;

import com.resend.core.exception.ResendException;

public interface ResendEmailGateway {
    void send(String from, String to, String subject, String html, String text) throws ResendException;
}
