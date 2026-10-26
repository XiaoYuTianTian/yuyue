# 环境探测 · 2026-09-19 13:10 +08

run_id: `20260919-131020`

## 硬门槛结果

| 检查项 | 结果 | 证据 |
|--------|------|------|
| JDK 17 | READY · OpenJDK 17.0.20 LTS (`JAVA_HOME` 与 `gradle.properties` 一致) | 本文件 |
| Gradle Wrapper | PARTIAL · 现有 `gradlew.bat` + `gradle-wrapper.jar` (43504) + properties；仍缺 Unix `gradlew` | `gradle/wrapper/` |
| Android SDK | READY · `C:\Android\Sdk`，adb 37.0.1 | `local.properties` |
| ARM 真机 | READY · Xiaomi MIX 3, Android 10 / API 29, abi `arm64-v8a`，已装 `com.yuyue.app` | `adb devices -l`；后续用 `com.yuyue.app.qa` 隔离 |
| Vosk 资产 | READY · 43,898,754 bytes | `android-app/src/main/assets/vosk-model-small-cn-0.22.zip` |
| BGE ONNX 资产 | READY · 24,010,842 bytes | `android-app/src/main/assets/bge-small-zh-v1.5/model_quantized.onnx` |
| MiniMax Key | READY · `LLM_API_KEY` / `LLM_BASE_URL` / `LLM_MODEL` 均 SET（值不入库） | `config.local.properties` |
| SiliconFlow Key | READY · `SILICONFLOW_API_KEY` SET；`FAST_LLM_*` 未显式配置，走构建默认 | `config.local.properties` |
| Git | UNSUPPORTED · 工作区不是 git 仓库 | `git rev-parse` |
| 遗留 server | 不启动 · Local Agent 主路径不依赖 | 计划约定 |
| x86_64 模拟器跑 native | UNSUPPORTED · `abiFilters` 仅 armeabi-v7a / arm64-v8a；用户选择真机为主 | `android-app/build.gradle.kts` |

## 设备明细

Xiaomi MIX 3 (`45c5974e`) Android 10 / API 29 / `arm64-v8a`，USB 调试已授权。个人包 `com.yuyue.app` 仍在。QA 包 `com.yuyue.app.qa` 因 `INSTALL_FAILED_USER_RESTRICTED` 未装上；APK 已放到 `/sdcard/Download/Yuyue-qa.apk`（181,765,453 bytes）。adb `input` 被小米拒绝（缺 `INJECT_EVENTS` / USB 安装未开）。

## 密钥处理

- 密钥只存在 gitignored `config.local.properties`。
- 本探测只记录键名与是否 SET、长度，不记录值。
- Debug APK 会把默认 Key 编进 `BuildConfig`；QA 产物不得当可分发包。
