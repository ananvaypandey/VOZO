package com.voikes.technologies.vozo.mesh

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Length-prefixed framing for stream transports (TCP).
 *
 * Nearby Connections delivers whole payloads, so it needs no framing, but a
 * TCP socket is a byte stream with no message boundaries. Every frame is
 * prefixed with a 4-byte big-endian length so a reader can tell exactly where
 * one frame ends and the next begins.
 */
object StreamFraming {

    const val MAX_FRAME_BYTES = 1 shl 20

    fun writeFrame(out: OutputStream, frame: String) {
        val bytes = frame.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_FRAME_BYTES) { "frame too large: ${bytes.size}" }
        // Deliberately not closed: the caller owns this stream and it is a
        // socket. Wrapping it in `use` would shut the whole connection down
        // after a single frame.
        val data = DataOutputStream(out)
        data.writeInt(bytes.size)
        data.write(bytes)
        data.flush()
    }

    /** Blocking read of a single frame. Returns null at end of stream. */
    fun readFrame(input: InputStream): String? {
        val data = DataInputStream(input)
        val length = try {
            data.readInt()
        } catch (e: java.io.EOFException) {
            return null
        }
        if (length < 0 || length > MAX_FRAME_BYTES) return null
        val buffer = ByteArray(length)
        data.readFully(buffer)
        return String(buffer, Charsets.UTF_8)
    }
}
