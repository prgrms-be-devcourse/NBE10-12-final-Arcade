import os
import unittest
from unittest.mock import patch

os.environ.setdefault("REPO", "owner/repo")
os.environ.setdefault("PR_NUMBER", "1")

import ai_review


class ApiError(Exception):
    def __init__(self, code):
        self.code = code


class FakeModels:
    def __init__(self, outcomes):
        self.outcomes = iter(outcomes)
        self.calls = []

    def generate_content(self, model, **_kwargs):
        self.calls.append(model)
        outcome = next(self.outcomes)
        if isinstance(outcome, Exception):
            raise outcome
        return outcome


class FakeClient:
    def __init__(self, outcomes):
        self.models = FakeModels(outcomes)


class GenerateWithFallbackTest(unittest.TestCase):
    def test_503_uses_next_model_without_retry(self):
        expected = object()
        client = FakeClient([ApiError(503), expected])

        result, model = ai_review.generate_with_fallback(
            client, "system", "input", {}, ["primary", "fallback"]
        )

        self.assertIs(expected, result)
        self.assertEqual("fallback", model)
        self.assertEqual(["primary", "fallback"], client.models.calls)

    def test_connection_reset_uses_next_model(self):
        expected = object()
        client = FakeClient([ConnectionResetError(104, "Connection reset by peer"), expected])

        result, model = ai_review.generate_with_fallback(
            client, "system", "input", {}, ["primary", "fallback"]
        )

        self.assertIs(expected, result)
        self.assertEqual("fallback", model)
        self.assertEqual(["primary", "fallback"], client.models.calls)

    def test_other_5xx_uses_next_model(self):
        expected = object()
        client = FakeClient([ApiError(502), expected])

        result, model = ai_review.generate_with_fallback(
            client, "system", "input", {}, ["primary", "fallback"]
        )

        self.assertIs(expected, result)
        self.assertEqual("fallback", model)
        self.assertEqual(["primary", "fallback"], client.models.calls)

    def test_client_error_stops_without_fallback(self):
        client = FakeClient([ApiError(400)])

        with self.assertRaises(ApiError):
            ai_review.generate_with_fallback(
                client, "system", "input", {}, ["primary", "fallback"]
            )

        self.assertEqual(["primary"], client.models.calls)


class InputBudgetTest(unittest.TestCase):
    @patch.object(ai_review, "MAX_INPUT_CHARS", 1_000)
    def test_large_diff_is_truncated_without_token_count_request(self):
        diff = (
            "diff --git a/a.py b/a.py\n--- a/a.py\n+++ b/a.py\n@@ -1 +1,200 @@\n"
            + "".join(f"+line {number} xxxxxxxxxxxxxxxxxxxx\n" for number in range(200))
        )

        _, _, message, truncated = ai_review.fit_to_budget("system", diff, "title", "body")

        self.assertTrue(truncated)
        self.assertLessEqual(len("system") + len(message), 1_000)


if __name__ == "__main__":
    unittest.main()
