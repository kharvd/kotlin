fun box(): String {
    val x = "abcde".length // 0
    val y = x // 1
    val z = y  // 2

    val z1: Int // 3

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

// 0 LOAD 1
// 0 LOAD 2
// 2 LOAD 3
