package net.netcoding.nifty.chat.events;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.minecraft.event.Cancellable;
import net.netcoding.nifty.common.minecraft.event.player.PlayerEvent;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentList;
import net.netcoding.nifty.core.util.concurrent.ConcurrentMap;
import net.netcoding.nifty.core.util.json.JsonMessage;

import java.util.Collection;
import java.util.Collections;

public class PlayerJsonChatEvent implements PlayerEvent, Cancellable {

	private final Collection<MinecraftMojangProfile> recipients = Nifty.getBungeeHelper().getPlayerList();
	private final ConcurrentMap<String, ConcurrentList<JsonMessage>> format = Concurrent.newMap();
	private final MinecraftMojangProfile profile;
	private boolean cancelled = false;
	private final String originalMessage;
	private JsonMessage message;

	public PlayerJsonChatEvent(MinecraftMojangProfile profile, String message) {
		this.profile = profile;
		this.originalMessage = StringUtil.isEmpty(message) ? "" : message;
		this.message = new JsonMessage(this.originalMessage);
	}

	/**
	 * Gets a grouped format.
	 *
	 * @return Json format from the sender.
	 */
	public ConcurrentList<JsonMessage> getFormat(String group) {
		return this.format.get(group);
	}

	/**
	 * Gets the json message from the sender.
	 *
	 * @return Json message from the sender.
	 */
	public JsonMessage getMessage() { // TODO: JSON
		return this.message;
	}

	/**
	 * Gets the original message from the sender.
	 *
	 * @return Original message from the sender.
	 */
	public String getOriginalMessage() { // TODO: JSON
		return this.originalMessage;
	}

	/**
	 * Gets the sender of the message.
	 *
	 * @return Profile of the sender.
	 */
	@Override
	public MinecraftMojangProfile getProfile() {
		return this.profile;
	}

	/**
	 * Gets all the profiles receiving the message.
	 *
	 * @return All profiles to receive message.
	 */
	public Collection<MinecraftMojangProfile> getRecipients() {
		return Collections.unmodifiableCollection(this.recipients);
	}

	/**
	 * Gets if the event is cancelled.
	 *
	 * @return True if cancelled, otherwise false.
	 */
	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	/**
	 * Adds a new grouped format.
	 *
	 * @param group Group to add.
	 * @param format Format to add.
	 */
	public void putFormat(String group, ConcurrentList<JsonMessage> format) {
		this.format.put(group, format);
	}

	/**
	 * Removes a grouped format.
	 *
	 * @param group Format grouping to remove.
	 */
	public void removeFormat(String group) {
		this.format.remove(group);
	}

	/**
	 * Prevents the event from occurring.
	 *
	 * @param cancelled True to cancel, otherwise false.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}