annotation class Hello
val v = 1

<caret>
fun some() {}

// INVOCATION_COUNT: 1
// EXIST: Hello
// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
