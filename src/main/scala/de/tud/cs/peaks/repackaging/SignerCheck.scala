package de.tud.cs.peaks.repackaging

import java.io.FileOutputStream
import java.security.KeyStore
import java.security.KeyPairGenerator
import java.math.BigInteger
import javax.security.auth.x500.X500Principal
import java.security.cert.X509Certificate
import java.security.KeyPair
import sun.security.x509.CertAndKeyGen
import sun.security.x509.X500Name
import java.util.Date
import sun.security.x509.CertificateSubjectName
import sun.security.x509.CertificateX509Key
import sun.security.x509.CertificateVersion
import java.security.SecureRandom
import sun.security.x509.CertificateIssuerName
import sun.security.x509.X509CertInfo
import sun.security.x509.CertificateSerialNumber
import sun.security.x509.CertificateAlgorithmId
import sun.security.x509.CertificateValidity
import sun.security.x509.AlgorithmId
import sun.security.x509.X509CertImpl

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