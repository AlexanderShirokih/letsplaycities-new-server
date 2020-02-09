package ru.quandastudio.lpsserver.data;

import java.util.List;
import java.util.Optional;

import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

public interface PictureManager {

	public List<Picture> getPicturesByUserId(List<User> users);
	
	public Optional<Picture> getPictureByUserId(User userId);
	
	public void deletePictureByUser(User user);

	public void addPicture(String imageData, User owner);
}
