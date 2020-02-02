package ru.quandastudio.lpsserver.data;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.data.dao.PictureRepository;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

@Service
@Transactional
public class PictureManagerImpl implements PictureManager {

	@Autowired
	private PictureRepository picturesDAO;

	@Override
	public List<Picture> getPicturesByUserId(List<User> users) {
		return picturesDAO.findByOwnerIn(users);
	}

	@Override
	public Optional<Picture> getPictureByUserId(User user) {
		return picturesDAO.findByOwner(user);
	}

}
