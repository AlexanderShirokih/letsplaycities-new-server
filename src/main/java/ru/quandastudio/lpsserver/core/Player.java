package ru.quandastudio.lpsserver.core;

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

	@NonNull
	private final ServerContext currentContext;

	@NonNull
	private final MessageChannel channel;

	@NonNull
	private final MessageRouter messageHandler;

	private User user;

	private Boolean canReceiveMessages;

	private Boolean allowSendUID;

	private String clientVersion;

	private String avatarData;

	private Integer clientBuild;

	private transient boolean isOnline;

	private Room room;

	public void sendMessage(LPSMessage msg) {
		channel.send(msg);
	}

	public void onMessage(LPSClientMessage msg) {
		messageHandler.handleMessage(this, msg);
	}

	public void onDisconnected() {
		// TODO Auto-generated method stub

	}

	public boolean checkVersion(int version) {
		return clientBuild >= version;
	}
}
