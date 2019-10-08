package ru.quandastudio.lpsserver.netty.core;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;

@Getter
@Setter
@RequiredArgsConstructor
public class Player {

	public static boolean USR_MSG_LOG = false;

	@NonNull
	private final Channel channel;

	private User user;

	private Boolean canReceiveMessages;

	private Boolean allowSendUID;

	private String clientVersion;

	private String avatarData;

	private Integer clientBuild;

	public void sendMessage(LPSMessage msg) {
		if (channel.isWritable()) {
			channel.writeAndFlush(msg);
		}
	}

	public void onMessage(LPSClientMessage msg) {
		// TODO Auto-generated method stub
	}

	public void onDisconnected() {
		// TODO Auto-generated method stub

	}
}
