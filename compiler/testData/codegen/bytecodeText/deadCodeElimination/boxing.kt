class A

fun box() {
    val x: Int? = 1
    x!!
    
    val y: Any? = if (1 == 1) x else A()
    y!!
    
    val z: Int? = if (1 == 1) x else null
    z!!
}

// 0 IFNULL
// 1 IFNONNULL
// 1 throwNpe
// 0 ATHROW
