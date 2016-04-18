package net.netcoding.niftychat.events;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.util.StringUtil;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collection;
import java.util.Collections;

public class PlayerJsonChatEvent extends Event implements Cancellable {

	private static final transient HandlerList handlers = new HandlerList();
	private final Collection<BukkitMojangProfile> recipients = NiftyBukkit.getBungeeHelper().getPlayerList();
	private final BukkitMojangProfile profile;
	private boolean cancelled = false;
	private final String originalMessage;
	private String message;

	public PlayerJsonChatEvent(BukkitMojangProfile profile, String message) {
		this.profile = profile;
		this.originalMessage = StringUtil.isEmpty(message) ? "" : message;
		this.message = this.originalMessage;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * Gets the message from the sender.
	 *
	 * @return Message from the sender.
	 */
	public String getMessage() { // TODO: JSON
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
	public BukkitMojangProfile getProfile() {
		return this.profile;
	}

	/**
	 * Gets all the profiles receiving the message.
	 *
	 * @return All profiles to receive message.
	 */
	public Collection<BukkitMojangProfile> getRecipients() {
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
	 * Prevents the event from occurring.
	 *
	 * @param cancelled True to cancel, otherwise false.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * Modifies the message from the sender.
	 *
	 * @param message New message to use.
	 */
	public void setMessage(String message) { // TODO: JSON
		this.message = message;
	}

}