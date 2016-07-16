package net.netcoding.nifty.chat.listeners;

import com.google.common.io.ByteArrayDataInput;
import net.netcoding.nifty.chat.cache.Config;
import net.netcoding.nifty.chat.commands.Message;
import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftHelper;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.plugin.messaging.ChannelListener;
import net.netcoding.nifty.core.util.DataUtil;
import net.netcoding.nifty.chat.cache.UserChatData;

public class MessageReceived extends MinecraftHelper implements ChannelListener {

	public MessageReceived(MinecraftPlugin plugin) {
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
			UserChatData senderData = UserChatData.getCache(Nifty.getMojangRepository().searchByUsername(fwData.readUTF())); // Sender
			UserChatData receiverData = UserChatData.getCache(Nifty.getMojangRepository().searchByUsername(fwData.readUTF())); // Receiver
			Message.notifySpies(this, receiverData.getProfile().getServer().getName(), senderData, receiverData, fwData.readUTF());
		}
	}

}