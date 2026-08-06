# OpenAI Fallback Provider — Design

## Problem

`GeminiService` is the sole AI backend for all 7 agent classes and `IssuerEvidenceGenerator`. Google's Gemini API started returning `429 RESOURCE_EXHAUSTED` due to the project's monthly spend cap being exceeded, which breaks all AI-agent features until the cap resets or is raised. This is a demo-reliability risk: if the cap is hit again mid-demo, every AI feature goes down with no recourse.

## Goal

Add an OpenAI-backed fallback so that if Gemini fails for any reason, the same logical request is retried against OpenAI, transparently, with no changes required in any calling code.

## Non-goals

- No new abstraction layer or provider-router service exposed to callers.
- No config-driven model selection (matches existing convention: Gemini's model is hardcoded in code today).
- No changes to agent classes, `IssuerEvidenceGenerator`, or `AgentController`.

## Architecture

`GeminiService` remains the single entry point every caller already depends on. It gains a dependency on a new `OpenAiService`, which mirrors `GeminiService`'s method surface (text, JSON-mode, history, multimodal). Every public `GeminiService` method wraps its existing Gemini call: if the Gemini call throws (after Gemini's own internal 429/503 retry loop is exhausted), and an OpenAI key is configured, `GeminiService` retries the same logical request against `OpenAiService` and returns that result instead. If the OpenAI call also fails, or no OpenAI key is configured, the original Gemini exception propagates as it does today.

```
Agent classes / IssuerEvidenceGenerator
              |
              v
        GeminiService  (unchanged public interface)
         |          \
         v           v
   Gemini API    OpenAiService --> OpenAI API
   (primary)      (fallback, used only on Gemini failure)
```

## Provider semantics

- **Gemini is strictly primary.** `isAvailable()` keeps its current meaning (Gemini key present). If the Gemini key is missing, behavior is unchanged from today — the call throws immediately with "Gemini API key not configured". OpenAI is never used as a substitute for a missing Gemini key, only as a fallback when a configured Gemini call fails at runtime.
- **Fallback trigger: any Gemini failure**, not just 429/503. This includes quota errors, network errors, timeouts, and malformed responses — a working demo cares more about getting an answer than about which failure category occurred.
- If OpenAI key isn't configured, Gemini failures propagate exactly as they do today (no behavior change for users who haven't set up OpenAI).

## OpenAiService

New class alongside `GeminiService`, reading `openai.api-key` (env var `OPENAI_API_KEY`, matching the exact `${VAR:}` pattern Gemini uses). Model is hardcoded as `gpt-4o-mini`, matching the existing convention of hardcoding the model name in code (see `GeminiService.GEMINI_API_URL`).

All requests — text, JSON-mode, history, images, and PDFs — go through Chat Completions (`/v1/chat/completions`). OpenAI's Chat Completions API supports native PDF input directly (confirmed against current OpenAI docs: PDFs are sent as a `file` content part with base64 `file_data`, supported on `gpt-4o-mini` since March 2025), so no separate Responses API path is needed — full parity with Gemini's multimodal support in a single code path.

Method mapping to match `GeminiService`:

| GeminiService method | OpenAiService equivalent |
|---|---|
| `generateContent` | plain chat completion |
| `generateJson` | chat completion with `response_format: {type: "json_object"}` |
| `generateContentWithHistory` | chat completion with prior turns mapped to messages |
| `generateJsonWithHistory` / `generateJsonWithHistoryAndMedia` | JSON mode + history + media parts |
| `generateMultimodalContent` / `generateMultimodalJson` | media parts (image/PDF) attached to the user message |

Request mapping details:
- System prompt → a `system` role message.
- Gemini history role `"model"` → OpenAI role `"assistant"`; Gemini `"user"` → OpenAI `"user"`.
- Images (`MediaFile.isImage()`) → `image_url` content part with a base64 data URI.
- PDFs (`MediaFile.isPdf()`) → `file` content part: `{"type": "file", "file": {"filename": ..., "file_data": "data:application/pdf;base64,..."}}`.
- Existing `MediaFile` filtering (`MAX_MEDIA_FILES`, `MAX_MEDIA_FILE_SIZE`, `MAX_TOTAL_MEDIA_SIZE`) in `GeminiService` already runs before either provider is called, so `OpenAiService` receives pre-filtered media.

## Logging

- `log.warn` in `GeminiService` when falling back to OpenAI, including the Gemini failure reason.
- `log.info` confirming which provider actually served each response, so demo logs make it obvious which path was used.

## Config

`openai.api-key=${OPENAI_API_KEY:}` added to both `application.properties` (local, gitignored, already updated) and `application.properties.example` (tracked template, already updated) — directly beside the existing `gemini.api-key` line, matching its exact pattern.

## Testing

The repo currently has no service-level unit tests (only a Spring context smoke test). This change adds the first ones, using JUnit 5 + Mockito (already on the classpath via `spring-boot-starter-test`):

- `GeminiService` fallback branching: mock a Gemini failure, assert `OpenAiService` is invoked with an equivalent request and its result is returned; assert no fallback occurs when OpenAI key is absent.
- `OpenAiService` request building and response parsing: text, JSON mode, history role mapping (`model` → `assistant`), image and PDF content parts.
