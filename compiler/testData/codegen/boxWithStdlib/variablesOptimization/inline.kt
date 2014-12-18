import kotlin.test.assertEquals

inline fun calc<T, R>(value : T, fn: (T)->R) : R = fn(value)
inline fun identity<T>(value : T) : T = calc(value) { it }

fun box(): String {
    val x = identity(1)
    assertEquals(1, x)

    return "OK"
}
