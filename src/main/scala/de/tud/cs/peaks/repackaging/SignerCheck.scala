package de.tud.cs.peaks.repackaging

import java.io.FileOutputStream
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

import sun.security.x509.AlgorithmId
import sun.security.x509.CertificateAlgorithmId
import sun.security.x509.CertificateSerialNumber
import sun.security.x509.CertificateValidity
import sun.security.x509.CertificateVersion
import sun.security.x509.CertificateX509Key
import sun.security.x509.X500Name
import sun.security.x509.X509CertImpl
import sun.security.x509.X509CertInfo

object SignerCheck {
  def main(args: Array[String]): Unit = {
    // setup
    val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
    val keyname = "ben"
    val keypass = "ben"
    
    keystore.load(null, keypass.toCharArray())
    
    val keygen = KeyPairGenerator.getInstance("RSA")
    keygen.initialize(1024)
    val keypair = keygen.generateKeyPair()
    
    val cert = generateCertificate("CN=My Application,O=My Organisation,L=My City,C=DE", keypair, 365, "SHA256WithRSA")
    
    keystore.setKeyEntry(keyname, keypair.getPrivate, keypass.toCharArray(), Array(cert))
    
    // trial
    val sjf = new SignedJarFile(new FileOutputStream("test.jar"), keystore, keyname, keypass)
    
    // TODO make path independent from user 
    val fileContent : Array[Byte] = Files.readAllBytes(Paths.get("/Users/benhermann/Code/peaks-capmodel/target/scala-2.11/classes/de/tud/cs/peaks/repackaging/SignedJarFile.class"))
    sjf.addFile("de/tud/cs/peaks/repackaging/SignedJarFile.class", fileContent)
    sjf.close()
  }
  
  
  def generateCertificate(dn : String, pair : KeyPair, days : Int, algorithm: String) : X509Certificate =
{
  val privkey = pair.getPrivate();
  val info = new X509CertInfo();
  val from = new Date();
  val to = new Date(from.getTime() + days * 86400000l);
  val interval = new CertificateValidity(from, to);
  val sn = new BigInteger(64, new SecureRandom());
  val owner = new X500Name(dn);
 
  info.set(X509CertInfo.VALIDITY, interval);
  info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
  info.set(X509CertInfo.SUBJECT, owner);
  info.set(X509CertInfo.ISSUER, owner);
  info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
  info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
  var algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
  info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
 
  // Sign the cert to identify the algorithm that's used.
  var cert = new X509CertImpl(info);
  cert.sign(privkey, algorithm);
 
  // Update the algorithm, and resign.
  algo = cert.get(X509CertImpl.SIG_ALG).asInstanceOf[AlgorithmId];
  info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
  cert = new X509CertImpl(info);
  cert.sign(privkey, algorithm);
  return cert;
}   
  

}