inline fun boxInline(): String {
    val x = "abcde".length
    val y = x
    val z = y

    val z1: Int

    if (1 == 1) {
        z1 = y
    } else {
        z1 = x
    }

    if (z != 5) {
        return "fail z1: $z"
    }

    if (z1 != 5) {
        return "fail z1: $z1"
    }

    return "OK"
}

fun box(): String {
    return boxInline()
}

// 0 LOAD 1
// 0 LOAD 2
// 4 LOAD 3
// 8 STORE
