"""
chat_service のユニットテスト。
LLM 呼び出しと DB 接続はモック化し、プロンプト選択・フォールバック挙動を検証する。
実行: pytest apps/ai/tests/
"""
from unittest.mock import MagicMock, patch

import pytest

from app.services.chat_service import (
    _build_career_prompt,
    _build_system_prompt,
    _format_career_context,
    process_chat,
)
from app.services.prompts.chat_prompts import (
    CAREER_SYSTEM_PROMPT_NO_DATA,
    HELP_SYSTEM_PROMPT,
    NORMAL_SYSTEM_PROMPT,
    PROOFREADING_SYSTEM_PROMPT,
)


class TestBuildSystemPrompt:
    def test_normal_mode(self):
        assert _build_system_prompt("NORMAL", 1) == NORMAL_SYSTEM_PROMPT

    def test_proofreading_mode(self):
        assert _build_system_prompt("PROOFREADING", 1) == PROOFREADING_SYSTEM_PROMPT

    def test_help_mode(self):
        assert _build_system_prompt("HELP", 1) == HELP_SYSTEM_PROMPT

    def test_unknown_mode_falls_back_to_normal(self):
        assert _build_system_prompt("INVALID", 1) == NORMAL_SYSTEM_PROMPT


class TestBuildCareerPrompt:
    @patch("app.services.chat_service._fetch_career_context")
    def test_returns_no_data_prompt_when_context_empty(self, mock_fetch):
        mock_fetch.return_value = ""
        result = _build_career_prompt(1)
        assert result == CAREER_SYSTEM_PROMPT_NO_DATA

    @patch("app.services.chat_service._fetch_career_context")
    def test_returns_no_data_prompt_on_db_error(self, mock_fetch):
        mock_fetch.side_effect = Exception("DB error")
        result = _build_career_prompt(1)
        assert result == CAREER_SYSTEM_PROMPT_NO_DATA

    @patch("app.services.chat_service._fetch_career_context")
    def test_injects_context_when_available(self, mock_fetch):
        mock_fetch.return_value = "【2025年度 棚卸サマリー】\n■ ITスキル\n  - Python: レベル 4"
        result = _build_career_prompt(1)
        assert "2025年度" in result
        assert "Python" in result


class TestFormatCareerContext:
    def test_empty_skills_and_goals(self):
        result = _format_career_context(2025, [], [], None)
        assert "未登録" in result
        assert "未設定" in result

    def test_with_skills(self):
        skills = [{"skill_name": "Python", "level_value": 4}]
        result = _format_career_context(2025, skills, [], None)
        assert "Python" in result
        assert "4" in result

    def test_with_goals(self):
        goals = [{"goal_category": "IT_SKILL", "target_name": "クラウド設計"}]
        result = _format_career_context(2025, [], goals, None)
        assert "クラウド設計" in result
        assert "ITスキル" in result

    def test_with_expectation(self):
        exp = {"tl_expectation": "リーダーシップを発揮してほしい", "company_expectation": None}
        result = _format_career_context(2025, [], [], exp)
        assert "リーダーシップ" in result


class TestProcessChat:
    @patch("app.services.chat_service.build_llm")
    @patch("app.services.chat_service._build_system_prompt")
    def test_calls_llm_with_history(self, mock_prompt, mock_build_llm):
        mock_llm = MagicMock()
        mock_llm.invoke.return_value = MagicMock(content="テスト応答")
        mock_build_llm.return_value = mock_llm
        mock_prompt.return_value = "システムプロンプト"

        history = [
            {"role": "user", "content": "こんにちは"},
            {"role": "assistant", "content": "こんにちは！"},
        ]
        result = process_chat("質問です", "NORMAL", 1, history)

        assert result == "テスト応答"
        mock_llm.invoke.assert_called_once()
        # SystemMessage + 2 history + 1 current = 4 messages
        call_args = mock_llm.invoke.call_args[0][0]
        assert len(call_args) == 4

    @patch("app.services.chat_service.build_llm")
    @patch("app.services.chat_service._build_system_prompt")
    def test_history_truncated_to_max(self, mock_prompt, mock_build_llm):
        mock_llm = MagicMock()
        mock_llm.invoke.return_value = MagicMock(content="応答")
        mock_build_llm.return_value = mock_llm
        mock_prompt.return_value = "プロンプト"

        history = [{"role": "user", "content": f"msg{i}"} for i in range(30)]
        process_chat("現在のメッセージ", "NORMAL", 1, history)

        call_args = mock_llm.invoke.call_args[0][0]
        # SystemMessage(1) + 最大20件 + current(1) = 22
        assert len(call_args) <= 22
