# AIChatApp

AIChatApp is a local Android chat client for multiple AI providers. The first version focuses on text chat, provider configuration, encrypted API keys, streaming responses, and local conversation history.

## Stack

- Kotlin, Jetpack Compose, Material3
- Room for providers, conversations, and messages
- DataStore for lightweight UI preferences
- AndroidX Security Crypto for API keys
- OkHttp for provider requests and SSE-style streaming
- Shared Android tooling under `D:\Projects\Personal\.devtools\android`

## Supported provider shapes

- OpenAI Responses: `POST /v1/responses`
- OpenAI-compatible Chat Completions: `POST /chat/completions`
- TokenHubProxy: compatible with the local proxy at `http://127.0.0.1:8787/v1`

API keys are not stored in Room and should never be committed. Provider metadata stores only a secret reference.

## Build

```powershell
cd D:\Projects\Personal\AI\AIChatApp
. D:\Projects\Personal\scripts\use-android-env.ps1
.\gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon
.\gradlew.bat :app:assembleDebug --console=plain --no-daemon
```
