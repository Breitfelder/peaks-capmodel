package de.tud.cs.peaks.slicing

/**
  * Created by benhermann on 25/08/16.
  */
trait JarFile {
  def addFile(filename: String, contents: Array[Byte])
}
