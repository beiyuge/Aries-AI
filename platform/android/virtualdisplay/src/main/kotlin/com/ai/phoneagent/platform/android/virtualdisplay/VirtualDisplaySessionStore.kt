package com.ai.phoneagent.platform.android.virtualdisplay

import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.HandlerThread
import java.util.concurrent.ConcurrentHashMap

internal class ActiveVirtualDisplaySession(
    val descriptor: AndroidVirtualDisplaySession,
    val virtualDisplay: VirtualDisplay,
    val imageReader: ImageReader,
    val frameBuffer: VirtualDisplayFrameBuffer,
    private val frameThread: HandlerThread,
) {
    fun release() {
        imageReader.setOnImageAvailableListener(null, null)
        virtualDisplay.release()
        imageReader.close()
        frameBuffer.clear()
        frameThread.quitSafely()
    }
}

internal interface VirtualDisplaySessionStore {
    fun put(session: ActiveVirtualDisplaySession)
    fun get(sessionId: String): ActiveVirtualDisplaySession?
    fun remove(sessionId: String): ActiveVirtualDisplaySession?
}

internal class InMemoryVirtualDisplaySessionStore : VirtualDisplaySessionStore {
    private val sessions = ConcurrentHashMap<String, ActiveVirtualDisplaySession>()

    override fun put(session: ActiveVirtualDisplaySession) {
        sessions[session.descriptor.sessionId] = session
    }

    override fun get(sessionId: String): ActiveVirtualDisplaySession? = sessions[sessionId]

    override fun remove(sessionId: String): ActiveVirtualDisplaySession? = sessions.remove(sessionId)
}
