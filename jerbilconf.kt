package jerbil
import java.io.File
import java.net.InetAddress
import java.nio.file.Paths
import java.nio.file.Path
import java.util.HashMap

fun loadDefaults() : HashMap<String, String> {
  val kv = HashMap<String, String>()
  kv.put("port", "8000")
  kv.put("host", InetAddress.getLocalHost().getHostName())
  kv.put("root", System.getProperty("user.dir"))
  kv.put("max_path", "1000")
  kv.put("directory_menus", "true")
  return kv
}

fun loadConfigFromFile(confile : File) : Config {
  fun isComment(line : String) = line.trim().startsWith("#")
  fun keepLine(line : String) = line.contains("=") && !isComment(line)
  val lines = confile.readLines().filter{keepLine(it)}
  val kv = loadDefaults()

  for (line in lines) {
    val s = line.split("=")
    val key = s.get(0).trim()
    val value = s.get(1).trim()
    kv.put(key, value)
  }

  return Config(kv)
}

fun loadDefaultConfig() = Config(loadDefaults())

class Config(kv : HashMap<String, String>) {
  val port = kv.get("port")!!.toInt() 
  val host = kv.get("host")!!.toString()
  val root = Paths.get(kv.get("root")!!.toString())
  val max_path = kv.get("max_path")!!.toInt()
  val directory_menus = kv.get("directory_menus")!!.toBoolean()

  override fun toString() : String {
    return """
       |Config 
       | port = $port
       | host = $host
       | root = $root
       | max_path = $max_path
       | directory_menus = $directory_menus
       |""".trimMargin()
  }
} 
