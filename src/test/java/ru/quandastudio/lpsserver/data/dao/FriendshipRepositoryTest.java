package ru.quandastudio.lpsserver.data.dao;

import static org.junit.Assert.assertEquals;

import javax.transaction.Transactional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
public class FriendshipRepositoryTest {

	@Autowired
	private FriendshipRepository friendship;

	@Test
	@Transactional
	@Sql(scripts = { "/test-friendship-data.sql" })
	public void test() {
		assertEquals(3, friendship.count());
	}

}
