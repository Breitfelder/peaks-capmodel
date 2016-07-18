package de.tud.cs.peaks.repackaging

import java.security.KeyStore
import java.io.OutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import scala.util.hashing.Hashing

class SignedJarFile (val out : OutputStream, 
                     val keyStore : KeyStore, 
                     val keyName : String,
                     val keyPass : String) 
{
  
  val zos = new ZipOutputStream(out)
  val hashFunction = null

   def addManifestAttribute(name : String, value : String) {
     
   }
   
   def addFile(filename: String, contents : Array[Byte]) {
     
     zos.putNextEntry(new ZipEntry(filename))
     zos.write(contents)
     zos.closeEntry();
   }
   
   def close() {
     
   }
}