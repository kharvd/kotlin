fun box() {
    val a = (1..100)

    if (a.filter { it % 2 == 0 }.fold(0) {x, y -> x + y} != 2550) {
        return
    }

    val mArray = Array<String>(3) { if (it == 0) "" else it.toString() }
}

// 0 LOAD 7
// 0 LOAD 8
// 0 LOAD 9
