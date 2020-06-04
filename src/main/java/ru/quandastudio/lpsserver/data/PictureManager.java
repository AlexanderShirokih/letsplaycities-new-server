package ru.quandastudio.lpsserver.data;

import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

import java.util.Optional;

public interface PictureManager {

	Optional<Picture> getPictureByUserId(User userId);

	void deletePictureByUser(User user);

	void save(Picture picture);
}
