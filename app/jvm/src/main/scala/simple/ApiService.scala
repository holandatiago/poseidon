package simple

object ApiService extends Api {
  def list(path: String): Seq[FileData] = {
    println(s"Getting files list for path:\t$path")
    val (dir, last) = path.splitAt(path.lastIndexOf("/") + 1)
    new java.io.File("./" + dir).listFiles().toSeq
      .filter(_.getName.startsWith(last))
      .map(f => FileData(f.getName, f.length()))
  }
}
