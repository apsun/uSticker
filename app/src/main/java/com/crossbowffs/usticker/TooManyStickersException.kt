package com.crossbowffs.usticker

class TooManyStickersException(
    val count: Int,
    val limit: Int,
    val path: String
) : Exception()