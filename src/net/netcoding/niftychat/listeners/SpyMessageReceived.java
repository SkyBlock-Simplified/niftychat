package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.BungeeListener;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.commands.Message;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public class SpyMessageReceived extends BukkitHelper implements BungeeListener {

	public SpyMessageReceived(JavaPlugin plugin) {
		super(plugin);
	}

	@Override
	public void onMessageReceived(String channel, Player player, byte[] message) throws Exception {
		ByteArrayDataInput input = ByteStreams.newDataInput(message);
		String subChannel = input.readUTF();
		if (!subChannel.equals(Config.CHAT_CHANNEL)) return;
		byte[] data = new byte[input.readShort()];
		input.readFully(data);
		ByteArrayDataInput fwData = ByteStreams.newDataInput(data);
		String action = fwData.readUTF();

		if (action.equals("SpyMessage")) {
			String senderName = fwData.readUTF();
			String receiverName = fwData.readUTF();
			String spyName = fwData.readUTF();
			String msg = fwData.readUTF();
			Message.send(this, senderName, receiverName, spyName, msg);
		}
	}

}