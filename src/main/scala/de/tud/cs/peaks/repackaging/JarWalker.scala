package de.tud.cs.peaks.repackaging

import java.io.FileInputStream
import java.util.jar.JarInputStream

/**
  * Created by benhermann on 13/09/16.
  */
object JarWalker {
  def main(args: Array[String]) {
    val input = new JarInputStream(new FileInputStream(args(0)))

    var entry = input.getNextJarEntry
    while (entry != null) {
      println(entry.getName())

      entry = input.getNextJarEntry
    }
  }
}
