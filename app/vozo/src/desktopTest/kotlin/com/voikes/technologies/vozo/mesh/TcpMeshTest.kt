package com.voikes.technologies.vozo.mesh

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.voikes.technologies.vozo.data.ChatStore
import com.voikes.technologies.vozo.db.VozoDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.util.Properties
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the phone <-> desktop transport end to end over a real loopback
 * socket. The server side mirrors TcpHostTransport on a phone, so this covers
 * framing, the hello handshake, and message delivery in both directions.
 */
class TcpMeshTest {

    private var serverSocket: ServerSocket? = null
    private val framesReceivedByServer = mutableListOf<String>()
    private val serverErrors = mutableListOf<String>()
    private val serverLog = mutableListOf<String>()

    private class Fixture(val store: ChatStore, val db: VozoDatabase)

    private fun newFixture(): Fixture {
        val db = VozoDatabase(
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), VozoDatabase.Schema)
        )
        return Fixture(ChatStore(db), db)
    }

    @AfterTest
    fun tearDown() {
        runCatching { serverSocket?.close() }
    }

    /** Mirrors TcpHostTransport: expects a hello, replies with its own. */
    private fun startServer(phoneStore: ChatStore? = null): Int {
        val server = ServerSocket(0)
        serverSocket = server
        thread(isDaemon = true) {
            runCatching {
                serverLog.add("accepted")
                val socket = server.accept()
                val input = socket.getInputStream()
                val output = socket.getOutputStream()

                val first = StreamFraming.readFrame(input) ?: run {
                    serverLog.add("no first frame")
                    return@runCatching
                }
                serverLog.add("first frame: $first")
                val hello = Wire.parse(first)
                assertTrue(hello is Wire.Frame.Hello, "first frame must be a hello")
                phoneStore?.onFrame(Wire.hello(hello.peerId, hello.displayName))
                serverLog.add("replied hello")

                StreamFraming.writeFrame(output, Wire.hello("phone-peer", "Pixel test"))

                while (true) {
                    val frame = StreamFraming.readFrame(input) ?: run {
                        serverLog.add("input closed")
                        break
                    }
                    serverLog.add("recv: $frame")
                    synchronized(framesReceivedByServer) { framesReceivedByServer.add(frame) }
                }
            }.onFailure {
                synchronized(serverErrors) { serverErrors.add("${it::class.simpleName}: ${it.message}") }
            }
        }
        return server.localPort
    }

    private fun await(timeoutMs: Long = 10_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(50)
        }
        return condition()
    }

    @Test
    fun framingRoundTripsTextWithNewlinesAndSeparators() {
        val payload = "line one\nline two with émojis"
        val bytes = ByteArrayOutputStream()
        StreamFraming.writeFrame(bytes, payload)
        val restored = StreamFraming.readFrame(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(payload, restored)
    }

    @Test
    fun framingKeepsConsecutiveFramesSeparate() {
        val bytes = ByteArrayOutputStream()
        StreamFraming.writeFrame(bytes, Wire.chat("a", "first"))
        StreamFraming.writeFrame(bytes, Wire.chat("b", "second"))
        val input = ByteArrayInputStream(bytes.toByteArray())
        val one = Wire.parse(StreamFraming.readFrame(input)!!)
        val two = Wire.parse(StreamFraming.readFrame(input)!!)
        assertEquals("first", (one as Wire.Frame.Chat).body)
        assertEquals("second", (two as Wire.Frame.Chat).body)
    }

    @Test
    fun phoneHelloCreatesPeerConversationOnDesktop() {
        val desktop = newFixture()
        val port = startServer()
        val transport = TcpClientTransport("desktop-peer", "Laptop", port)
        val controller = MeshController(listOf(transport))
        controller.onFrame = { desktop.store.onFrame(it) }
        transport.connectTo("127.0.0.1")

        val found = await {
            desktop.db.vozoDatabaseQueries.selectConversations().executeAsList().isNotEmpty()
        }
        assertTrue(
            found,
            "phone hello should create a conversation. serverLog=$serverLog " +
                "serverErrors=$serverErrors clientError=${transport.lastError} " +
                "peers=${transport.peers.value}",
        )

        val conversation = desktop.db.vozoDatabaseQueries.selectConversations().executeAsOne()
        assertEquals("Pixel test", conversation.title)
        assertEquals("phone-peer", conversation.peer_id)

        transport.stop()
        controller.stop()
    }

    @Test
    fun messageTravelsFromDesktopToPhone() {
        val phone = newFixture()
        val port = startServer(phoneStore = phone.store)

        val desktop = newFixture()
        val transport = TcpClientTransport("desktop-peer", "Laptop", port)
        val controller = MeshController(listOf(transport))
        controller.onFrame = { desktop.store.onFrame(it) }
        desktop.store.attachTransport(controller)
        transport.connectTo("127.0.0.1")

        assertTrue(
            await { transport.peers.value.isNotEmpty() },
            "desktop should discover the phone peer",
        )

        val chatId = desktop.store.startPeerChat("phone-peer", "Pixel test")
        desktop.store.sendMessage(chatId, "hello from the laptop")

        assertTrue(
            await {
                synchronized(framesReceivedByServer) { framesReceivedByServer.isNotEmpty() }
            },
            "phone should receive the chat frame",
        )
        val frame = synchronized(framesReceivedByServer) { framesReceivedByServer.first() }
        assertTrue(
            frame.contains("hello from the laptop"),
            "frame should carry the body, got: $frame",
        )

        transport.stop()
        controller.stop()
    }
}
