package jerbil
import java.io.File
import java.net.InetAddress
import java.nio.file.Paths
import java.nio.file.Path

/*
Architecture: 
  - Constructor which takes explicit options, simply sets them
  - Constructor which takes a file path, gets parsed
*/

fun parseConfigFile() {


}

class Config(
    val port : Int = 8000,
    val host : String = InetAddress.getLocalHost().getHostName(),
    val root : Path = Paths.get(System.getProperty("user.dir")),
    val max_path : Int = 1000,
    val directory_menus : Boolean = true) 
{
   public fun print() : Unit {
     val confstr = """ 
     |Config:
     | port = $port
     | host = $host
     | root = $root 
     | max_path = $max_path
     | directory_menus = $directory_menus
     """.trimMargin()
     println(confstr)
   }
}
