package de.tud.cs.peaks.repackaging

import java.security.KeyStore
import java.io.OutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.util.jar.Manifest
import scala.util.hashing.Hashing
import java.nio.charset.Charset
import scala.collection.mutable.HashMap
import java.util.jar.Attributes
import java.security.MessageDigest
import java.util.Base64
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import java.security.Signature
import java.security.PrivateKey
import java.util.Arrays
import de.tud.cs.peaks.slicing.JarFile

class SignedJarFile(val out: OutputStream,
                    val keyStore: KeyStore,
                    val keyName: String,
                    val keyPass: String) extends JarFile {

  private val zos = new ZipOutputStream(out)
  private val hashAlgorithm = "SHA-256"
  private val hashFunction = MessageDigest.getInstance(hashAlgorithm)
  private val signingAlgorithm = "SHA256withRSA"

  private val MANIFEST_NAME = "META-INF/MANIFEST.MF"
  private val SIGNATURE_NAME = "META-INF/SIGNUMO.SF"
  private val SIGNATURE_RSA_NAME = "META-INF/SIGNUMO.RSA"

  private val ATTRIBUTE_NAME_MAXLENGTH = 70

  private val manifestAttributes = HashMap.empty[String, String]
  private val fileDigests = HashMap.empty[String, String]
  private val sectionDigests = HashMap.empty[String, String]

  def addManifestAttribute(name: String, value: String) {
    if (name.getBytes(Charset.forName("UTF-8")).length > ATTRIBUTE_NAME_MAXLENGTH)
      return
    manifestAttributes.put(name, value)
  }

  def addFile(filename: String, contents: Array[Byte]) {
    zos.putNextEntry(new ZipEntry(filename))
    zos.write(contents)
    zos.closeEntry();

    hashFunction.reset()
    val fileHash = Base64.getEncoder.encodeToString(hashFunction.digest(contents))
    fileDigests.put(filename, fileHash)
  }

  def close() {
    val m = writeManifest();
    val signatureContent = writeFileSignatures(generateManifestHash(m), generateManifestMainHash(m));
    writeSignature(signatureContent)
    zos.close();
  }

  private def writeManifest(): Manifest = {
    zos.putNextEntry(new ZipEntry(MANIFEST_NAME))
    val m = new Manifest()

    val main = m.getMainAttributes()
    main.put(Attributes.Name.MANIFEST_VERSION, "1.0")

    // write attributes
    for ((n, v) <- manifestAttributes) {
      main.put(n, v)
    }

    // write file hashes
    val digestName = new Attributes.Name(hashAlgorithm + "-Digest")
    for ((fn, digest) <- fileDigests) {
      val a = new Attributes()
      m.getEntries.put(fn, a)
      a.put(digestName, digest)
      sectionDigests.put(fn, generateSectionDigest(fn, a))
    }

    m.write(zos);
    zos.closeEntry();

    return m
  }

  private def generateSectionDigest(k: String, a: Attributes): String = {
    val m = new Manifest()
    m.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    val os = new ByteOutputStream
    m.write(os)
    val emptyLength = os.getBytes.length

    m.getEntries.put(k, a)

    m.write(os)
    var bytes = os.getBytes
    bytes = Arrays.copyOfRange(bytes, emptyLength, bytes.length)

    hashFunction.reset()
    return Base64.getEncoder.encodeToString(hashFunction.digest(bytes))
  }

  private def generateManifestHash(m: Manifest): String = {
    hashFunction.reset()
    val os = new ByteOutputStream
    m.write(os)
    return Base64.getEncoder.encodeToString(hashFunction.digest(os.getBytes))
  }

  private def generateManifestMainHash(m: Manifest): String = {
    val internalManifest = new Manifest();
    internalManifest.getMainAttributes.putAll(m.getMainAttributes.asInstanceOf[java.util.Map[_, _]])

    hashFunction.reset()
    val os = new ByteOutputStream
    internalManifest.write(os)
    return Base64.getEncoder.encodeToString(hashFunction.digest(os.getBytes))
  }

  private def writeFileSignatures(manifestHash: String, manifestMainHash: String): Array[Byte] = {
    zos.putNextEntry(new ZipEntry(SIGNATURE_NAME))
    val sigM = new Manifest();
    val mainAttr = sigM.getMainAttributes();

    mainAttr.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    mainAttr.put(new Attributes.Name(hashAlgorithm + "-Digest-Manifest"), manifestHash)
    mainAttr.put(new Attributes.Name(hashAlgorithm + "-Digest-Manifest-Main"), manifestMainHash)

    // individual file sections
    val digestAttr = new Attributes.Name(hashAlgorithm + "-Digest")
    for ((name, value) <- sectionDigests) {
      val a = new Attributes()
      sigM.getEntries.put(name, a)
      a.put(digestAttr, value)
    }

    sigM.write(zos)
    zos.closeEntry()

    val bos = new ByteOutputStream()
    sigM.write(bos)
    return bos.getBytes
  }

  private def writeSignature(content: Array[Byte]) {
    zos.putNextEntry(new ZipEntry(SIGNATURE_RSA_NAME))
    zos.write(sign(content))
  }

  private def sign(content: Array[Byte]): Array[Byte] = {
    val privKey = keyStore.getKey(keyName, keyPass.toCharArray()).asInstanceOf[PrivateKey]
    val signingAlgo = Signature.getInstance(signingAlgorithm)
    signingAlgo.initSign(privKey)
    signingAlgo.update(content)
    return signingAlgo.sign()
  }
}