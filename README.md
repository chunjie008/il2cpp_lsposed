# libtool

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Root](https://img.shields.io/badge/Root-Required-red.svg)](https://www.xda-developers.com/root/)
[![Framework](https://img.shields.io/badge/Framework-LSPosed-blue.svg)](https://github.com/LSPosed/LSPosed)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

一个通用的 Xposed 注入模块，旨在为任何目标应用提供一个稳定、可靠的 `.so` 注入框架。

## 简介 (Introduction)

**libtool** 是一个为开发者和逆向工程师设计的 Xposed 模块。它提供了一个简单而强大的框架，用于将自定义的原生库（`.so` 文件）注入到任何目标 Android 应用程序中，从而实现运行时修改、功能增强或分析。本项目解决了 Xposed 模块开发中常见的重复注入和跨包访问资源等问题，力求稳定可靠。

## 目录 (Table of Contents)

- [功能](#功能)
- [工作原理](#工作原理)
- [先决条件](#先决条件-prerequisites)
- [如何构建](#如何构建-how-to-build)
- [如何安装和使用](#如何安装和使用)
- [项目结构](#项目结构)
- [更新日志](#更新日志-changelog)
- [未来计划](#未来计划-future-work)
- [常见问题](#常见问题-troubleshooting)
- [贡献](#贡献-contributing)
- [免责声明](#免责声明)
- [许可证](#许可证-license)
- [致谢](#致谢-credits)

## 功能

- 通过 Xposed Hook 技术，在应用启动时加载指定的动态库。
- 采用 `IXposedHookZygoteInit` 在系统启动时获取模块路径，注入方式更稳定，避免了跨包访问资源的权限问题。
- 通过记录进程名，完美解决因 `attachBaseContext` 多次调用而导致的重复注入问题。
- 模块会自动从自身的 `assets` 目录中提取 `libTool.so` 并加载。

## 工作原理

本模块通过 Hook `android.content.ContextWrapper.attachBaseContext` 方法实现注入。当一个应用启动并调用此方法时，模块会执行以下操作：

1.  **初始化 (Zygote)**：在 Android 系统启动时，通过 `IXposedHookZygoteInit` 接口的 `initZygote` 方法，安全地获取并保存模块自身的 APK 路径。
2.  **Hook 注入点**：当目标应用加载时，Hook 其 `attachBaseContext` 方法。
3.  **防止重复注入**：在 Hook 方法内部，使用一个 `Set` 记录当前已注入的进程名 (`processName`)。如果发现当前进程已经被注入过，则直接跳过，确保每个进程只注入一次。
4.  **提取 SO 文件**：使用反射创建一个 `AssetManager` 实例，并将第 1 步中获取的模块 APK 路径添加进去。然后从这个 `AssetManager` 中读取 `libTool.so` 文件。
5.  **写入和加载**：将 `.so` 文件写入到被 Hook 的目标应用的私有目录 (`filesDir`) 下，为其设置可读和可执行权限，最后通过 `System.load()` 加载。
6.  **结果提示**：加载成功后，在主线程弹出一个 Toast 提示用户。
7.  **失败重试**：如果注入失败，会将进程名从记录中移除，以便在下次 `attachBaseContext` 调用时有机会重试。

## 先决条件 (Prerequisites)

-   **Android 设备 (已 Root)**: Xposed 框架需要 Root 权限来修改系统行为。推荐使用 Magisk 获取 Root。
-   **LSPosed 框架**: 本项目依赖 Xposed API。LSPosed 是目前主流的、在 Android 8.0+ 上运行的 Xposed 框架实现。
-   **Android Studio**: 用于编译和构建本项目的 APK 文件。

## 如何构建 (How to Build)

1.  克隆本仓库: `git clone https://github.com/chunjie008/il2cpp_libtool_lsposed.git`
2.  在 Android Studio 中打开项目。
3.  将你需要注入的原生库文件 (例如 `libTool.so`) 放入 `app/src/main/assets/` 目录。
4.  通过 `Build > Build Bundle(s) / APK(s) > Build APK(s)` 构建 APK。
5.  生成的 APK 文件位于 `app/build/outputs/apk/debug/`。

## 如何安装和使用

1.  **满足先决条件**：请确保你的设备满足 [先决条件](#先决条件-prerequisites) 中列出的要求。
2.  **安装模块**：将在上一步中生成的 APK 文件安装到设备上。
3.  **激活模块**：在 LSPosed 管理器中，激活 `libtool` 模块。
4.  **设置作用域**：在 LSPosed 中为 `libtool` 模块选择你希望注入的目标应用。
5.  **重启设备**：**务必重启 Android 设备**，因为模块需要在 Zygote 启动时获取路径，这样才能使注入逻辑生效。

之后，当你打开被作用域选中的应用时，模块会自动将 `libTool.so` 注入，并会有一个 "注入成功: [应用包名]" 的 Toast 提示。

## 项目结构

- `app/src/main/java/com/wzh/libtool/MainHook.kt`: 模块的核心代码，实现了 Xposed Hook 的所有逻辑。
- `app/src/main/assets/libTool.so`: 需要注入的原生动态库。**你需要将你自己的 SO 文件放在这里。**
- `app/src/main/assets/xposed_init`: Xposed 模块入口配置文件，指向 `MainHook` 类。
- `app/src/main/AndroidManifest.xml`: Android 清单文件，包含了模块声明的元数据。

## 更新日志 (Changelog)

### v1.2 (Current)
- **修复**: 增加了进程注入记录，彻底解决 `attachBaseContext` 多次调用导致的重复注入问题。
- **优化**: 改为 `IXposedHookZygoteInit` 实现，在 Zygote 启动时获取模块路径，避免了因权限问题导致的 `NameNotFoundException`，注入更稳定。

### v1.0
- 初始版本，实现了基本的 SO 注入功能。

## 未来计划 (Future Work)

- [ ] 增加一个设置界面 (Activity)，让用户可以动态选择目标应用，而无需在 LSPosed 中手动配置。
- [ ] 支持自定义 SO 文件名，可通过设置界面进行配置。
- [ ] 增加一个选项，用于在不重启目标应用的情况下重新加载 SO 文件。

## 常见问题 (Troubleshooting)

1.  **模块未生效，没有看到 "注入成功" 的提示？**
    *   **检查先决条件**：确认设备已 Root，LSPosed 已正常工作。
    *   **检查模块是否激活**：打开 LSPosed 管理器，确认 `libtool` 模块已勾选启用。
    *   **检查作用域**：在 LSPosed 中点击 `libtool` 模块，进入“作用域”设置，确保你想要 Hook 的目标应用已被勾选。
    *   **确认已重启**：`libtool` 依赖在系统启动时获取关键信息，因此激活模块或首次安装后 **必须重启手机**。
    *   **查看日志**：打开 LSPosed 管理器，查看日志，搜索 `libtool` 或 `Xposed` 关键字，确认是否有错误信息输出。

2.  **应用闪退？**
    *   **CPU 架构不匹配**：请确保你放入 `assets` 目录的 `libTool.so` 文件与目标手机的 CPU 架构一致（如 `arm64-v8a` 或 `armeabi-v7a`）。架构不匹配是闪退的最常见原因。
    *   **SO 库本身的问题**：尝试确认 `libTool.so` 是否能在目标应用和系统版本上正常工作。

3.  **日志中出现 `FileNotFoundException: libTool.so` 错误？**
    *   这是因为模块在 `assets` 目录中找不到 `libTool.so` 文件。请检查 [如何构建](#如何构建-how-to-build) 的第 3 步，确保你已经将 SO 文件正确放置。

## 贡献 (Contributing)

欢迎提交 Pull Request。对于重大更改，请先开一个 Issue 来讨论您想要更改的内容。

## 免责声明

**本项目仅供学习和技术研究使用，严禁用于任何商业用途或在线网络游戏。**

使用者因使用本工具而产生的一切法律后果，由使用者自行承担。作者对因使用本工具而导致的任何直接或间接损失不承担任何责任。

下载和使用本项目的任何代码，即代表您同意并遵守以上条款。

## 许可证 (License)

本项目采用 [MIT](https://choosealicense.com/licenses/mit/) 许可证。

## 致谢 (Credits)

-   **mIsmanXP**: `libtool.so` 的原作者。更多信息请参考 [原帖地址](https://platinmods.com/threads/imgui-il2cpp-tool.211155/)。
-   **water**: 本项目的开发者。
-   以及所有在 QQ 交流群 (1037044062) 中提供过帮助的朋友。
