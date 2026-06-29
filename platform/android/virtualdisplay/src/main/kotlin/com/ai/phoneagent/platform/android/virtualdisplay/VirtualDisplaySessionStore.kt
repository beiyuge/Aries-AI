package com.ai.phoneagent.platform.android.virtualdisplay

import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import java.util.concurrent.ConcurrentHashMap

class ActiveVirtualDisplaySession(
    val descriptor: AndroidVirtualDisplaySession,
    val virtualDisplay: VirtualDisplay,
    val imageReader: ImageReader,
) {
    fun release() {
        virtualDisplay.release()
        imageReader.close()
    }
}

interface VirtualDisplaySessionStore {
    fun put(session: ActiveVirtualDisplaySession)
    fun get(sessionId: String): ActiveVirtualDisplaySession?
    fun remove(sessionId: String): ActiveVirtualDisplaySession?
}

class InMemoryVirtualDisplaySessionStore : VirtualDisplaySessionStore {
    private val sessions = ConcurrentHashMap<String, ActiveVirtualDisplaySession>()

    override fun put(session: ActiveVirtualDisplaySession) {
        sessions[session.descriptor.sessionId] = session
    }

    override fun get(sessionId: String): ActiveVirtualDisplaySession? = sessions[sessionId]

    override fun remove(sessionId: String): ActiveVirtualDisplaySession? = sessions.remove(sessionId)
}
