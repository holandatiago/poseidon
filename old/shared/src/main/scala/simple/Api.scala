package simple

trait Api {
  def list(path: String): Seq[FileData]
}
