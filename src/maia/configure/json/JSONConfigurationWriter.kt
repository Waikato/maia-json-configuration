package maia.configure.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import maia.configure.Configuration
import maia.configure.ConfigurationElement
import maia.configure.visitation.ConfigurationVisitor
import maia.util.kotlinClass
import java.util.*
import kotlin.reflect.KClass

/**
 * Configuration writer which outputs the configuration in JSON format.
 *
 * @param indent    The level of indentation between sub-objects. If
 *                  zero or negative, no indentation or new-lines are
 *                  inserted.
 */
class JSONConfigurationWriter : ConfigurationVisitor {

    private val subconfigurationStack = Stack<Pair<String, HashMap<String, JsonElement>>>()
    private var result : JsonObject? = null

    override fun begin(cls: KClass<out Configuration>) {
        result = null
        subconfigurationStack.clear()
        val new = HashMap<String, JsonElement>()
        new["type"] = JsonPrimitive(cls.qualifiedName)
        subconfigurationStack.push(Pair("", new))
        subconfigurationStack.push(Pair("", HashMap()))
    }

    override fun item(name : String, value : Any?, metadata : ConfigurationElement.Metadata) {
        val top = subconfigurationStack.peek()
        top.second[name] = valueToJsonElement(value)
    }

    override fun beginSubConfiguration(name : String, cls : KClass<out Configuration>, metadata : ConfigurationElement.Metadata) {
        val new = HashMap<String, JsonElement>()
        new["type"] = JsonPrimitive(cls.qualifiedName)
        subconfigurationStack.push(Pair(name, new))
        subconfigurationStack.push(Pair("", HashMap()))
    }

    override fun endSubConfiguration() {
        val body = subconfigurationStack.pop()
        subconfigurationStack.peek().second["body"] = JsonObject(body.second)
        val top = subconfigurationStack.pop()
        subconfigurationStack.peek().second[top.first] = JsonObject(top.second)
    }

    override fun end() {
        val body = subconfigurationStack.pop()
        subconfigurationStack.peek().second["body"] = JsonObject(body.second)
        val top = subconfigurationStack.pop()
        result = JsonObject(top.second)
    }

    fun toJson() : JsonObject {
        return result ?: throw Exception("Writer not ended")
    }

    override fun toString() : String {
        return toJson().toString()
    }

    private fun valueToJsonElement(value : Any?) : JsonElement {
        return when (value) {
            null, is String, is Number, is Boolean -> jsonPrimitiveObject(value)
            is Array<*> -> JsonArray(value.map { valueToJsonElement(it) })
            is Iterable<*> -> JsonArray(value.map { valueToJsonElement(it) })
            else -> throw Exception("Can't write type ${value.kotlinClass.simpleName} to JSON")
        }
    }

    private fun jsonPrimitiveObject(value : Any?) : JsonObject {
        val map = HashMap<String, JsonPrimitive>().apply {
            set("type", JsonPrimitive(value?.kotlinClass?.simpleName))
            set("value", when (value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> JsonPrimitive(null as String?)
            })
        }
        return JsonObject(map)
    }

}
