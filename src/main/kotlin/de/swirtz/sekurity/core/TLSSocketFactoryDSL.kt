package de.swirtz.sekurity.core

import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.nio.file.Paths
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

/**
 * Provides DSL for creating JSSE [SSLSocketFactory] and [SSLServerSocketFactory] connections
 */
@TlsDSLMarker
class TLSSocketFactoryProvider(init: ProviderConfiguration.() -> Unit) {


    private val config: ProviderConfiguration = ProviderConfiguration().apply(init)
    private val LOG = LoggerFactory.getLogger(TLSSocketFactoryProvider::class.java)

    fun createSocketFactory(protocols: List<String>): SSLSocketFactory = with(createSSLContext(protocols)) {
        return ExtendedSSLSocketFactory(socketFactory, protocols.toTypedArray(),
                getOptionalCipherSuites() ?: socketFactory.defaultCipherSuites)
    }

    fun createServerSocketFactory(protocols: List<String>): SSLServerSocketFactory = with(createSSLContext(protocols)) {
        return ExtendedSSLServerSocketFactory(serverSocketFactory, protocols.toTypedArray(),
                getOptionalCipherSuites() ?: serverSocketFactory.defaultCipherSuites)
    }

    private fun getOptionalCipherSuites() = config.socketConfig?.cipherSuites?.toTypedArray()


    private fun createSSLContext(protocols: List<String>): SSLContext {
        if (protocols.isEmpty()) {
            throw IllegalArgumentException("At least one protocol must be provided.")
        }
        return SSLContext.getInstance(protocols[0]).apply {
            val kmConfig = config.kmConfig
            val tmConfig = config.tmConfig
            LOG.debug("Creating Factory with \nKeyManager: $kmConfig \nTrustManager: $tmConfig")

            val keyManagerFactory = kmConfig?.let { conf ->
                val defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm()
                LOG.debug("KeyManager default algorithm: $defaultAlgorithm")
                KeyManagerFactory.getInstance(conf.algorithm ?: defaultAlgorithm).apply {
                    init(loadKeyStore(conf), conf.password)
                }
            }
            val trustManagerFactory = tmConfig?.let { conf ->
                val defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
                LOG.debug("TrustManager default algorithm: $defaultAlgorithm")
                TrustManagerFactory.getInstance(conf.algorithm ?: defaultAlgorithm).apply {
                    init(loadKeyStore(conf))
                }
            }

            init(keyManagerFactory?.keyManagers, trustManagerFactory?.trustManagers, SecureRandom())
        }

    }

    private fun loadKeyStore(store: Store) = KeyStore.getInstance(store.fileType).apply {
        LOG.debug("load keystore from ${store.name}. current dir: ${Paths.get(".").toFile().absolutePath}")
        try {
            load(FileInputStream(store.name), store.password)
        }catch (e: Exception){
            LOG.debug("Exception when loading file", e)
        }
    }
}

@DslMarker
annotation class TlsDSLMarker

@TlsDSLMarker
data class SocketConfiguration(var cipherSuites: List<String>? = null, var timeout: Int? = null, var clientAuth: Boolean = false)

@TlsDSLMarker
class Store(val name: String, val fileType: String = "JKS") {
    var algorithm: String? = null
    var password: CharArray? = null


    infix fun withPass(pass: String) = apply {
        password = pass.toCharArray()
    }

    infix fun algorithm(algo: String) = apply {
        algorithm = algo
    }

}

@TlsDSLMarker
class ProviderConfiguration {

    var kmConfig: Store? = null
    var tmConfig: Store? = null
    var socketConfig: SocketConfiguration? = null

    fun open(name: String, type: String = "JKS") = Store(name, type)

    fun sockets(configInit: SocketConfiguration.() -> Unit) {
        this.socketConfig = SocketConfiguration().apply(configInit)
    }

    fun keyManager(store: () -> Store) {
        this.kmConfig = store()
    }

    fun trustManager(store: () -> Store) {
        this.tmConfig = store()
    }
}