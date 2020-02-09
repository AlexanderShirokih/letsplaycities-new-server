package ru.quandastudio.lpsserver.handlers;

import java.util.Objects;
import java.util.Optional;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.PictureManager;
import ru.quandastudio.lpsserver.data.UserManager;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSAvatar;

public class AvatarMessageHandler extends MessageHandler<LPSAvatar> {

	public AvatarMessageHandler() {
		super(LPSAvatar.class);
	}

	@Override
	public void handle(Player player, LPSAvatar msg) {
		final ServerContext context = player.getCurrentContext();

		final UserManager users = context.getUserManager();
		final PictureManager pics = context.getPictureManager();
		final User user = player.getUser();

		switch (msg.getType()) {
		case DELETE:// Delete avatar
			pics.deletePictureByUser(user);
			users.updateHash(user, null);
			break;
		case SEND:// Save or update avatar
			updateAvatar(user, users, pics, msg);
			break;
		default:
			break;
		}

	}

	private void updateAvatar(User user, UserManager users, PictureManager pics, LPSAvatar msg) {
		if (!Objects.equals(msg.getHash(), user.getAvatarHash())) {
			Optional<Picture> pic = pics.getPictureByUserId(user);
			pic.ifPresentOrElse((Picture p) -> {
				p.setImageData(msg.getAvatar().getBytes());
			}, () -> {
				pics.addPicture(msg.getAvatar(), user);
			});
			users.updateHash(user, msg.getHash());
		}

	}
}
