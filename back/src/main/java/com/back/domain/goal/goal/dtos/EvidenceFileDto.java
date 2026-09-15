package com.back.domain.goal.goal.dtos;

import java.io.InputStream;

/**
 * 내려보낼 증빙 파일. content 는 스토리지에서 막 연 스트림이라 다 쓰면 닫아야 한다
 * (컨트롤러가 InputStreamResource 로 감싸면 서블릿이 닫는다).
 *
 * mimeType 은 담지 않는다 - 응답은 항상 application/octet-stream 이다.
 * 저장된 mimeType 은 업로더가 보낸 문자열이라 그대로 실으면 text/html 을 우리 도메인에서 실행시킬 수 있다.
 */
public record EvidenceFileDto(String fileName, Long size, InputStream content) { }
