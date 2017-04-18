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

  val pairs = file.readLines().filter {
    it.contains("=") && !it.trim().startsWith("#")
  }.map {
    val p = it.split("=").map{it.trim()}
    Pair( p.get(0), p.get(1) )
  }

  pairs.forEach {
    (left, right) -> 
      try {
        println("Config: Setting '$left' to '$right'")
        when (left) {
          "port" -> conf.port = right.toInt()
          "host" -> conf.host = right
          "root" -> conf.root = File(right)
          "max_path" -> conf.max_path = right.toInt()
          "directory_menus" -> conf.directory_menus = right.toBoolean()
          "debug" -> conf.debug = right.toBoolean()
          else -> println("Config: Key '$left' does not exist")
        }
      }
      catch (e : Exception) {
        println("Config: Cannot set '$left' to '$right': $e")
      }
    }
  return conf
}
