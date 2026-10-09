package com.komprexo.app

import android.os.Build
import android.util.Log
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Test APK only: public test identifiers and exception TYPE, never message/data/token. */
class SafeTestProgressListener : RunListener() {
    private val generation=AtomicInteger()
    private val watchdog=Executors.newSingleThreadScheduledExecutor { action ->
        Thread(action,"KomprexoTestWatchdog").apply { isDaemon=true }
    }
    private var deadline:ScheduledFuture<*>?=null
    private fun clean(text:String?)=(text ?: "unknown").replace(Regex("[^A-Za-z0-9_.]"),"_")
    private fun report(event:String,description:Description,category:String="none") {
        Log.i("KomprexoTest","event=$event api="+Build.VERSION.SDK_INT+
            " test="+clean(description.className)+"."+clean(description.methodName)+" category="+clean(category))
    }
    override fun testStarted(description:Description) {
        val ticket=generation.incrementAndGet()
        deadline?.cancel(false)
        report("START",description)
        deadline=watchdog.schedule({
            if(generation.get()==ticket) {
                report("STALLED_120S",description)
                // Emit only public class/method identifiers, never stack messages,
                // source paths, locals or arbitrary thread names.
                Thread.getAllStackTraces().forEach { (thread,frames) ->
                    val label=if(thread.name=="main") "main" else if(thread.name.startsWith("Instr:")) "runner" else null
                    if(label!=null) frames.take(24).forEach { frame ->
                        Log.i("KomprexoTest","event=STACK thread=$label state="+thread.state+
                            " frame="+clean(frame.className)+"."+clean(frame.methodName))
                    }
                }
                // A stuck @Before/main-thread call cannot respond to JUnit interruption.
                // Fail this emulator-only run; never accept an incomplete test count.
                if(generation.get()==ticket) android.os.Process.killProcess(android.os.Process.myPid())
            }
        },120,TimeUnit.SECONDS)
    }
    override fun testFinished(description:Description) {
        generation.incrementAndGet()
        deadline?.cancel(false)
        report("FINISH",description)
    }
    override fun testFailure(failure:Failure)=report("FAIL",failure.description,failure.exception.javaClass.simpleName)
}
