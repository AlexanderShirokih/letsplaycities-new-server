package ru.quandastudio.lpsserver.data.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

import java.util.Optional;

@Repository
public interface PictureRepository extends JpaRepository<Picture, Long> {

	Picture save(Picture p);

	@Modifying
	@Query("update Picture p set p.imageData = :image where p.owner = :owner")
	void updateByOwner(@Param("owner") User owner, @Param("image") byte[] image);

	@Modifying
	void deleteByOwnerId(Integer owner);

	Optional<Picture> findByOwner(User owner);

}
