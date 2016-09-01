package de.tud.cs.peaks.repackaging


import java.io.{File, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import de.tud.cs.peaks.slicing.JarFile

/**
  * Created by benhermann on 01/09/16.
  */
class SimpleJarFile (jarFilename : String) extends JarFile {
  val zos = new ZipOutputStream(new FileOutputStream(jarFilename))


  def addFile(filename: String, contents: Array[Byte]): Unit = {
    zos.putNextEntry(new ZipEntry(filename))
    zos.write(contents)
    zos.closeEntry()
  }

  def close(): Unit = {
    zos.close()
  }

}
