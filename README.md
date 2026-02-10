# il2cpp_libtool_lsposed 项目说明

这是一个 Xposed 模块项目，旨在将 `libTool.so` 动态库注入到目标 Android 应用程序中。

## 功能

- 本项目只支持arm64架构，其他架构请自行修改替换libTool.so
- libTool.so 取自1.1.0版本
- 通过 Xposed Hook 技术，在应用启动时加载指定的动态库。
- 模块会自动从自身的 `assets` 目录中提取 `libTool.so`。
- 将 `.so` 文件复制到目标应用的私有目录并加载，以执行自定义原生代码。

## 工作原理

本模块通过 Hook `android.content.ContextWrapper.attachBaseContext` 方法实现注入。当一个应用启动并调用此方法时，模块会执行以下操作：

1.  获取模块自身的 `Context`。
2.  通过 `moduleContext.assets` 访问并读取模块内置的 `libTool.so` 文件。
3.  将 `.so` 文件写入到被 Hook 的目标应用的 `files` 目录下。
4.  为写入的 `.so` 文件设置可读和可执行权限。
5.  调用 `System.load()` 方法加载该动态库，使其在目标应用的进程空间内运行。

## 如何使用

1.  **编译项目**：使用 Android Studio 编译生成 APK 文件。
2.  **放置 SO 文件**：将你需要注入的 `libTool.so` 文件放置在 `app/src/main/assets/` 目录下。
3.  **安装模块**：在已安装 Xposed 框架的 Android 设备上安装此 APK。
4.  **激活模块**：在 Xposed Installer（或 LSPosed、EdXposed 等管理器）中激活本模块，并选择目标作用域（需要注入的应用）。
5.  **重启设备**：重启 Android 设备使模块生效。

之后，当你打开被作用域选中的应用时，模块会自动将 `libTool.so` 注入，并会有一个 "注入成功: [应用包名]" 的 Toast 提示。

## 注意事项

- 本项目默认排除了系统 (`android`) 和模块自身 (`com.wzh.libtool`)，不会对它们进行注入。
- 请确保你的 `libTool.so` 适配目标应用的 CPU 架构。
- Xposed 日志中会打印详细的加载成功或失败信息，方便调试。

## 免责声明

**本项目仅供学习和技术研究使用，严禁用于任何商业用途或在线网络游戏。**

使用者因使用本工具而产生的一切法律后果，由使用者自行承担。作者对因使用本工具而导致的任何直接或间接损失不承担任何责任。

下载和使用本项目的任何代码，即代表您同意并遵守以上条款。

## 作者

- water
- qq群：1037044062

- libtools.so 作者：mIsmanXP 原帖地址： https://platinmods.com/threads/imgui-il2cpp-tool.211155/
