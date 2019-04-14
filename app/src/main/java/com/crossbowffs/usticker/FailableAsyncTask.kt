package com.crossbowffs.usticker

import android.os.AsyncTask

/**
 * Represents an AsyncTask with a background step that may throw an exception.
 * The result of the background step is wrapped in a Result and passed to the
 * given callback on the main thread.
 */
abstract class FailableAsyncTask<TParam, TResult> : AsyncTask<TParam, Unit, Result<TResult>>() {
    private var callback: ((Result<TResult>) -> Unit)? = null

    /**
     * Override this, not doInBackground, to perform the background step.
     */
    abstract fun run(arg: TParam): TResult

    /**
     * Executes the AsyncTask with a single parameter and a callback to
     * be called on the main thread when the AsyncTask completes.
     */
    fun executeWithCallback(arg: TParam, callback: (Result<TResult>) -> Unit) {
        this.callback = callback
        execute(arg)
    }

    final override fun doInBackground(vararg args: TParam): Result<TResult> {
        return try {
            Result.Ok(run(args[0]))
        } catch (e: Exception) {
            Result.Err(e)
        }
    }

    final override fun onPostExecute(result: Result<TResult>) {
        val callback = this.callback
        if (callback != null) {
            callback(result)
        }
    }
}
