[English](../README.md) · [中文](README.zh-CN.md)

<p align="center">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/logo.jpg" width="128" alt="语曰">
</p>

<h1 align="center">语曰 · Yuyue</h1>

<p align="center">按住说一句，落到那一天。<br>
用说话写的私人时间日记 — 本机优先，不是云笔记。</p>

<p align="center">
  <a href="../LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/license-MIT-1f4e5f"></a>
  <img alt="Android" src="https://img.shields.io/badge/Android-API%2029%2B-3D6B4F">
  <img alt="本机优先" src="https://img.shields.io/badge/%E6%95%B0%E6%8D%AE-%E6%9C%AC%E6%9C%BA-9A6B4F">
</p>

**语曰**是一款 Android 应用：用说话写下私人时间日记。AI 只做可选润色；**本机数据库与备份是权威**。

<p align="center">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/home.png" width="24%" alt="首页 · 按住说">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/time.png" width="24%" alt="时间 · 月历">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/recall.png" width="24%" alt="回忆 · 搜索">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/vault.png" width="24%" alt="设置 · 备份">
</p>

<p align="center"><sub>真机截图（QA 包）。</sub></p>

## 主循环

| 循环 | 含义 |
|------|------|
| **记** | 按住说、打字、分享照片 — 原话先落本机 |
| **看** | 月历 → 当日胶囊 → 详情 |
| **忆** | 语音/文字回忆 — 本机 FTS / 向量 |
| **保险柜** | 应用锁、YUY2 加密备份、废纸篓、导出 |

## 快速开始

```bash
git clone https://github.com/XiaoYuTianTian/yuyue.git
cd yuyue
cp config.local.properties.example config.local.properties
```

把 `config.local.properties` 里所有 `***` 换成你自己的值（可不填 API Key，离线仍可记/看/本机忆）。

Android Studio **Open** 仓库根目录 → 运行 **`android-app`**。

```bash
.\gradlew.bat :android-app:installDebug
```

更完整的英文说明与文档表见 [README.md](../README.md)。

## 许可

[MIT](../LICENSE) © 2026 XiaoYuTianTian
