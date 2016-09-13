package de.tud.cs.peaks.repackaging


import java.io.{File, FileOutputStream}
import java.util.jar.{Attributes, JarEntry, JarOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import de.tud.cs.peaks.slicing.JarFile

/**
  * Created by benhermann on 01/09/16.
  */
class SimpleJarFile (jarFilename : String) extends JarFile {
  val manifest = new java.util.jar.Manifest();
  manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

  val zos = new JarOutputStream(new FileOutputStream(jarFilename), manifest)


  def addFile(filename: String, contents: Array[Byte]): Unit = {
    var entryname = filename

    if (entryname.startsWith("/")) entryname = entryname.substring(1)
    val entry = new JarEntry(entryname)
    entry.setTime(System.currentTimeMillis / 1000)
    zos.putNextEntry(entry)

    zos.write(contents)
    zos.closeEntry()
  }

  def close(): Unit = {
    zos.close()
  }

}
