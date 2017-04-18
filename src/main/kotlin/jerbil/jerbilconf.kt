package jerbil
import java.io.File
import java.net.InetAddress

fun loadDefaults() : HashMap<String, String> {
  return hashMapOf (
      "port" to "8000",
      "host" to InetAddress.getLocalHost().getHostName(),
      "root" to System.getProperty("user.dir"),
      "max_path" to "1000",
      "directory_menus" to "true",
      "debug" to "false"
  )
}

fun loadConfigFromFile(file : File) : Config {

  if (!file.exists()) {
    println("${file.getName()} does not exist, using defaults")
    return Config(loadDefaults())
  }

  fun isComment(line : String) = line.trim().startsWith("#")
  fun keepLine(line : String) = line.contains("=") && !isComment(line)
  val lines = file.readLines().filter{keepLine(it)}
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
  val root = File(kv.get("root")!!.toString())
  val max_path = kv.get("max_path")!!.toInt()
  val directory_menus = kv.get("directory_menus")!!.toBoolean()
  val debug = kv.get("debug")!!.toBoolean()
  val _kv : HashMap<String, String> = kv

  override fun toString() : String {
    val prefix = " > "
    val entries = _kv.map{"${it.key}: ${it.value}"}
    val confstr = entries.joinToString (
          separator = "\n$prefix",
          prefix = prefix
      )
    return """
       |Config:
       |$confstr """.trimMargin()
  }
}
