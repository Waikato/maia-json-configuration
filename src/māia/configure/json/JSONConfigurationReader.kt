package māia.configure.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import māia.configure.Configuration
import māia.configure.metadata
import māia.configure.visitation.ConfigurationVisitable
import māia.util.asIterable
import māia.util.classForName
import māia.util.filter
import māia.util.map
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties


/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class JSONConfigurationReader(val source : JsonObject) : ConfigurationVisitable {

    constructor(json : String) : this(Json.parseToJsonElement(json) as JsonObject)

    val configClass = classForName<Configuration>((source["type"] as JsonPrimitive).content)

    override fun getConfigurationClass() : KClass<out Configuration> {
        return configClass
    }

    override fun iterateElements() : Iterator<ConfigurationVisitable.Element> {
        return (source["body"] as JsonObject).entries.iterator().map { entry ->
            val obj = entry.value as JsonObject
            if ("body" in obj) {
                ConfigurationVisitable.Element.SubConfiguration(entry.key, JSONConfigurationReader(obj), getConfigClassPropertyByName(entry.key).metadata)
            } else {
                ConfigurationVisitable.Element.Item(entry.key, decodePrimitiveObject(obj), getConfigClassPropertyByName(entry.key).metadata)
            }
        }
    }

    fun getConfigClassPropertyByName(name : String) : KProperty<*> {
        return configClass.memberProperties.iterator()
                .filter { it.name == name }
                .asIterable()
                .first()
    }

    private fun decodePrimitiveObject(obj : JsonObject) : Any? {
        val type = obj["type"]!!.jsonPrimitive.content
        val valuePrimitive = obj["value"]!!.jsonPrimitive
        return when (type) {
            "String" -> valuePrimitive.content
            "Boolean" -> valuePrimitive.boolean
            "Float" -> valuePrimitive.float
            "Double" -> valuePrimitive.double
            "Int" -> valuePrimitive.int
            "Long" -> valuePrimitive.long
            "null" -> null
            else -> throw Exception("Unrecognised type $type")
        }
    }

}
