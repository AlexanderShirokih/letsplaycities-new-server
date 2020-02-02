package ru.quandastudio.lpsserver.data.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;

@Repository
public interface PictureRepository extends JpaRepository<Picture, Long> {
	@SuppressWarnings("unchecked")
	public Picture save(Picture picture);

	@Modifying
	@Query("update Picture p set p.imageData = :image where p.owner = :owner")
	public void updateByOwner(@Param("owner") User owner, @Param("image") byte[] image);

	@Modifying
	public void deleteByOwner(User owner);

	public Optional<Picture> findByOwner(User owner);

	public List<Picture> findByOwnerIn(List<User> owners);

}
