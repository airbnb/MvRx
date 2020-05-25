package com.airbnb.mvrx.mocking

import com.airbnb.mvrx.MvRxState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstructorCodeTest : BaseTest() {

    @Test
    fun testImports() {
        val code = com.airbnb.mvrx.mocking.printer.ConstructorCode(
                State(),
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        )

        assertEquals(
            "com.airbnb.mvrx.mocking.ConstructorCodeTest, kotlin.Double, kotlin.Float, kotlin.Int, kotlin.String",
            code.imports.joinToString()
        )
    }

    @Test
    fun testConstructor() {
        val code = com.airbnb.mvrx.mocking.printer.ConstructorCode(
                State(),
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        )

        assertEquals(
            "valmockStatebylazy{ConstructorCodeTest.State(int=1,float=1.0f,str=\"hello\",charSequence=\"'hi'withnested\\\"quotes\\\"and\\tatab\",double=4.5,map=mutableMapOf(3to\"three\",2to\"two\"),strList=listOf(\"hi\",\"there\"),nestedObject=ConstructorCodeTest.NestedObject(myEnum=ConstructorCodeTest.MyEnum.A),singleTon=ConstructorCodeTest.MySingleton,nestedObjectList=listOf(ConstructorCodeTest.NestedObject(myEnum=ConstructorCodeTest.MyEnum.A)))}",
            code.lazyPropertyToCreateObject.removeWhiteSpace()
        )
    }

    @Test
    fun testJsonObject() {
        val code = com.airbnb.mvrx.mocking.printer.ConstructorCode(
                StateWithJsonObject(),
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        )

        assertEquals(
            "valmockStateWithJsonObjectbylazy{ConstructorCodeTest.StateWithJsonObject(json=\"\"\"{\"color\":\"red\",\"numbers\":[{\"favorite\":7},{\"lowest\":0}]}\"\"\")}",
            code.lazyPropertyToCreateObject.removeWhiteSpace()
        )
    }

    @Test
    fun testJsonObjectNotTruncated() {
        val code = com.airbnb.mvrx.mocking.printer.ConstructorCode(
                StateWithJsonObject(),
                Integer.MAX_VALUE,
                3
        )

        assertEquals(
            "valmockStateWithJsonObjectbylazy{ConstructorCodeTest.StateWithJsonObject(json=\"\"\"{\"color\":\"red\",\"numbers\":[{\"favorite\":7},{\"lowest\":0}]}\"\"\")}",
            code.lazyPropertyToCreateObject.removeWhiteSpace()
        )
    }

    @Test
    fun testJsonArray() {
        val code = com.airbnb.mvrx.mocking.printer.ConstructorCode(
                StateWithJsonArray(),
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        )

        assertEquals(
            "valmockStateWithJsonArraybylazy{ConstructorCodeTest.StateWithJsonArray(json=\"\"\"[{\"favorite\":7},{\"lowest\":0}]\"\"\")}",
            code.lazyPropertyToCreateObject.removeWhiteSpace()
        )
    }

    @Test
    fun testInvalidJson() {
        val code =
                com.airbnb.mvrx.mocking.printer.ConstructorCode(
                        StateWithInvalidJsonObject(),
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )

        assertEquals(
            "valmockStateWithInvalidJsonObjectbylazy{ConstructorCodeTest.StateWithInvalidJsonObject(json=\"notvalid{\\\"color\\\":\\\"red\\\",\\\"numbers\\\":[{\\\"favorite\\\":7},{\\\"lowest\\\":0}]}\")}",
            code.lazyPropertyToCreateObject.removeWhiteSpace()
        )
    }

    @Test
    fun testLazy() {
        val code = com.airbnb.mvrx.mocking.printer.ConstructorCode(
                StateWithLazy(),
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        )

        assertEquals(
            "valmockStateWithLazybylazy{ConstructorCodeTest.StateWithLazy(lazyInt=lazy{1})}",
            code.lazyPropertyToCreateObject.removeWhiteSpace()
        )
    }


    @Test
    fun testCustomTypePrinter() {
        data class Test(
            val date: CustomDate = CustomDate.fromString("2000")
        ) : MvRxState

        val result = com.airbnb.mvrx.mocking.printer.ConstructorCode(
                Test(),
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                customTypePrinters = listOf(
                        com.airbnb.mvrx.mocking.printer.typePrinter<CustomDate>(
                                transformImports = { it.plus("hello world") },
                                codeGenerator = { date, _ -> "CustomDate.fromString(\"${date.asString()}\")" }
                        )
                )
        )

        result.expect("ConstructorCodeTest.testCustomTypePrinter\$Test(date=CustomDate.fromString(\"2000\"))")

        assertTrue(result.imports.contains("hello world"))
    }

    @Test
    fun listIsTruncated() {
        data class Test(
            val list: List<Int> = listOf(1, 2, 3, 4)
        ) : MvRxState

        com.airbnb.mvrx.mocking.printer.ConstructorCode(Test(), 3, 200).expect("ConstructorCodeTest.listIsTruncated\$Test(list=listOf(1,2,3))")
    }

    @Test
    fun listIsNotTruncated() {
        data class Test(
            val list: List<Int> = listOf(1, 2, 3, 4)
        ) : MvRxState

        com.airbnb.mvrx.mocking.printer.ConstructorCode(
                Test(),
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        ).expect("ConstructorCodeTest.listIsNotTruncated\$Test(list=listOf(1,2,3,4))")
    }

    @Test
    fun listIsNotTruncatedWhenTypesDiffer() {
        data class Test(
            val list: List<Any> = listOf(1, 2, 3, "A")
        ) : MvRxState

        com.airbnb.mvrx.mocking.printer.ConstructorCode(Test(), 3, 200).expect("ConstructorCodeTest.listIsNotTruncatedWhenTypesDiffer\$Test(list=listOf(1,2,3,\"A\"))")
    }

    @Test
    fun listIsNotTruncatedWhenSomeItemsAreNull() {
        data class Test(
            val list: List<Int?> = listOf(null, 1, 2, 3)
        ) : MvRxState

        com.airbnb.mvrx.mocking.printer.ConstructorCode(Test(), 3, 200).expect("ConstructorCodeTest.listIsNotTruncatedWhenSomeItemsAreNull\$Test(list=listOf(null,1,2,3))")
    }

    @Test
    fun listIsTruncatedWhenAllItemsAreNull() {
        data class Test(
            val list: List<Int?> = listOf(null, null, null, null)
        ) : MvRxState

        com.airbnb.mvrx.mocking.printer.ConstructorCode(Test(), 3, 200).expect("ConstructorCodeTest.listIsTruncatedWhenAllItemsAreNull\$Test(list=listOf(null,null,null))")
    }

    @Suppress("ArrayInDataClass")
    @Test
    fun arrayIsTruncated() {
        data class Test(
            val list: Array<Int> = arrayOf(1, 2, 3, 4)
        ) : MvRxState

        com.airbnb.mvrx.mocking.printer.ConstructorCode(Test(), 3, 200).expect("ConstructorCodeTest.arrayIsTruncated\$Test(list=arrayOf(1,2,3))")
    }

    @Suppress("ArrayInDataClass")
    @Test
    fun arrayIsNotTruncated() {
        data class Test(
            val list: Array<Int> = arrayOf(1, 2, 3, 4)
        ) : MvRxState

        com.airbnb.mvrx.mocking.printer.ConstructorCode(
                Test(),
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        ).expect("ConstructorCodeTest.arrayIsNotTruncated\$Test(list=arrayOf(1,2,3,4))")
    }

    private fun <T : MvRxState> com.airbnb.mvrx.mocking.printer.ConstructorCode<T>.expect(expectedCode: String) {
        assertEquals("valmockTestbylazy{$expectedCode}", lazyPropertyToCreateObject.removeWhiteSpace())
    }

    data class StateWithJsonObject(val json: String = """{"color":"red","numbers":[{"favorite":7},{"lowest":0}]}""") :
        MvRxState

    data class StateWithInvalidJsonObject(val json: String = """not valid{"color":"red","numbers":[{"favorite":7},{"lowest":0}]}""") :
        MvRxState

    data class StateWithJsonArray(val json: String = """[{"favorite":7},{"lowest":0}]""") :
        MvRxState

    data class NestedObject(val nullableInt: Int? = null, val myEnum: MyEnum = MyEnum.A)

    data class StateWithLazy(val lazyInt: Lazy<Int> = lazy { 1 })

    enum class MyEnum {
        A
    }

    object MySingleton

    data class State(
        val int: Int = 1,
        val float: Float = 1f,
        val boolean: Boolean = false,
        val str: String = "hello",
        val charSequence: CharSequence = "'hi' with nested \"quotes\" and \ta tab",
        val double: Double = 4.5,
        val map: Map<Int, String> = mapOf(3 to "three", 2 to "two"),
        val strList: List<String> = listOf("hi", "there"),
        val nestedObject: NestedObject = NestedObject(),
        val singleTon: MySingleton = MySingleton,
        val nestedObjectList: List<NestedObject> = listOf(NestedObject())
    ) : MvRxState

    class CustomDate private constructor(private val time: Long) {
        fun asString(): String = time.toString()

        companion object {
            fun fromString(dateString: String) = CustomDate(dateString.toLong())
        }
    }
}

private fun String.removeWhiteSpace(): String {
    return replace(" ", "")
        .replace("\n", "")
        .replace("\r", "")
        .replace("\t", "")
}