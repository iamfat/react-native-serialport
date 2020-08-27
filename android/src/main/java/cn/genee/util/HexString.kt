package cn.genee.util

internal val HEX_DIGITS =
    charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

fun Int.toHexString(): String {
    var n = this
    var charArr = charArrayOf()
    do {
        val b = n.and(0xf)
        charArr += HEX_DIGITS[b]
        n = n.ushr(4)
    } while (n > 0)
    return String(charArr)
}

fun Short.toHexString(): String {
    return this.toInt().and(0xffff).toHexString()
}

fun Byte.toHexString(): String {
    return this.toInt().and(0xff).toHexString()
}

fun ByteArray.toHexString(): String {
    val charArr = CharArray(this.size * 2)
    var j = 0
    for (i in this.indices) {
        val b = this[i].toInt().and(0xff)
        charArr[j++] = HEX_DIGITS[b.ushr(4).and(0x0f)]
        charArr[j++] = HEX_DIGITS[b.and(0x0f)]
    }
    return String(charArr)
}
