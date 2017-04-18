package jerbil

import jerbil.loadJerbilConfigFile
import jerbil.JerbilConfig

import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import kotlin.concurrent.thread
import java.util.ArrayList

////// Config

var CONF = JerbilConfig()

////// Paths

fun File.resolveLocalPath(path : String) : File {
  val other = File(path.trim('/'))
  val target = CONF.root.resolve(other).normalize()
  return when {
    target.startsWith(CONF.root) -> target
    else -> File("")
  }
}

class GopherPath(path : String) {
  val raw = path
  val file = CONF.root.resolveLocalPath(path)
  val isFile = file.isFile()
  val isDirectory = file.isDirectory()
}

////// IO

fun debugln(s : String) = if (CONF.debug) println(s) else Unit
fun notFound() : String = "Resource not found\n"

fun charTypeOfFile(file : File) : Char {
  if (file.isDirectory()) return '1'

  val archives = "z,7z,xz,gz,tar,lz,rar,bz2,apk,jar,lzma".split(",")
  val images = "jpg,jpeg,png".split(",")
  val audio = "ogg,mp3,wav,flac,m4a,opus,aac".split(",")
  val text = "txt,conf,csv,md,json".split(",")
  val html = "html,xhtml".split(",")

  val suffix = file.extension.toLowerCase()

  return when (suffix) {
    in archives -> '5'
    in images -> 'I'
    in audio -> 's'
    in text -> '0'
    in html -> 'h'
    "gif" -> 'g'
    else -> '9' // Assume binary file
  }
}

fun File.listFilesSorted() : List<File> {
  val files = this.listFiles()?.toCollection(ArrayList()) ?: ArrayList<File>()
  return files.sorted()
}

fun dirToMenu(dir : File) : String {

  fun fileToLine(f : File) : String {
    val type = charTypeOfFile(f)
    val text = f.getName()
    val path = "/${f.relativeTo(CONF.root)}"
    return "${type}${text}\t${path}\t${CONF.host}\t${CONF.port}\r\n"
  }

  if (!CONF.directory_menus || !dir.isDirectory())
    return notFound()

  val files = dir.listFilesSorted()
  val menu = files.map{fileToLine(it)}.joinToString(separator = "")

  println(menu)
  return menu
}

fun readGopherPath(reader : InputStream) : GopherPath {
  val CR = '\r'.toInt()
  val LF = '\n'.toInt()
  val path = StringBuffer(CONF.max_path)

  for (i in 0..CONF.max_path) {
    val ch = reader.read()
    val lastch = if (i <= 0) 0 else path.last().toInt()
    val isEndOfPath = (lastch == CR && ch == LF)
    val isEOF = (ch == -1)

    if (isEndOfPath || isEOF)
      return GopherPath(path.toString().trim())

    path.append(ch.toChar())
  }
  return GopherPath("")
}

fun InputStream.copyToFlush(to : OutputStream) {
  this.copyTo(to)
  to.flush()
}

fun writeString(writer: OutputStream, str : String) =
  str.byteInputStream().copyToFlush(writer)

fun writeFile(writer: OutputStream, file : File) =
  file.inputStream().use{ it.copyToFlush(writer) }

fun writeDirMenu(writer : OutputStream, file : File) =
  writeString(writer, dirToMenu(file))

fun writeNotFound(writer : OutputStream) =
  writeString(writer, notFound())

////// Main 

fun mainIO(sock : Socket) {
  val reader = sock.getInputStream().buffered()
  val writer = sock.getOutputStream().buffered()
  val path = readGopherPath(reader)

  println(">>> ${path.raw}")
  println(">>> ${path.file}")

  try {
    when {
      path.isDirectory -> writeDirMenu(writer, path.file)
      path.isFile -> writeFile(writer, path.file)
      else -> writeNotFound(writer)
    }
  }
  catch (e : SocketException) {
    println("SocketException: ${sock.getInetAddress()}")
  }
}

fun main(args: Array<String>) {
  val cf = if (args.size > 1) args.get(1) else "jerbil.conf"
  CONF = loadJerbilConfigFile(File(cf))
  println("Config: $CONF ")

  val listener = ServerSocket(CONF.port)

  while (true) {
    val sock = listener.accept()
    thread(block = {-> sock.use{mainIO(it)}})
  }
}
