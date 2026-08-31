package com.back.global.globalExceptionHandler;

import com.back.global.exception.ServiceException;

public class ContestForbiddenException extends ServiceException {
    public ContestForbiddenException() {
        super("403-1", "주최측 또는 관리자만 수행할 수 있습니다.");
    }
}
