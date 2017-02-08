import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import java.nio.file.Paths
import java.nio.file.Path
import java.util.Hashtable
import kotlin.concurrent.thread

class Config {
   public val port = 8001
   public val host = "127.0.0.1"
   public val root = Paths.get("/home/jasp/")
   public val max_path = 1000
   public val directory_menus = true
}

////// Init Related

val CONF = Config()
val suffixTable = initSuffixTable()

fun initSuffixTable() : Hashtable<String, Char> { 
  val tbl = Hashtable<String, Char>()
  tbl.put("txt", '1')
  tbl.put("html", 'h')
  return tbl
}

////// Path Related

fun isPathInsideRoot(path : String) : Boolean {
  return path.startsWith(CONF.root.toString())
}

fun mapPathToFile(path : String) : String {
  val relativePath = Paths.get(path.trim{it == '/'})
  val newpath = CONF.root.resolve(relativePath).normalize()
  return newpath.toString()
}

fun mapFileToPath(file : File) : String {
  val absRoot = CONF.root.toAbsolutePath().toString()
  val absFile = file.getAbsolutePath().toString()
  return absFile.substring(absRoot.length)
}

////// IO Related

fun notFound() : String = "Resource not found"

fun dirToMenu(dir : File) : String {

  fun getTypeChar(file : File) : Char {
    val suffix = file.getName().split(".").last().toLowerCase()
    val char = if (file.isDirectory()) '1' 
               else {suffixTable.get(suffix) ?: '0'}
    return char
  }

  fun fileToLine(f : File) : String {
    val type = getTypeChar(f)
    val text = f.getName()
    val path = mapFileToPath(f)
    val host = CONF.host
    val port = CONF.port
    return "${type}${text}\t${path}\t${host}\t${port}\r\n"
  }

  if (!CONF.directory_menus || !dir.isDirectory())
    return notFound()

  val files = dir.listFiles()
  val buffer = StringBuffer()

  files.forEach{buffer.append(fileToLine(it))}
  val menu = buffer.toString()

  println(menu)
  return menu
}

fun readPathString(reader : BufferedInputStream) : String {
  val CR = '\r'
  val LF = '\n'
  val path = StringBuffer(CONF.max_path)

  while (true) {
    val ch = reader.read().toChar()
    val lastch = if (path.length > 0) path.last() else 0.toChar()
    val isEndOfPath = (lastch == CR && ch == LF)
    val isEOF = (ch == (-1).toChar())
    val isTooLarge = (path.length >= CONF.max_path)

    when {
      isEndOfPath || isEOF -> 
        return path.toString().trim()
      isTooLarge -> 
        return String()
      else -> 
        path.append(ch)
    }
  }
}

fun readFromWriteTo(from : InputStream, to : OutputStream) : Unit {
  val buf = ByteArray(2048,{0})
  loop@ while (true) {
    val r = from.read(buf)
    when {
      r == -1 -> break@loop
      else -> to.write(buf, 0, r)
    }
  }
  to.flush()
}

fun writeString(writer: OutputStream, str : String) : Unit {
  writer.write(str.toByteArray())
  writer.flush()
}

fun writeFile(writer: OutputStream, file : File) : Unit {
  val input = FileInputStream(file)
  readFromWriteTo(input, writer)
  input.close()
}

////// Main 

fun mainIO(sock : Socket) {
  val reader = BufferedInputStream(sock.getInputStream())
  val writer = BufferedOutputStream(sock.getOutputStream())
  val filePath = mapPathToFile(readPathString(reader))
  val file = File(filePath)
  val isFileInRoot = isPathInsideRoot(filePath)

  try {
    when {
      !isFileInRoot -> writeString(writer, notFound())
      file.isDirectory() -> writeString(writer, dirToMenu(file))
      file.isFile() -> writeFile(writer, file)
      else -> writeString(writer, notFound())
    }
    sock.close()
  }
  catch (e : SocketException) {
    println("SocketException on write: ${sock.getInetAddress()}")
  }
}

fun main(args: Array<String>) {
  val listener = ServerSocket(CONF.port)
  println("Listening on ${CONF.port}")

  while (true) {
    val sock = listener.accept()
    thread(block = {-> mainIO(sock)})
  }
}
