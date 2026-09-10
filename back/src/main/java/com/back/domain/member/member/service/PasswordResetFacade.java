package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetFacade {
    private final PasswordService passwordService;
    private final PasswordResetTokenStore passwordResetTokenStore;
    private final PasswordResetRequestRateLimiter rateLimiter;
    private final PasswordResetMailSender passwordResetMailSender;

    @Transactional
    public void requestReset(String email, String clientIp) {
        String normalizedEmail = passwordService.normalizeEmail(email);
        rateLimiter.check(normalizedEmail, clientIp);

        passwordService.findActiveMemberByEmail(normalizedEmail).ifPresent(member -> {
            String token = passwordResetTokenStore.issue(member.getId());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    passwordResetMailSender.sendResetLink(member, token);
                }
            });
        });
    }

    public void validateToken(String token) {
        if (!passwordResetTokenStore.isValid(token)) {
            throw invalidToken();
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String newPasswordConfirm) {
        Long memberId = passwordResetTokenStore.findValidMemberId(token);
        if (memberId == null) throw invalidToken();

        // 정책·확인값·기존 비밀번호 재사용까지 확인하기 전에는 일회용 토큰을 소진하지 않는다.
        Member member = passwordService.validatePasswordReset(
                memberId, newPassword, newPasswordConfirm
        );

        Long consumedMemberId = passwordResetTokenStore.consume(token);
        if (!memberId.equals(consumedMemberId)) throw invalidToken();

        passwordService.applyValidatedPasswordReset(member, newPassword);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                passwordResetMailSender.sendResetCompleted(member);
            }
        });
    }

    private ServiceException invalidToken() {
        return new ServiceException("400-3", "유효하지 않거나 만료된 비밀번호 재설정 링크입니다.");
    }
}
