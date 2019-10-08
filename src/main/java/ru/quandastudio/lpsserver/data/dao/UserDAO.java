package ru.quandastudio.lpsserver.data.dao;

import java.util.Optional;

import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.netty.models.AuthType;

public interface UserDAO {

	public void addUser(User user);

	public Optional<User> getUserById(Integer userId, String accessHash);

	public Optional<User> getUserBySnUID(String snUID, AuthType authType);

	public void update(User user);

}
