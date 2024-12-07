package dev.alphexo.boxattestation.utils

import android.util.Log
import android.util.Xml
import dev.alphexo.boxattestation.models.CertCA
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.io.encoding.Base64
import java.io.ByteArrayInputStream
import java.security.PublicKey
import kotlin.io.encoding.ExperimentalEncodingApi
import java.util.Base64 as JavaBase64

class CertInfo(xmlString: String) {
    private var deviceId = "Unknown DeviceID"
    private var privateEC = "Unknown Private EC Key"
    private var privateRSA = "Unknown Private RSA Key"
    var certsEC = mutableListOf<X509Certificate>()
    var certsRSA = mutableListOf<X509Certificate>()
    private val certFile = CertFile()


    @OptIn(ExperimentalEncodingApi::class)
    fun stringToX509Certificate(certificateString: String): X509Certificate {
        var cleanedCert: String = certificateString
        Log.v("CertInfo", "stringToX509Certificate: $cleanedCert")

        if (!cleanedCert.startsWith("MIIC")) {
            cleanedCert = certFile.cleanCertificate(cleanedCert)
            Log.v("CertInfo", "stringToX509Certificate (CLEANED): $cleanedCert")
        }

        val certificateData = Base64.decode(cleanedCert)
        val certificateFactory = CertificateFactory.getInstance("X.509")
        return certificateFactory.generateCertificate(ByteArrayInputStream(certificateData)) as X509Certificate
    }

    init {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xmlString))

        var eventType = parser.eventType
        var currentAlgorithm = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Keybox" -> deviceId = parser.getAttributeValue(null, "DeviceID")
                        "Key" -> currentAlgorithm = parser.getAttributeValue(null, "algorithm")
                        "PrivateKey" -> {
                            if (currentAlgorithm == "ecdsa") {
                                privateEC = parser.nextText()
                            } else if (currentAlgorithm == "rsa") {
                                privateRSA = parser.nextText()
                            }
                        }

                        "Certificate" -> {
                            if (currentAlgorithm == "ecdsa") {
                                certsEC.add(stringToX509Certificate(parser.nextText()))
                            } else if (currentAlgorithm == "rsa") {
                                certsRSA.add(stringToX509Certificate(parser.nextText()))
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    fun verifyCertificateCA(): Pair<CertCA, CertCA> {
        val verifyChainEC = certsEC
            .map { getCertCA(it.publicKey) }.firstOrNull { it != CertCA.UNKNOWN } ?: CertCA.UNKNOWN
        val verifyChainRSA = certsRSA
            .map { getCertCA(it.publicKey) }.firstOrNull { it != CertCA.UNKNOWN } ?: CertCA.UNKNOWN

        return Pair(verifyChainEC, verifyChainRSA)
    }

    private fun getCertCA(publicKey: PublicKey): CertCA {
        val publicKeyBase64 = JavaBase64.getEncoder().encodeToString(publicKey.encoded)
        Log.v("verifyCertificateChain", "Public Key (Base64): $publicKeyBase64")

        return when (publicKeyBase64) {
            CertAuthorities.AOSP_EC -> CertCA.AOSP_EC
            CertAuthorities.AOSP_RSA -> CertCA.AOSP_RSA
            CertAuthorities.GOOGLE -> CertCA.GOOGLE
            CertAuthorities.KNOX -> CertCA.KNOX
            else -> CertCA.UNKNOWN
        }
    }
}



