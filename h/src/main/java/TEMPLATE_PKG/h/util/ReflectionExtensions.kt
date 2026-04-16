package ${BASE_PACKAGE}.h.util

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method


inline fun <reified T> Any.getField(fieldName: String): T? = runCatching {
    XposedHelpers.getObjectField(this, fieldName) as? T
}.getOrNull()

fun Any.getAllFields(): List<Field> {
    val fields = mutableListOf<Field>()
    var clazz: Class<*>? = this.javaClass

    // 向上追溯父类，直到 Any 或 Object
    while (clazz != null && clazz != Any::class.java && clazz != Object::class.java) {
        clazz.declaredFields.forEach { field ->
            // 提前尝试解除限制，规避 SecurityException
            runCatching { field.isAccessible = true }
            fields.add(field)
        }
        clazz = clazz.superclass
    }
    return fields
}

inline fun <reified T> Any.call(methodName: String, vararg params: Any?): T? = runCatching {
    XposedHelpers.callMethod(this, methodName, *params) as? T
}.getOrNull()

fun resolveClass(
    typeName: String,
    lpparam: XC_LoadPackage.LoadPackageParam
): Class<*>? {
    return primitiveTypes[typeName]
        ?: runCatching { Class.forName(typeName, false, lpparam.classLoader) }.getOrNull()
}

fun resolveParamTypes(
    typeNames: List<String>,
    lpparam: XC_LoadPackage.LoadPackageParam
): Array<Class<*>>? {
    val resolved = mutableListOf<Class<*>>()
    typeNames.forEach { typeName ->
        val clazz = resolveClass(typeName, lpparam) ?: return null
        resolved += clazz
    }
    return resolved.toTypedArray()
}

fun findField(
    clazz: Class<*>,
    fieldName: String
) = allFieldsOf(clazz).firstOrNull { it.name == fieldName }

fun allFieldsOf(clazz: Class<*>): List<Field> {
    val fields = mutableListOf<Field>()
    var current: Class<*>? = clazz
    while (current != null && current != Any::class.java) {
        fields += current.declaredFields
        current = current.superclass
    }
    return fields
}

fun superClassesOf(clazz: Class<*>): List<String> {
    val chain = mutableListOf<String>()
    var current = clazz.superclass
    while (current != null && current != Any::class.java) {
        chain += current.name
        current = current.superclass
    }
    return chain
}

fun methodSignature(method: Method): String {
    val params = method.parameterTypes.joinToString(",") { it.name }
    return "${method.returnType.name} ${method.name}($params)"
}

fun constructorSignature(constructor: Constructor<*>): String {
    val params = constructor.parameterTypes.joinToString(",") { it.name }
    return "${constructor.declaringClass.name}($params)"
}

private val primitiveTypes = mapOf(
    "boolean" to Boolean::class.javaPrimitiveType!!,
    "byte" to Byte::class.javaPrimitiveType!!,
    "char" to Char::class.javaPrimitiveType!!,
    "short" to Short::class.javaPrimitiveType!!,
    "int" to Int::class.javaPrimitiveType!!,
    "long" to Long::class.javaPrimitiveType!!,
    "float" to Float::class.javaPrimitiveType!!,
    "double" to Double::class.javaPrimitiveType!!,
    "void" to Void.TYPE
)