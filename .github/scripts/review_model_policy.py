import errno
import random


# 모델별 접근 제한·미지원·과부하에는 다음 후보가 성공할 수 있다.
FALLBACK_HTTP_CODES = {403, 404, 408, 429}
# Actions의 Linux errno(104·110)와 로컬 macOS errno(54·60)를 모두 인식한다.
NETWORK_ERRNOS = {errno.ECONNRESET, errno.ETIMEDOUT, 54, 60, 104, 110}
NETWORK_ERROR_NAMES = {
    "ConnectError",
    "ConnectTimeout",
    "ConnectionError",
    "ConnectionResetError",
    "PoolTimeout",
    "ReadError",
    "ReadTimeout",
    "TimeoutError",
    "WriteError",
    "WriteTimeout",
}


def _error_chain(error: Exception):
    """최상위 SDK 예외부터 네트워크 원인까지 중복 없이 순회한다."""
    seen = set()
    current = error
    while current is not None and id(current) not in seen:
        seen.add(id(current))
        yield current
        current = current.__cause__ or current.__context__


def classify_model_error(error: Exception) -> tuple[str, str]:
    """모델 오류를 다음 모델 폴백 또는 즉시 실패로 분류한다."""
    code = getattr(error, "code", None)
    if code in FALLBACK_HTTP_CODES:
        return "fallback", f"HTTP {code}"
    if isinstance(code, int) and code >= 500:
        return "fallback", f"HTTP {code}"

    for cause in _error_chain(error):
        if getattr(cause, "errno", None) in NETWORK_ERRNOS:
            return "fallback", f"네트워크 errno {cause.errno}"
        if type(cause).__name__ in NETWORK_ERROR_NAMES:
            return "fallback", type(cause).__name__

    return "raise", type(error).__name__


def _unique(models):
    """모델 목록의 순서를 유지하며 중복을 제거한다."""
    return list(dict.fromkeys(model for model in models if model))


def build_model_plan(group_1, group_2, rng=None) -> list[str]:
    """1그룹 무작위 모델·인접 모델·2그룹 무작위 모델 순으로 최대 3개를 고른다."""
    group_1 = _unique(group_1)
    group_2 = _unique(group_2)
    if not group_1:
        raise ValueError("MODEL_GROUP_1에 모델이 하나 이상 필요합니다.")

    rng = rng or random.SystemRandom()
    first_index = rng.randrange(len(group_1))
    plan = [group_1[first_index]]
    if len(group_1) > 1:
        plan.append(group_1[(first_index + 1) % len(group_1)])

    fallback_candidates = [model for model in group_2 if model not in plan]
    if fallback_candidates:
        plan.append(fallback_candidates[rng.randrange(len(fallback_candidates))])
    return plan
