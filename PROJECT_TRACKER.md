# AIChatApp Project Tracker

## 2026-06-02 Local Update Summary

- Tracker sync: `ATTACH-012` now matches the completed in-app PDF/text attachment preview work already tracked by `NEXT-P2-001`.
- Validation: source search confirms PDF/text attachments route through `canPreviewInApp`, `AttachmentPreviewDialog`, `PdfRenderer`, and `readAttachmentPreviewText`.
- Done: conversation drawer rows now show a fork-source label with a branch icon, resolving the source conversation title when available and showing a missing-source state when the original conversation is no longer in the list.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest.conversationForkSourceLabelShowsSourceTitleOrMissingSource`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the drawer fork-source label change.
- Done: Settings now includes a one-time cleanup action for historical assistant/group bot messages that saved raw DSML tool markup, converting matching XML blocks into readable tool-call summaries while leaving ordinary/user messages untouched.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest.historicalDsmlCleanerConvertsMarkupToReadableToolSummary --tests com.personal.aichat.ChatRepositoryForkTest.cleanupHistoricalDsmlToolMarkupUpdatesSingleAndGroupAssistantMessages`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the historical DSML cleanup tool change.
- Done: grouped tool-call cards now vary their header/detail icons and background watermark by tool type, using search, page-open, file, or generic tool visuals for faster scanning.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest.toolCallVisualKindVariesByToolName` passed after the grouped tool-card visual-kind change.
- Done: group chats now show a semi-automatic summary refresh prompt when a long discussion has no summary yet or has enough new messages after the last summary, reusing the existing summary bot picker for one-click updates.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest.groupSummaryRefreshHintPromptsWhenSummaryIsMissingAfterEnoughMessages --tests com.personal.aichat.ChatRepositoryForkTest.groupSummaryRefreshHintSkipsFreshSummary --tests com.personal.aichat.ChatRepositoryForkTest.groupSummaryRefreshHintPromptsWhenSummaryIsStale` passed after the group summary refresh prompt change.
- Done: group auto-play now supports per-group saved player preferences for finite rounds, turn interval, and one retry for failed bot turns, with a group overflow settings dialog.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest.groupAutoPlayPreferenceDefaultsAndNormalizes --tests com.personal.aichat.ChatRepositoryForkTest.nextGroupAutoPlayBotCyclesAfterMostRecentBotMessage`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the group player enhancement.
- Done: group-chat bot identity visuals now show stable avatar initials, a short identity code, and the resolved color label in bot bubbles, bot picker rows, and bot manager rows.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest.botIdentityLabelsAreCompactAndStable`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the bot identity visual change.
- Done: Room migration tests now cover seeded schema 1 to the latest schema and every exported schema to the latest schema, with Robolectric execution plus an instrumentation-test counterpart.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatDatabaseMigrationRobolectricTest`, `:app:compileDebugAndroidTestKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the Room migration test change.
- Done: Provider config QR export/import is now available from Settings, using compressed QR payloads, an in-app QR preview dialog, and scanner-based import that reuses the existing provider import path.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the provider QR import/export change.
- Done: PDF and text attachments now open in an in-app preview dialog, with PDF first-page rendering, text selection/scrolling, and a system-open fallback.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the in-app attachment preview change.
- Done: group-chat editing can now save a multi-preset background combination per group, reinsert that group's common combination, and insert multiple selected presets at once.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the group background preset combination change.
- Done: background presets now support optional categories, default presets ship with categories, Settings can edit/filter by category, and the group-chat preset insertion dialog can filter and display categories.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the background preset category change.
- Done: background presets now support JSON export/share and JSON import from Settings; imports append cleaned presets, regenerate conflicting IDs, and keep existing presets intact.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the background preset import/export change.
- Done: background presets can now be searched by title/content in both Settings and the group-chat preset insertion dialog, with multi-keyword matching and empty states.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the background preset search change.
- Done: API configuration management now shows a provider capability matrix for image input, file input, web search tools, and reasoning support, with compact capability badges on each provider row.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the provider capability matrix change.
- Done: favorite detail view now supports selecting multiple messages in a snippet and removing them from the favorite while enforcing that at least one message remains.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the favorite batch message-removal change.
- Done: favorite library now has a sort menu for recent updates, created time, title, and primary tag ordering after search/tag filtering.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the favorite sorting change.
- Done: favorite batch mode now supports adding one or more tags to selected favorite snippets, merging with existing tags and de-duplicating case-insensitively.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the favorite batch-tagging change.
- Done: favorite library now has a batch mode with current-filter select-all, per-card checkboxes, and batch deletion for selected favorite snippets.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the favorite batch-delete change.
- Done: favorite search results now highlight matched text in titles, descriptions, tags, source/model metadata, and show a highlighted body snippet when message content matches the query.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the favorite search highlighting change.
- Done: conversation and group-chat export messages now carry structured attachment metadata, and exported Markdown/text/long-image content includes an attachment index with file name, MIME type, and size.
- Done: added repository tests covering attachment indexes in single-chat and group-chat exports/share text.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the export attachment-index change.
- Done: group chat overflow menu now exposes Markdown file export, reusing the existing group share text for full chats and selected group-message subsets.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the group Markdown export entry change.
- Done: incoming system-share target picker now supports searching new single-chat providers, existing single chats, and group chats by title/topic/model/provider/bot fields.
- Done: share-target search keeps attachment compatibility filtering for new/existing single chats and shows targeted empty-state text when no destination matches.
- Done: source-level mojibake scan is clean again; historical tool-output mojibake compatibility labels were moved behind ASCII Unicode escape constants.
- Validation: `:app:compileDebugKotlin`, `:app:assembleDebug`, and the pre-commit mojibake scan passed after the share-target search change.
- Done: provider context-window override input now accepts compact forms such as `1M`, `400K`, `1.5m`, `1,000,000`, and plain token counts instead of digits-only input.
- Done: provider settings show the parsed token count while editing and disable saving for unrecognized context-window values, preventing accidental `1M -> 1 token` or `400K -> 400 tokens` saves.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ContextWindowTest` and `:app:assembleDebug` passed after the context-window input parsing change.

## 2026-05-30 Local Update Summary

- Done: debug builds now install with the distinct Android launcher label `AI Chat Debug`, while release builds keep the normal `AI Chat` label.
- Done: added `scripts/build-release-apk.ps1` to build signed release APKs into `app/build/outputs/apk/release` using the local JDK, Android SDK, Gradle user home, and existing release signing properties.
- Done: release signing prerequisites were verified locally after the release keystore was restored; `:app:validateSigningRelease` and `:app:assembleRelease` completed successfully and produced `app-release.apk`.
- Done: Markdown headings now render inline Markdown, so heading text such as `建议的**周模板**` no longer leaks raw `**` markers.
- Done: DeepSeek/OpenAI-compatible DSML tool markup now handles the full-width `｜｜DSML｜｜` marker and normalizes model-emitted `open_url` / `open_url_page` tool names to the supported `open` tool, preventing raw DSML tool XML from being saved as assistant body text.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ProviderAdapterTest` and `:app:assembleDebug` passed after the DSML/open_url and Markdown heading fixes.
- Done: Settings now provides a one-time cleanup action for historical assistant and group bot messages that already saved raw DSML markup; the cleanup rewrites matching tool XML blocks into readable tool-call summaries and leaves non-assistant messages untouched.

## 2026-05-29 Local Update Summary

- Done: improved dark mode readability for system bars, main top bars, single-chat user bubbles, group-chat user bubbles, and group bot Markdown rendering.
- Done: group AI interruption UX now exposes a stop action during generation, keeps the partial draft as a stopped message, and marks stopped group context with `[已停止]` for later bot turns.
- Done: folder behavior now treats non-empty `groupName` as real folders only; foldered chats are hidden from the loose chat list, deleting a folder moves chats back to the ungrouped list, and blank/default folders are not shown as folders.
- Done: selecting a single chat from the drawer while a group chat is open now closes the group chat overlay so the selected single chat is visible immediately.
- Done: app back/close behavior now closes the topmost UI layer first, including drawer, settings, provider manager, API detail dialogs, bot manager, favorites, share target picker, and local edit dialogs; root single/group chat pages still allow Android to exit to launcher.
- Done: single-chat tool calls now aggregate into an attached tool card above the matching assistant response; group tool cards share the same narrower visual treatment.
- Done: message/tool bubble bottoms are more compact, and grouped tool cards now use a subtle search watermark for clearer visual separation from normal response bubbles.
- Done: GPT image-generation mode adds dedicated IMAGE conversations, Room v13/v14 fields, OpenAI Responses `image_generation` support, Images API `/images/generations` and `/images/edits` support for compatible middlemen, reference-image input, local generated-image attachments, image-mode composer controls for size/quality/count/format/background, and debug raw-response logging.
- Done: generated-image previews can now open with system apps without crashing; `chat_generated_images` is covered by FileProvider and external open failures show a lightweight toast.
- Done: provider/network errors now surface clearer Chinese guidance for invalid requests, API keys, permissions, model/Base URL mismatches, rate limits, quota/billing issues, and 5xx upstream failures.
- Done: large image attachments now keep the original local preview while sending a downscaled/compressed payload to providers; pending upload limits are calculated from the provider payload size.
- Done: attachment limits are configurable in settings, covering single upload payload, total pending payload, and original image import caps while keeping the existing defaults.
- Done: grouped tool-call cards now parse and show query text, opened page URLs, and citation URLs separately before the raw input/output details.
- Done: favorite tag management now supports renaming, merging into an existing tag, and deleting tags without deleting favorite content.
- Done: favorites can now export all snippets as JSON for restore or Markdown for reading, and import JSON exports back into the local favorites library.
- Planned: improve full long-image export for grouped tool cards, structure tool source URLs more explicitly, and consider persistent single-chat turn ids if UI-order grouping is not enough later.
- Done: grouped tool card watermarks and icons now vary by tool type, using search for `web_search`, page-open for `open`/`open_page`/`open_url`, file for `web_fetch`/file tools, and a generic tool icon otherwise.
- Planned: extend image-generation mode with masks/local edit regions, reusable generation parameters, richer image export layouts, and DeepSeek support only if an official image-generation API becomes available.
- Validation: `:app:testDebugUnitTest`, `:app:compileDebugKotlin`, and `:app:assembleDebug` passed after the GPT image-generation mode changes; `:app:compileDebugKotlin` and `:app:assembleDebug` also passed after the generated-image external-open fix.
- Done: context capacity is estimated per provider/model and displayed in single-chat and group-chat headers. Provider settings now allow a manual context-window override for custom or proxy models.
- Done: single-chat and group-chat sends can automatically compress older context into persistent summaries before continuing, while leaving the visible message history unchanged.
- Done: mobile top bars now keep the title row compact and move group/provider/model/context capacity into a horizontally scrollable metadata strip, avoiding truncation on narrow screens.
- Done: streaming AI and tool bubbles now show configurable in-progress motion, with breathing borders/status dots for single chat, group chat, and tool cards.
- Validation: `:app:testDebugUnitTest --tests com.personal.aichat.ChatRepositoryForkTest` passed after the context capacity and auto-compression changes.

## 2026-05-29 Context Capacity Update

- CONTEXT-001: Added Room v15 context fields for provider context-window overrides, single-chat compression summaries, and group-chat compression summaries.
- CONTEXT-002: Added local approximate token estimation and known model context-window lookup. The UI labels the capacity as approximate and falls back to "unknown" when no window is known.
- CONTEXT-003: Single chats now build request context from "compressed summary + messages after cutoff", and can auto-compress when estimated usage crosses the safety threshold.
- CONTEXT-004: Group chats now keep a separate compression summary from the existing group summary, and point/manual/auto bot turns can compress before continuing.
- CONTEXT-005: Added manual "compress context now" actions in single-chat and group-chat overflow menus.
- UI-015: Moved long provider/model/context labels out of the constrained title row into a scrollable metadata strip for narrow phone screens.
- UI-016: Added configurable streaming bubble motion (`standard`, `subtle`, `off`) so partial AI/tool output remains visibly in progress during stream stalls.
- RISK-014: Context capacity uses local approximate estimation rather than model-specific tokenizers. Keep the "approximate" UI wording and prefer provider overrides for custom/proxy models.

更新时间：2026-05-29

## 1. 项目目标

AIChatApp 是一个本地 Android 多 Provider AI 聊天客户端，目标是在移动端统一管理多组 AI API 配置、固定会话模型、保存本地聊天历史，并逐步支持工具调用、联网搜索、多模态附件、成果收藏、分享导出和后台生成。

当前产品规则：

- 会话创建后固定 `providerId/model`，已有会话不通过顶部控件切换模型。
- 模型对比通过“从某条消息分叉到其他 Provider/model”的方式完成。
- OpenAI Responses / TokenHubProxy 可使用联网搜索和图片/文件附件。
- DeepSeek / OpenAI-compatible Chat 当前以文本、函数式 web_search 和网页抓取工具回传为主，DeepSeek 官方 API 当前不支持图片/文件附件输入。

## 2. 状态和优先级

| 状态 | 含义 |
| --- | --- |
| Done | 已实现并通过基础验证 |
| In Progress | 已部分实现，还需要继续完善 |
| Planned | 已规划，尚未开始 |
| Watch | 可用但需要真机或长期稳定性观察 |
| Blocked | 被外部条件阻塞 |

| 优先级 | 含义 |
| --- | --- |
| P0 | 核心可用性、安全性、构建发布、聊天主链路 |
| P1 | 高频体验能力，明显影响日常使用 |
| P2 | 增强功能、体验细节、可维护性 |
| P3 | 长期扩展、实验能力、低频高级能力 |

## 3. 当前总体进度

| 模块 | 状态 | 进度 | 说明 |
| --- | --- | --- | --- |
| Android 项目骨架 | Done | 100% | Kotlin + Compose + Material3 + Room + DataStore + OkHttp |
| 本机开发环境文档 | Done | 100% | 已按本机真实路径整理本地构建说明 |
| Provider 抽象 | Done | 93% | OpenAI Responses、OpenAI-compatible、TokenHubProxy 已接入，支持 Provider 删除与机器人批量改绑，Anthropic/Gemini 预留 |
| 会话固定模型 | Done | 100% | 请求实际使用 `conversation.model`，不再临时读 provider default |
| 会话分叉 | Done | 95% | 支持从消息气泡分叉到其他 Provider，并保存来源关系 |
| OpenAI Responses | Done | 92% | 支持流式、reasoning effort、usage 元数据、web_search、附件输入，并修复群聊首轮搜索上下文兼容 |
| DeepSeek / OpenAI-compatible | Done | 92% | 支持 Chat Completions 流式、usage、函数式 web_search、DSML open/web_fetch 工具回传 |
| 联网搜索 | Done | 92% | OpenAI hosted web_search + compatible function calling + 官方 DeepSeek 搜索结果 + DuckDuckGo/Bing fallback |
| 工具调用 UI | Done | 86% | 工具调用独立卡片、可折叠、显示查询词和 URL；群聊同轮工具调用已聚合展示 |
| 多模态附件 | Done | 82% | Provider 级附件开关、图片/文件选择、拍照、系统分享导入、气泡附件展示、图片 app 内预览 |
| 设置页 | Done | 90% | 独立设置页，支持主题色、夜间模式、字体、debug log、搜索模式、背景预设、配置导入导出、Provider 删除/改绑 |
| 聊天 UI | In Progress | 88% | 顶栏压缩、输入框修复、自动追踪滚动、选择模式、Markdown 表格与分隔线、长 AI 气泡侧边跳转和浮动操作已优化 |
| 会话列表 | In Progress | 85% | 抽屉、折叠文件夹、重命名、日期分组、创建/更新时间显示已实现 |
| 成果收藏 | Done | 85% | 支持收藏单条/多条消息为片段，保存快照、来源、标题、标签、描述，并可搜索、查看、追加、移除、分享 |
| 多 AI 群聊 | Done | 91% | 新增常驻 AI 机器人、独立群聊表、手动点名、编辑/删除、播放器式轮流发言、群摘要、机器人颜色、历史气泡折叠、工具消息聚合和基础 UI |
| 后台生成 | Done | 80% | 前台服务保活，后台完成后通知 |
| 分享导出 | In Progress | 84% | 文本、Markdown 文件、长图、单气泡分享、群聊消息分享、长气泡浮动分享入口和系统分享接入已实现，超长内容仍需优化 |
| 自动化测试 | In Progress | 76% | Provider adapter、fork、附件请求体、web_search、收藏片段、多 AI 群聊仓库链路、Provider 删除/改绑、群聊播放器和长气泡导航逻辑已有单测 |
| CI / Release | Done | 90% | Jenkins 本地 release pipeline 和签名配置已接入 |

## 4. 已完成事项

### 4.1 Provider 与请求链路

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| API-001 | 统一 Provider 数据模型和 adapter 接口 | Done | P0 | `ChatProviderConfig` / `ProviderAdapter` 已落地 |
| API-002 | OpenAI Responses adapter | Done | P0 | `/v1/responses` 支持 SSE delta、usage、reasoning |
| API-003 | OpenAI-compatible Chat adapter | Done | P0 | `/chat/completions` 支持流式 delta、usage |
| API-004 | TokenHubProxy adapter | Done | P1 | 复用 Responses adapter |
| API-005 | 请求使用会话固定模型 | Done | P0 | `sendMessage`、`retryLast`、fork 自动回复均使用 `conversation.model` |
| API-006 | DeepSeek reasoning/thinking 配置入口评估 | Done | P2 | 当前保留 provider reasoning 配置，后续按 provider 差异扩展 |
| API-007 | raw response debug log | Done | P1 | 设置中可开启，assistant 消息保存原始 SSE frame |
| API-008 | assistant 元数据显示 | Done | P1 | 气泡底部显示总耗时、首 token、token usage |
| API-009 | Provider 删除与机器人依赖保护 | Done | P1 | 无机器人依赖时删除 Provider 和 Key；有依赖时提示批量改绑或取消 |
| API-010 | 机器人批量改绑后删除 Provider | Done | P1 | 依赖源 Provider 的机器人改绑到目标 Provider 默认模型，再删除源 Provider |

### 4.2 会话固定模型与分叉

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| BRANCH-001 | `ConversationEntity` 增加 fork 来源字段 | Done | P0 | `forkedFromConversationId` / `forkedFromMessageId` 已迁移 |
| BRANCH-002 | 从消息气泡分叉到其他 Provider | Done | P0 | 复制历史消息到目标消息，保存新会话来源 |
| BRANCH-003 | 从 USER 消息分叉后自动用目标模型回复 | Done | P1 | 仓库测试覆盖 |
| BRANCH-004 | 对话列表展示分叉来源标记 | Done | P2 | 抽屉会话行已显示带分叉图标的来源标签，可展示来源对话标题或来源不可用状态 |
| BRANCH-005 | 顶部模型切换降级为显示属性 | Done | P1 | 新建会话时选择模型，已有会话模型固定 |

### 4.3 联网搜索和工具调用

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| SEARCH-001 | 设置中增加搜索模式 | Done | P1 | 关闭 / 自动搜索 / 强制搜索 |
| SEARCH-002 | OpenAI Responses hosted `web_search` | Done | P1 | 请求体发送 `tools: web_search` |
| SEARCH-003 | DeepSeek/OpenAI-compatible 函数式 `web_search` | Done | P1 | 模型返回 tool call 后 app 执行搜索，再回传 tool result |
| SEARCH-004 | app 侧 DuckDuckGo 搜索客户端 | Done | P2 | 解析 HTML 搜索结果并返回标题、URL、摘要 |
| SEARCH-005 | 工具调用独立 UI | Done | P1 | 非普通气泡样式，可折叠 |
| SEARCH-006 | 搜索 URL 展示 | Done | P1 | 解析 `url_citation`、正文 URL、OpenAI `action.url` |
| SEARCH-007 | OpenAI 多个 hosted web_search item 聚合 | Done | P1 | search/open_page 合并为一个工具卡片 |
| SEARCH-008 | 工具调用历史参与上下文策略 | Done | P1 | TOOL 消息落库但不回传给模型上下文 |
| SEARCH-009 | DeepSeek/OpenAI-compatible 网页抓取工具兼容 | Done | P1 | 兼容 DSML `open` / `open_page` / `web_fetch`，工具结果回传后继续生成 |
| SEARCH-010 | DeepSeek thinking + 工具调用 reasoning 回传 | Done | P1 | 非流式和流式 DSML 工具链均回传 `reasoning_content`，避免 thinking mode 400 |
| SEARCH-011 | 群聊 GPT 首轮 web_search 兼容 | Done | P1 | 群聊首轮无历史时合成 user task，避免 Responses web_search system-only 请求失败 |

### 4.4 多模态附件

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| ATTACH-001 | 消息模型支持附件元数据 | Done | P1 | `ChatAttachment` + `messages.attachmentsJson` |
| ATTACH-002 | Room v7/v8 附件和 provider 附件开关迁移 | Done | P1 | schema 7/8 已导出 |
| ATTACH-003 | 输入框附件入口 | Done | P1 | 支持选择图片、选择文件、拍摄照片 |
| ATTACH-004 | 附件复制到 app 私有目录 | Done | P1 | 保存到 `files/chat_attachments` |
| ATTACH-005 | OpenAI Responses 图片/文件提交 | Done | P1 | 图片走 `input_image`，文件走 `input_file` data URL |
| ATTACH-006 | 用户气泡展示附件 | Done | P1 | 气泡内显示附件列表 |
| ATTACH-007 | 图片缩略图和 app 内预览 | Done | P1 | 图片附件显示缩略图，点击弹窗放大 |
| ATTACH-008 | provider 级附件开关 | Done | P1 | DeepSeek 默认关闭，OpenAI/TokenHub 默认开启 |
| ATTACH-009 | Android 系统分享导入 | Done | P1 | App 可作为系统分享目标，支持文本、单文件、多文件导入为待发送草稿 |
| ATTACH-010 | 分享目标选择 | Done | P1 | 分享进入后可选已有单聊、已有群聊或快速新建单聊，不自动发送 |
| ATTACH-011 | 附件大小限制和错误提示 | Done | P1 | 选择附件和系统分享导入均限制单个附件 20MB、待发送总量 50MB，超限文件跳过并提示 |
| ATTACH-012 | PDF/文本文件 app 内预览 | Done | P2 | PDF 附件已支持 app 内第一页预览，文本/JSON/Markdown/日志等文本附件已支持 app 内滚动/选择预览，并保留系统打开入口 |

### 4.5 设置、主题和配置导入导出

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| SETTINGS-001 | 设置从弹窗升级为独立页面 | Done | P1 | 主题、字体、debug、搜索和 provider 管理集中入口 |
| SETTINGS-002 | 主题色配置 | Done | P1 | 多套调色板影响 app、聊天背景、控件、气泡 |
| SETTINGS-003 | 夜间模式 | Done | P1 | 修复顶部文字、按钮、输入框图标暗色可见性 |
| SETTINGS-004 | 字体大小调整 | Done | P1 | 设置中支持字体缩放 |
| SETTINGS-005 | Provider 配置文本导入导出 | Done | P1 | JSON 文本包含 provider 字段和 key |
| SETTINGS-006 | Provider 删除和机器人改绑 | Done | P1 | 配置管理中可删除无依赖 Provider；有依赖时选择目标配置批量改绑后删除 |
| SETTINGS-007 | 聊天背景预设管理 | Done | P1 | 设置页可新增、编辑、删除、上移、下移背景预设，供群聊主题快速插入 |
| SETTINGS-008 | Provider 配置二维码导入导出 | Done | P2 | 设置页已支持生成压缩配置二维码、扫码导入，并继续兼容 JSON 文本导入导出 |

### 4.6 聊天 UI 和交互

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| UI-001 | 输入框光标跳动修复 | Done | P0 | 使用 `TextFieldValue` 保持 selection |
| UI-002 | AI 流式输出自动追踪底部 | Done | P1 | 用户手动上滑后暂停追踪，回到底部后恢复 |
| UI-003 | 输出中状态和停止按钮 | Done | P1 | 发送按钮在生成中变为停止按钮 |
| UI-004 | 顶部工具条压缩 | Done | P1 | 分享/多选等操作合并到菜单 |
| UI-005 | 抽屉外区域点击关闭 | Done | P1 | 点击非抽屉区域可收起 |
| UI-006 | 选择模式点击气泡即选中 | Done | P1 | 不再必须点 checkbox |
| UI-007 | “选择到这里”快捷按钮 | Done | P1 | 滚动到其他气泡处可范围选择 |
| UI-008 | 选中气泡高亮 | Done | P1 | 边框和容器色增强 |
| UI-009 | Markdown 表格渲染 | Done | P1 | 表格边框、行高、分隔线处理已优化 |
| UI-010 | `---` Markdown 分隔线 | Done | P1 | 渲染为横线 |
| UI-011 | 滚动条平滑性 | Watch | P1 | 已优化，长气泡边界卡顿仍需真机观察 |
| UI-012 | 中文乱码修复 | In Progress | P1 | 主界面大部分已修复，tracker 本次已重写；仍需持续扫描 |
| UI-013 | 长 AI 气泡顶部/底部快速跳转 | Done | P1 | 单聊 assistant 和群聊展开 BOT 长气泡在滚动时显示侧边上下跳转按钮，点击可滚到当前气泡顶部/底部 |
| UI-014 | 长 AI 气泡浮动操作菜单 | Done | P1 | 当前长 AI 气泡顶部离屏时显示三点菜单，支持复制、分享文本、分享长图、收藏；菜单展开期间不被隐藏计时收起 |

### 4.7 后台生成和通知

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| BG-001 | 多会话生成任务不中断 | Done | P1 | 切换会话不取消其他会话生成 |
| BG-002 | 前台服务保活生成 | Done | P1 | 生成中启动 foreground service |
| BG-003 | 后台完成通知 | Done | P1 | app 在后台且生成完成时发通知 |
| BG-004 | Android 13+ 通知权限请求 | Done | P1 | MainActivity 请求 `POST_NOTIFICATIONS` |

### 4.8 成果收藏

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| FAV-001 | 收藏片段 Room v9 数据模型 | Done | P1 | 新增 `favorite_snippets`，保存快照、来源、标签、描述和搜索文本 |
| FAV-002 | 单气泡收藏入口 | Done | P1 | 消息气泡操作区可收藏当前消息 |
| FAV-003 | 多选收藏入口 | Done | P1 | 顶部三点菜单可收藏选中消息，沿用范围选择 |
| FAV-004 | 收藏编辑弹窗 | Done | P1 | 保存时可填写标题、标签和描述 |
| FAV-005 | 收藏夹页面 | Done | P1 | 抽屉顶部进入，支持关键词搜索和标签筛选 |
| FAV-006 | 收藏详情查看 | Done | P1 | 使用现有 Markdown、附件和元数据展示消息快照 |
| FAV-007 | 收藏分享与跳回来源 | Done | P1 | 支持文本、长图、复制、编辑、删除、跳回来源对话 |
| FAV-008 | 收藏仓库测试 | Done | P1 | 覆盖快照保存、标签规范化、源消息变更后快照不变、拒绝流式消息 |
| FAV-009 | 追加消息到已有收藏 | Done | P1 | 多选当前对话消息后可追加到同来源收藏，自动去重并按时间排序 |
| FAV-010 | 从收藏移除消息 | Done | P1 | 收藏详情可移除单条快照消息，并保留至少一条消息 |

### 4.9 多 AI 群聊

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| GROUP-001 | 群聊 Room v10 数据模型 | Done | P1 | 新增 `ai_bots`、`group_chat_rooms`、`group_chat_members`、`group_messages` 并导出 schema 10 |
| GROUP-002 | 常驻 AI 机器人管理 | Done | P1 | 设置页进入机器人管理，可从 Provider 创建/编辑/启停/删除机器人 |
| GROUP-003 | 群聊创建与成员选择 | Done | P1 | 抽屉和群聊页可新建群聊，选择启用机器人加入 |
| GROUP-004 | 用户群消息落库 | Done | P1 | 用户在群聊发言只保存消息，不自动触发 AI |
| GROUP-005 | 手动点名机器人发言 | Done | P1 | 按群成员选择机器人回复，单群同一时间只允许一个机器人流式任务 |
| GROUP-006 | 群聊上下文构造 | Done | P1 | 注入群聊系统提示、机器人角色提示、主题、成员、摘要和最近 20 条消息 |
| GROUP-007 | 群摘要生成 | Done | P1 | 可选择机器人总结当前讨论，完成后写入群 `summary` |
| GROUP-008 | 群聊工具调用显示 | Done | P1 | TOOL 群消息落库并复用可折叠工具卡片 UI |
| GROUP-009 | 群聊附件上下文 | Done | P1 | 支持附件的机器人收到附件；不支持附件的机器人收到附件元信息文本 |
| GROUP-010 | 群聊消息收藏 | Done | P1 | 群消息可保存为收藏片段快照，来源记录为群聊标题和模型信息 |
| GROUP-011 | 复制群聊配置 | Done | P1 | 可从当前群聊复制标题、主题和成员机器人，允许保存前修改，不复制历史消息 |
| GROUP-012 | 群聊 UI 验证 | Watch | P1 | 已实现群聊页面、点名/总结/播放器入口和机器人气泡，仍需真机验证滚动与后台任务体验 |
| GROUP-013 | 群聊播放器式开始/暂停 | Done | P1 | 点击开始后按成员顺序循环发言；暂停后当前回复说完即停；播放中用户仍可插话 |
| GROUP-014 | 群聊 GPT 搜索兼容 | Done | P1 | 群聊历史以 user context 回传，首轮无历史时合成 user task，首轮 GPT web_search 可正常触发 |
| GROUP-015 | 机器人气泡颜色 | Done | P1 | Room v11 增加机器人固定气泡色，支持自动高对比色和机器人编辑页选择颜色 |
| GROUP-016 | 群聊 BOT 历史气泡折叠 | Done | P1 | 历史 BOT 气泡默认折叠并保留元数据；最新/输出中气泡展开，用户手动展开在当前页面会话保留 |
| GROUP-017 | 群聊发言轮次标记 | Done | P1 | BOT 气泡显示自动第几轮第几个发言、点名第几次发言或总结发言标记 |
| GROUP-018 | 群聊工具调用聚合和顺序 | Done | P1 | 同一轮工具调用聚合为一个可折叠工具气泡，并排在对应正文气泡之前 |
| GROUP-019 | 群聊多选、收藏和分享 | Done | P1 | 群聊支持多选、范围选择、收藏选中消息、追加收藏、文本分享和长图分享入口 |
| GROUP-020 | 群聊编辑和删除 | Done | P1 | 群聊三点菜单可编辑标题、主题、成员机器人，删除使用软删除且不影响历史消息表 |
| GROUP-021 | 群聊背景预设插入 | Done | P1 | 新建/编辑群聊时可选择设置页维护的背景预设并插入到主题文本框 |

## 5. 已知风险

| ID | 风险 | 状态 | 优先级 | 处理建议 |
| --- | --- | --- | --- | --- |
| RISK-001 | 附件使用 Base64 data URL，图片/文件过大时会导致请求体和 token 成本过高 | Watch | P1 | 增加大小限制、图片压缩、Files API 上传 |
| RISK-002 | DeepSeek 当前不支持图片/文件附件 | Done | P1 | 默认关闭附件按钮，保留 provider 开关 |
| RISK-003 | OpenAI hosted web_search 不一定暴露所有内部访问 URL | Watch | P1 | 保留 raw log 和多策略解析 |
| RISK-004 | 长图导出超长对话可能触达 Bitmap 内存限制 | Watch | P1 | 后续做分页/PDF 导出 |
| RISK-005 | `AIChatAppRoot.kt` 文件过大 | Watch | P2 | 后续按 screen/component/dialog/markdown 拆分 |
| RISK-006 | 中文乱码可能仍残留在旧代码或历史 tracker 文本里 | Watch | P1 | 每次提交前执行 mojibake 扫描 |
| RISK-007 | Room migration 版本增长较快 | Done | P2 | 已新增 Room migration 自动化测试，覆盖 schema 1 到最新版本及每个导出 schema 到最新版本 |
| RISK-008 | release keystore 丢失会导致同包名无法升级 | Watch | P0 | 需要安全备份 JKS 和密码文件 |
| RISK-009 | 收藏附件复用本机文件路径，文件被清理后只能保留元数据 | Watch | P2 | 后续增加附件复制/校验或收藏导出打包 |
| RISK-010 | 群聊提示词和最近 20 条上下文可能不足以处理长讨论 | Watch | P1 | 后续增加自动摘要滚动更新、上下文长度设置和主持人调度 |
| RISK-011 | 群聊消息第一版尚未完整接入长图导出 | Watch | P2 | 后续复用导出模型，补齐群聊完整导出和群消息长图分享 |
| RISK-012 | OpenAI Responses hosted web_search 依赖上游代理稳定性 | Watch | P1 | 已优化群聊首轮请求形态和错误提示；仍需观察 502/upstream_error 是否偶发 |
| RISK-013 | 长 AI 气泡侧边浮动按钮依赖 LazyList 可见项估算 | Watch | P2 | 已修复底部跳转 offset 和菜单展开隐藏问题，仍需真机验证不同屏幕尺寸与输入法状态下的位置表现 |

## 6. 下一步计划

### 6.1 P0 / P1

| ID | 任务 | 状态 | 验收标准 |
| --- | --- | --- | --- |
| NEXT-P1-001 | 图片发送前压缩 | Done | 大图导入后保留原图本地预览，并生成最大边 2048px、优先低体积的发送副本；Provider 请求优先使用发送副本 |
| NEXT-P1-002 | 附件限制设置项 | Done | 设置页已支持全局调整单个附件上传上限、待发送附件总量上限和图片原图导入上限，默认保持 20MB/50MB/80MB |
| NEXT-P1-003 | OpenAI Files API 上传路径 | Planned | 大 PDF/文件不走 Base64，改用 `file_id` |
| NEXT-P1-004 | 搜索工具卡片细节优化 | Done | 工具卡片展开后优先显示查询词、打开 URL 和 citation URL，再保留原始输入/输出用于排查 |
| NEXT-P1-005 | 真实 GPT Key 验证图片 + PDF 输入 | Planned | 真机选择图片/PDF 后 GPT 能正确识别内容 |
| NEXT-P1-006 | 真实 DeepSeek Key 回归验证附件按钮隐藏 | Planned | DeepSeek 对话无附件按钮，纯文本/搜索功能正常 |
| NEXT-P1-007 | 滚动卡顿专项优化 | Planned | 排查长气泡交界处滚动卡顿和自绘滚动条影响 |
| NEXT-P1-008 | provider 错误提示进一步中文化 | Done | 400/401/403/404/429、quota/billing、DNS、SSL、超时、Base URL 和 5xx 上游异常均给出更清晰的中文处理建议 |
| NEXT-P1-009 | 收藏夹标签管理 | Done | 收藏夹标签管理弹窗已支持重命名、合并到已有标签和删除标签，收藏内容不受影响 |
| NEXT-P1-010 | 收藏导入导出 | Done | 收藏夹支持全量 JSON 导出/导入恢复，并支持 Markdown 导出归档 |
| NEXT-P1-011 | 群聊真机回归 | Planned | 真机验证新建机器人、创建群聊、点名/播放器发言、搜索工具调用、附件上下文和切换页面不中断 |
| NEXT-P1-012 | 群聊导出增强 | In Progress | 群聊全文/选中消息已支持文本、Markdown 文件和长图分享；仍需继续优化完整群聊导出布局与超长内容表现 |
| NEXT-P1-013 | 长气泡侧边按钮真机体验回归 | Planned | 验证单聊/群聊长气泡上下跳转、浮动三点菜单、菜单展开不自动消失、与滚动条和回到底部按钮不重叠 |
| NEXT-P1-014 | 系统分享真机回归 | Planned | 验证相册、文件管理器、浏览器分享到 App；覆盖单文件、多文件、纯文本、文件加文本说明和 App 前台分享 |
| NEXT-P1-015 | 分享目标页搜索 | Done | 分享目标较多时可搜索单聊、群聊和 Provider；搜索覆盖新建单聊 Provider、已有单聊和已有群聊 |
| NEXT-P1-016 | 背景预设真机回归 | Planned | 验证设置页增改删排序、新建/编辑群聊插入预设、删除预设后已保存群聊主题不变 |

### 6.2 P2 / P3

| ID | 任务 | 状态 | 验收标准 |
| --- | --- | --- | --- |
| NEXT-P2-001 | PDF/文本 app 内预览 | Done | PDF 附件可在 app 内预览第一页，文本/JSON/Markdown/日志等文本附件可在 app 内滚动/选择预览，并保留系统应用打开入口 |
| NEXT-P2-002 | Provider 多模态能力矩阵 | Done | 设置页的 API 配置管理已展示每个 provider 是否支持图片输入、文件输入、搜索工具和 reasoning，并在每行显示紧凑能力徽章 |
| NEXT-P2-003 | 二维码导入导出 Provider 配置 | Done | 设置页可生成包含 Provider 配置的压缩二维码，扫码后复用导入流程新增配置，并有单元测试覆盖二维码载荷导入 |
| NEXT-P2-004 | UI 文件拆分 | Planned | `AIChatAppRoot.kt` 拆分为 chat/settings/drawer/markdown/components |
| NEXT-P2-005 | Room migration 测试 | Done | Robolectric 单测已验证带旧数据的 schema 1 可迁移到最新 schema，且每个已导出 schema 均可迁移到最新 schema；AndroidTest 版本也可编译 |
| NEXT-P2-006 | 导出内容包含附件索引 | Done | Markdown/文本/长图导出会列出用户上传附件名称、MIME 类型和大小，单聊与群聊导出均有单测覆盖 |
| NEXT-P2-007 | 文本级选区收藏 | Planned | 支持收藏气泡内选中的一段文字 |
| NEXT-P2-008 | 收藏搜索结果高亮 | Done | 收藏夹搜索命中标题、描述、标签、来源/模型元数据和正文时高亮显示，并显示正文命中片段 |
| NEXT-P2-009 | 收藏批量管理 | Done | 收藏夹已支持批量模式、当前筛选全选、批量删除、批量追加标签、按更新时间/收藏时间/标题/标签排序，以及在收藏详情中批量移除消息且至少保留一条消息 |
| NEXT-P2-010 | 群聊自动主持人 | Planned | 支持由主持人机器人控制下一位发言者、暂停和总结 |
| NEXT-P2-011 | 群聊播放器增强 | Done | 群聊播放器可按群聊保存自动轮数、发言间隔和失败后重试一次的偏好，自动续播会按偏好暂停、延迟或重试 |
| NEXT-P2-012 | 机器人头像和颜色 | Done | 群聊机器人气泡、点名选择器和机器人管理列表均显示稳定头像缩写、短身份码和解析后的颜色标签，便于快速扫描区分机器人 |
| NEXT-P2-013 | 群摘要自动滚动更新 | Done | 群聊会在无摘要且消息较多、或上次摘要后积累较多新消息时提示生成/更新摘要，并复用总结机器人选择器执行半自动刷新 |
| NEXT-P2-014 | 背景预设导入导出和分类搜索 | Done | 背景预设已支持设置页和群聊插入弹窗的关键词搜索、JSON 导出/分享与追加导入、分类编辑与分类筛选，并支持编辑群聊时按群聊保存和插入常用预设组合 |
| NEXT-P3-001 | Anthropic adapter | Planned | Claude Messages API 文本流式对话 |
| NEXT-P3-002 | Gemini adapter | Planned | Gemini GenerateContent 文本和多模态对话 |
| NEXT-P3-003 | 对话 PDF 导出 | Planned | 支持完整对话导出 PDF |
| NEXT-P3-004 | Prompt 模板和系统提示词 | Planned | 可保存常用 prompt 并按会话应用 |

## 7. 验证清单

本机 Android 环境：

```powershell
$env:JAVA_HOME='D:\Projects\Personal\AndroidApps\.devtools\android\jdk\jdk-17.0.18+8'
$env:ANDROID_HOME='D:\Projects\Personal\AndroidApps\.devtools\android\sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"
```

每次核心功能变更后建议执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon
.\gradlew.bat :app:assembleDebug --console=plain --no-daemon
```

本次已执行：

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat :app:assembleDebug --console=plain --no-daemon
```

提交前建议执行中文乱码扫描：

```powershell
$pattern = @(
    '\u9352', '\u93C2', '\u7EEE', '\u9441', '\u7035', '\u95B0',
    '\u6D93', '\u6D63', '\u6960', '\u7481', '\u6FB6', '\u5BB8',
    '\u9422', '\u9597', '\u93C5', '\u93C6', '\u59AF',
    '\u6D34', '\u93C9', '\u6AD9', '\u6FE1', '\u704F', '\u8930',
    '\u6DC7', '\u757E', '\u7BA0', '\u6D7C', '\u934F', '\u93B4',
    '\u680D', '\u8DFA', '\u5997', '\u4E4F', '\u6BDA', '\u935A',
    '\u699B', '\u9483', '\u7C83', '\u943D', '\u6A3A', '\u7F02',
    '\u5F42', '\u7F03', '\u56E5', '\u57CC', '\u6434'
) -join '|'
rg -n $pattern app/src/main/java app/src/test/java PROJECT_TRACKER.md README.md
```

## 8. Tracker 维护规则

- 每完成一个功能，更新对应条目的状态、进度和验收说明。
- 发现明显 bug 时，先加入风险或近期计划，再决定是否提升到 P0/P1。
- 不在 tracker 写入真实 API Key、keystore 密码、Jenkins 凭据或其他敏感信息。
- 已实现但尚未真机验证的功能优先标为 `Watch`。
- 每次 release 前更新总体进度、风险和验证清单。
