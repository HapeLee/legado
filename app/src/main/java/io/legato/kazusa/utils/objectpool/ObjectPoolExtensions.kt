package io.legato.kazusa.utils.objectpool

fun <T> ObjectPool<T>.synchronized(): ObjectPool<T> = ObjectPoolLocked(this)
