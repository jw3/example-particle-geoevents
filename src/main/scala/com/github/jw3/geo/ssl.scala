package com.github.jw3.geo

import java.io.{FileInputStream, InputStream}
import java.nio.file.Path
import java.security.{KeyStore, SecureRandom}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object ssl {
  sealed trait State { def enabled: Boolean }
  object State { def apply(enabled: Boolean): State = if (enabled) Enabled else Disabled }
  case object Enabled extends State { val enabled = true }
  case object Disabled extends State { val enabled = false }

  sealed trait KeystoreType { def id: String }
  case object JKS extends KeystoreType { val id = "JKS" }
  case object P12 extends KeystoreType { val id = "PKCS12" }

  def from(path: Path, pass: Array[Char]): HttpsConnectionContext = {
    val t = ksType(path)
    val ks: KeyStore = KeyStore.getInstance(t.get.id)
    ks.load(stream(path), pass)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, pass)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    ConnectionContext.https(sslContext)
  }

  private def stream(path: Path): InputStream = {
    if (path.isAbsolute) new FileInputStream(path.toFile)
    else getClass.getClassLoader.getResourceAsStream(path.toFile.toString)
  }

  private def ksType(name: Path): Option[KeystoreType] = name match {
    case n if n.toString.endsWith("p12") ⇒ Some(P12)
    case _ ⇒ None
  }
}
