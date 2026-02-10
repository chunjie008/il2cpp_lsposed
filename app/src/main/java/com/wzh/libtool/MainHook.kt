package com.wzh.libtool

import android.content.Context
import android.content.res.AssetManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        private const val SO_NAME = "libTool.so"
        private const val MODULE_PKG = "com.wzh.libtool"
        private var modulePath: String? = null
        // 用于记录已注入的进程，防止重复注入
        private val injectedProcesses = ConcurrentHashMap.newKeySet<String>()

        /**
         * 使用 AssetManager 从模块 APK 中加载 SO
         * 这是比 createPackageContext 更稳妥的方式
         */
        private fun loadToolFromAssets(context: Context) {
            if (modulePath == null) {
                XposedBridge.log("libtool: Error: modulePath is null. The module probably wasn't initialized correctly.")
                return
            }

            try {
                val outFile = File(context.filesDir, SO_NAME)

                // 1. 创建 AssetManager 并添加模块 APK 路径
                // (AssetManager.addAssetPath 是隐藏 API, 需要反射调用)
                val assetManager = AssetManager::class.java.newInstance()
                val addAssetPathMethod = assetManager.javaClass.getMethod("addAssetPath", String::class.java)
                addAssetPathMethod.invoke(assetManager, modulePath)

                // 2. 从模块的 Assets 中提取 SO
                assetManager.open(SO_NAME).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 3. 赋予执行权限并加载
                outFile.setReadable(true, false)
                outFile.setExecutable(true, false)
                System.load(outFile.absolutePath)
                XposedBridge.log("libtool: Successfully loaded: ${outFile.absolutePath}")

            } catch (t: Throwable) {
                XposedBridge.log("libtool: Error loading SO from assets:")
                XposedBridge.log(t) // Log the full stack trace for debugging
            }
        }
    }

    /**
     * 在 Zygote 进程初始化时被调用，此时获取模块路径最安全
     */
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android" || lpparam.packageName == MODULE_PKG) return

        // Hook attachBaseContext, 这是一个在 Application 创建早期调用的方法
        XposedHelpers.findAndHookMethod(
            "android.content.ContextWrapper",
            lpparam.classLoader,
            "attachBaseContext",
            Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // 检查当前进程是否已注入，如果已注入则直接返回
                    if (!injectedProcesses.add(lpparam.processName)) {
                        return
                    }
                    
                    XposedBridge.log("libtool: Injecting into ${lpparam.processName}")

                    val appContext = param.args[0] as Context

                    try {
                        // 执行 SO 加载逻辑
                        loadToolFromAssets(appContext)

                        // 弹窗提示
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(appContext, "注入成功: ${lpparam.packageName}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        XposedBridge.log("libtool: Init failed in afterHookedMethod:")
                        XposedBridge.log(e)
                        // 注入失败时，将进程名从集合中移除，以便下次有机会重试
                        injectedProcesses.remove(lpparam.processName)
                    }
                }
            }
        )
    }
}
