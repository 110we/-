package com.kalidroid

import android.app.Application
import com.kalidroid.kali.KaliContainer
import com.kalidroid.kali.MountManager
import com.kalidroid.kali.RootfsManager
import com.kalidroid.permission.PermissionManager
import com.kalidroid.workflow.WorkflowEngine
import com.kalidroid.workflow.WorkflowParser

class KaliDroidApp : Application() {
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var kaliContainer: KaliContainer
        private set
    lateinit var workflowEngine: WorkflowEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        permissionManager = PermissionManager(this)
        kaliContainer = KaliContainer(this, RootfsManager(this), MountManager())
        workflowEngine = WorkflowEngine(permissionManager.getExecutor(), WorkflowParser())
        installBundledProot()
    }

    /**
     * 若 assets 内已内置 proot（v2.1 打包时放入 assets/proot），
     * 启动时自动释放到 filesDir/bin/proot 并加可执行权限。
     */
    private fun installBundledProot() {
        runCatching {
            if (kaliContainer.prootInstalled()) return
            val stream = assets.open("proot")
            kaliContainer.installProot(stream)
        }
    }

    companion object {
        lateinit var instance: KaliDroidApp
            private set
    }
}
