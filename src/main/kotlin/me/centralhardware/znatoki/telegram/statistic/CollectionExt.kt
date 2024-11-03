package me.centralhardware.znatoki.telegram.statistic

fun <T> Collection<T>.containsAny(vararg elements: T): Boolean {
    val set = elements.toSet()
    return any(set::contains)
}
