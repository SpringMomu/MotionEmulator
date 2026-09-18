package com.zhufucdev.motion_emulator.ui.map

import com.zhufucdev.me.stub.Point

/** Snapshots retain stroke boundaries, including an undoable clear operation. */
internal class TraceStroke {
    private val current = mutableListOf<Point>()
    private val history = mutableListOf<List<Point>>()
    val points: List<Point> get() = current.toList()
    fun markBegin() { history.add(current.toList()) }
    fun add(point: Point) { current.add(point) }
    fun undo() {
        val previous = history.removeLastOrNull() ?: return
        current.clear()
        current.addAll(previous)
    }
    fun clear() {
        if (current.isEmpty()) return
        markBegin()
        current.clear()
    }
}
