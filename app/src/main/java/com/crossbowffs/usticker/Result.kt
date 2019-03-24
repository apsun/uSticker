package com.crossbowffs.usticker

/**
 * Discriminated union containing either a value or an exception.
 */
sealed class Result<T> {
    class Ok<T>(val value: T) : Result<T>()
    class Err<T>(val err: Exception) : Result<T>()
}
