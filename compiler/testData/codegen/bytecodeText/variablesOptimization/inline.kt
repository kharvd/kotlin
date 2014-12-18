inline fun calc<T, R>(value : T, fn: (T)->R) : R = fn(value)
inline fun identity<T>(value : T) : T = calc(value) { it }

fun box() {
    val x = identity(1)

    1 == x
}

// 0 ILOAD 2
