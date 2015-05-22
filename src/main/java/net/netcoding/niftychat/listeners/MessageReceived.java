package net.netcoding.niftychat.listeners;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.messages.BungeeListener;
import net.netcoding.niftychat.cache.Config;
import net.netcoding.niftychat.cache.UserChatData;
import net.netcoding.niftychat.commands.Message;
import net.netcoding.niftycore.util.DataUtil;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteArrayDataInput;

public class MessageReceived extends BukkitHelper implements BungeeListener {

	public MessageReceived(JavaPlugin plugin) {
		super(plugin);
	}

	@Override
	public void onMessageReceived(String channel, byte[] message) throws Exception {
		ByteArrayDataInput input = DataUtil.newDataInput(message);
		String subChannel = input.readUTF();
		if (!subChannel.equals(Config.CHAT_CHANNEL)) return;
		byte[] data = new byte[input.readShort()];
		input.readFully(data);
		ByteArrayDataInput fwData = DataUtil.newDataInput(data);
		String action = fwData.readUTF();

		if (action.equals("Message")) {
			String senderName = fwData.readUTF();
			String receiverName = fwData.readUTF();
			String msg = fwData.readUTF();
			Message.send(this, senderName, receiverName, receiverName, msg);
		} else if (action.equals("SpyMessage")) {
			UserChatData senderData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByUsername(fwData.readUTF())); // Sender
			UserChatData receiverData = UserChatData.getCache(NiftyBukkit.getMojangRepository().searchByUsername(fwData.readUTF())); // Receiver
			Message.notifySpies(this, receiverData.getProfile().getServer().getName(), senderData, receiverData, fwData.readUTF());
		}
	}

}