package simple

import upickle.default.{ReadWriter, macroRW}

case class FileData(name: String, size: Long)

object FileData {
  implicit val rw: ReadWriter[FileData] = macroRW
}
