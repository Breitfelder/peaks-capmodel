package de.tud.cs.peaks.repackaging

import java.io.FileOutputStream
import java.util.jar.JarOutputStream
import java.util.jar.JarFile

class Scenario (val originalJars : Seq[JarFile], 
                val outputPath : String) {
  
  def run() {
    originalJars.map { 
      x => 
        new JarOutputStream(new FileOutputStream(x.getName()), 
                            ManifestCreator.createFromAncestor(x.getManifest)) }

  }
  
}