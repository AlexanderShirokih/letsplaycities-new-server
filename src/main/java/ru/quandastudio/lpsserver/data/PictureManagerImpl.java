package ru.quandastudio.lpsserver.data;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.quandastudio.lpsserver.data.dao.PictureRepository;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PictureManagerImpl implements PictureManager {

	private final PictureRepository picturesDAO;

	@Override
	public Optional<Picture> getPictureByUserId(User user) {
		return picturesDAO.findByOwner(user);
	}

	@Override
	public void deletePictureByUser(User user) {
		picturesDAO.deleteByOwner(user);
	}
	
	@Override
	public void save(Picture picture) {
		picturesDAO.save(picture);
	}

}
