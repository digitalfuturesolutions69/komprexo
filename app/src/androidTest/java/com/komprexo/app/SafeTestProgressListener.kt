package com.komprexo.app

import android.os.Build
import android.util.Log
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

/** Test APK only: public test identifiers and exception TYPE, never message/data/token. */
class SafeTestProgressListener : RunListener() {
    private fun clean(text:String?)=(text ?: "unknown").replace(Regex("[^A-Za-z0-9_.]"),"_")
    private fun report(event:String,description:Description,category:String="none") {
        Log.i("KomprexoTest","event=$event api="+Build.VERSION.SDK_INT+
            " test="+clean(description.className)+"."+clean(description.methodName)+" category="+clean(category))
    }
    override fun testStarted(description:Description)=report("START",description)
    override fun testFinished(description:Description)=report("FINISH",description)
    override fun testFailure(failure:Failure)=report("FAIL",failure.description,failure.exception.javaClass.simpleName)
}
