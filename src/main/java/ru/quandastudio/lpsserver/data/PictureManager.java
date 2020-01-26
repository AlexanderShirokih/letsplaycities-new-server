package ru.quandastudio.lpsserver.data;

import java.util.List;

import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

public interface PictureManager {

	public List<Picture> getPicturesByUserId(List<User> users);
	
}
