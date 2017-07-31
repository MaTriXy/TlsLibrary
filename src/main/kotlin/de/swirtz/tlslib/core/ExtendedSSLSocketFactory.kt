import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class ExtendedSSLSocketFactory(val protocols: Array<String>, val cipherSuites: Array<String>, val delegate: SSLSocketFactory)
    : SSLSocketFactory() {

    override fun createSocket(p0: Socket?, p1: String?, p2: Int, p3: Boolean): Socket {
        return extend(delegate.createSocket(p0, p1, p2, p3) as SSLSocket)
    }

    override fun createSocket(p0: String?, p1: Int): Socket {
        return extend(delegate.createSocket(p0, p1) as SSLSocket)
    }

    override fun createSocket(p0: String?, p1: Int, p2: InetAddress?, p3: Int): Socket {
        return extend(delegate.createSocket(p0, p1, p2, p3) as SSLSocket)
    }

    override fun createSocket(p0: InetAddress?, p1: Int): Socket {
        return extend(delegate.createSocket(p0, p1) as SSLSocket)
    }

    override fun createSocket(p0: InetAddress?, p1: Int, p2: InetAddress?, p3: Int): Socket {
        return extend(delegate.createSocket(p0, p1, p2, p3) as SSLSocket)
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return protocols
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return protocols
    }

    private fun extend(socket: SSLSocket): Socket = socket.apply {
        enabledCipherSuites = cipherSuites
        enabledProtocols = protocols
    }
}