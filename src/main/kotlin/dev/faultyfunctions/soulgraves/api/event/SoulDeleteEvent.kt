package dev.faultyfunctions.soulgraves.api.event

import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


/**
 * Remove Reasons:
 * 1. Soul Pickup,
 * 2. Soul Explode,
 * 3. API Delete,
 * 4. Valid Check (e.g. Marker Entity Removed, World Unload)
 * 5. Clear Not Valid Soul At Server Start
 *
 * The event cannot be cancelled, it is only for notification
 * If you need to modify the soul, please listen to other events
 * When the event is triggered, the database has sent/completed the deletion request.
 */
class SoulDeleteEvent(val soul: Soul): Event() {

    companion object {
        val HANDLER_LIST = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLER_LIST
        }
    }

    override fun getHandlers(): HandlerList {
        return SoulSpawnEvent.HANDLER_LIST
    }

}