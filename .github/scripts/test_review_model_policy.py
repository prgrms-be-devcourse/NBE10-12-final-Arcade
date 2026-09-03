import unittest

from review_model_policy import build_model_plan, classify_model_error


class ApiError(Exception):
    def __init__(self, code):
        self.code = code


class ReviewModelPolicyTest(unittest.TestCase):
    def test_quota_and_unavailable_errors_fall_back(self):
        self.assertEqual(("fallback", "HTTP 404"), classify_model_error(ApiError(404)))
        self.assertEqual(("fallback", "HTTP 429"), classify_model_error(ApiError(429)))
        self.assertEqual(("fallback", "HTTP 503"), classify_model_error(ApiError(503)))

    def test_connection_reset_falls_back(self):
        wrapped = RuntimeError("request failed")
        wrapped.__cause__ = ConnectionResetError(104, "Connection reset by peer")

        action, reason = classify_model_error(wrapped)

        self.assertEqual("fallback", action)
        self.assertIn("104", reason)

    def test_other_server_errors_fall_back(self):
        self.assertEqual(("fallback", "HTTP 502"), classify_model_error(ApiError(502)))

    def test_client_errors_fail_without_fallback(self):
        self.assertEqual(("raise", "ApiError"), classify_model_error(ApiError(400)))
        self.assertEqual(("raise", "ApiError"), classify_model_error(ApiError(401)))


class FakeRandom:
    def __init__(self, indexes):
        self.indexes = iter(indexes)

    def randrange(self, size):
        index = next(self.indexes)
        if index >= size:
            raise AssertionError(f"{index} is outside randrange({size})")
        return index


class BuildModelPlanTest(unittest.TestCase):
    def test_random_group1_then_adjacent_with_wrap_then_group2(self):
        plan = build_model_plan(
            ["flash-a", "flash-b", "flash-c"],
            ["flash-lite"],
            FakeRandom([2, 0]),
        )

        self.assertEqual(["flash-c", "flash-a", "flash-lite"], plan)

    def test_duplicate_models_are_not_retried(self):
        plan = build_model_plan(
            ["flash-a", "flash-a"],
            ["flash-a", "flash-lite"],
            FakeRandom([0, 0]),
        )

        self.assertEqual(["flash-a", "flash-lite"], plan)


if __name__ == "__main__":
    unittest.main()
