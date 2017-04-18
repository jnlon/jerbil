package jerbil
import java.io.File
import java.net.InetAddress
import java.lang.Exception

data class JerbilConfig (
  var port : Int = 8000,
  var host : String = InetAddress.getLocalHost().getHostName(),
  var root : File = File(System.getProperty("user.dir")),
  var max_path : Int =  1000,
  var directory_menus : Boolean = true,
  var debug : Boolean = false
)

fun String.toBoolean() : Boolean =
  if (this == "true") true
  else if (this == "false") false
  else throw Exception("Not a boolean value: '$this'")

fun loadJerbilConfigFile(file : File) : JerbilConfig {

  val conf = JerbilConfig()  // Default values
  println("Config: Loading from ${file.getCanonicalPath()}")

  if (!file.exists()) {
    println("Config: File does not exist, using defaults")
    return conf
  }

  val pairs = file.readLines()
    .map { it.split("=").map{ it.trim() } }
    .filter { it.size == 2 && !it.get(0).startsWith("#") }
    .map { Pair( it.get(0), it.get(1)) }

  for ((left, right) in pairs) {
    try {
      when (left) {
        "port" -> conf.port = right.toInt()
        "host" -> conf.host = right
        "root" -> conf.root = File(right)
        "max_path" -> conf.max_path = right.toInt()
        "directory_menus" -> conf.directory_menus = right.toBoolean()
        "debug" -> conf.debug = right.toBoolean()
        else -> throw Exception("Config: Key '$left' does not exist")
      }
    }
    catch (e : Exception) {
      println("Config: Cannot set '$left' to '$right': $e")
      System.exit(1)
    }
  }

  return conf
}
