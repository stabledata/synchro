package com.stabledata

import io.ably.lib.realtime.AblyRealtime

object Ably {
    private val ably = AblyRealtime(envString("ABLY_API_KEY"))
    fun publish(team: String, event: String, data: Any) {
        val channel = ably.channels.get(team)
        channel.publish(event, data)
    }
}