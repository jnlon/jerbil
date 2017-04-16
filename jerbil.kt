package jerbil

import jerbil.Config

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
import kotlin.collections.Collection
import kotlin.concurrent.thread
import java.util.Optional
import java.util.Hashtable
import java.util.ArrayList

////// Init Related

var CONF = loadDefaultConfig()
val suffixTable = initSuffixTable()

fun initSuffixTable() : Hashtable<String, Char> { 

  val tbl = Hashtable<String, Char>()
  val archives = "z,7z,xz,gz,tar,lz,rar,bz2,apk,jar,lzma".split(",")
  val images = "jpg,jpeg,png".split(",")
  val audio = "ogg,mp3,wav,flac,m4a,opus,aac".split(",")

  archives.forEach{tbl.put(it, '5')}
  images.forEach{tbl.put(it, 'I')}
  audio.forEach{tbl.put(it, 's')}

  tbl.put("txt", '0')
  tbl.put("html", 'h')
  tbl.put("gif", 'g')

  return tbl
}

////// Path Related

fun pathToFile(path : String) : Optional<File> {
  val relativePath = path.trim{it == '/'}
  val absPath = CONF.root.resolve(relativePath).normalize()
  val inRoot = absPath.startsWith(CONF.root)
  when { 
    inRoot -> return Optional.of(File(absPath.toString()))
    else -> return Optional.empty()
  }
}

fun fileToPath(file : File) : String {
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
               else suffixTable.get(suffix) ?: '0'
    return char
  }

  fun fileToLine(f : File) : String {
    val type = getTypeChar(f)
    val text = f.getName()
    val path = fileToPath(f)
    val host = CONF.host
    val port = CONF.port
    return "${type}${text}\t${path}\t${host}\t${port}\r\n"
  }

  if (!CONF.directory_menus || !dir.isDirectory())
    return notFound()

  val files = dir.listFiles()?.toCollection(ArrayList())?.sorted() ?: ArrayList()
  val menu = files.map{fileToLine(it)}.joinToString(separator = "")

  println(menu)
  return menu
}

fun readPathString(reader : BufferedInputStream) : String {
  val CR = '\r'.toInt()
  val LF = '\n'.toInt()
  val path = StringBuffer(CONF.max_path)

  for (i in 0..CONF.max_path) {
    val ch = reader.read()
    val lastch = if (i <= 0) 0 else path.last().toInt()
    val isEndOfPath = (lastch == CR && ch == LF)
    val isEOF = (ch == -1)

    if (isEndOfPath || isEOF)
      return path.toString().trim()

    path.append(ch.toChar())
  }
  return String()
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
  val rawPath = readPathString(reader)
  val file : Optional<File> = pathToFile(rawPath) // TODO: Make this Optional?

  println(">>> $rawPath")

  try {
    when {
      !file.isPresent() -> writeString(writer, notFound()) 
      file.get().isDirectory() -> writeString(writer, dirToMenu(file.get()))
      file.get().isFile() -> writeFile(writer, file.get())
      else -> writeString(writer, notFound())
    }
    sock.close()
  }
  catch (e : SocketException) {
    println("SocketException: ${sock.getInetAddress()}")
  }
}

fun main(args: Array<String>) {
  val conffile = if (args.size > 1) args.get(1) else "jerbil.conf"
  CONF = loadConfigFromFile(File(conffile))
  println("Conffile: $conffile")
  println(CONF.toString())

  val listener = ServerSocket(CONF.port)

  while (true) {
    val sock = listener.accept()
    thread(block = {-> mainIO(sock)})
  }
}
