package me.centralhardware.znatoki.telegram.statistic.extensions

import java.util.regex.Pattern
import javax.swing.text.MaskFormatter

fun String?.hashtag(): String? = this?.let { "#" + replace(" ", "_") }
fun String?.makeBold() = this?.let { "*$this*" }

private var PHONE_MASK_FORMATTER: MaskFormatter = run {
    MaskFormatter("#-###-###-##-##").apply { valueContainsLiteralCharacters = false }
}

fun String?.formatTelephone(): String? {
    if (this.isNullOrEmpty()) return ""
    return runCatching { PHONE_MASK_FORMATTER.valueToString(this) }.getOrElse { "" }
}

private val VALID_PHONE_NR: Pattern = Pattern.compile("^[78]\\d{10}$")

fun String?.validateTelephone() = this != null && VALID_PHONE_NR.matcher(this).matches()
