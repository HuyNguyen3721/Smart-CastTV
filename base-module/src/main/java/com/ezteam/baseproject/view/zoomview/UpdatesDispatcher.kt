package com.ezteam.baseproject.view.zoomview

/**
 * Holds [ZoomEngine.Listener] and dispatches updates to them
 * when [dispatchOnIdle] or [dispatchOnMatrix] are called.
 *
 * It asks for a new matrix at each listener update which is important
 * so they don't mess each other.
 */
internal class UpdatesDispatcher(private val engine: ZoomEngine) {

    private val listeners = mutableListOf<ZoomEngine.Listener>()

    /**
     * Dispatches [ZoomEngine.Listener.onUpdate] updates.
     */
    internal fun dispatchOnMatrix() {
        listeners.forEach {
            it.onUpdate(engine, engine.matrix)
        }
    }

    /**
     * Dispatches [ZoomEngine.Listener.onIdle] updates.
     */
    internal fun dispatchOnIdle() {
        listeners.forEach {
            it.onIdle(engine)
        }
    }

    /**
     * Registers a new [Listener] to be notified of matrix updates.
     * @param listener the new listener
     */
    internal fun addListener(listener: ZoomEngine.Listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /**
     * Removes a previously registered listener.
     * @param listener the listener to be removed
     */
    internal fun removeListener(listener: ZoomEngine.Listener) {
        listeners.remove(listener)
    }

}