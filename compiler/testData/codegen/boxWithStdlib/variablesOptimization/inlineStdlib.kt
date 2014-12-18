import kotlin.test.assertEquals

fun box(): String {
    val a = (1..100)

    if (a.filter { it % 2 == 0 }.fold(0) {x, y -> x + y} != 2550) {
        return "fail"
    }

    val mArray = Array<String>(3) { if (it == 0) "" else it.toString() }

    assertEquals("", mArray[0])
    assertEquals("1", mArray[1])
    assertEquals("2", mArray[2])

    return "OK"
}
