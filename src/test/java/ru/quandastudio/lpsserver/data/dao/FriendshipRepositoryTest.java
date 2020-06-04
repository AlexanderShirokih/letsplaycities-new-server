package ru.quandastudio.lpsserver.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.quandastudio.lpsserver.LpsServerApplication;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;
import ru.quandastudio.lpsserver.data.entities.User;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {LpsServerApplication.class}, webEnvironment = WebEnvironment.MOCK)
@WebAppConfiguration
@Sql({"/create_tables.sql", "/init_data.sql"})
@Transactional
public class FriendshipRepositoryTest {

    @Autowired
    private FriendshipRepository rep;

    @Test
    public void testEntitySaves() {
        User a = new User(1);
        User b = new User(2);

        Friendship f = new Friendship(a, b);
        rep.save(f);

        Friendship found = rep.findBySenderAndReceiverOrReceiverAndSender(a, b).orElse(null);

        assertNotNull(found);
        assertEquals(f.getSender().getId(), found.getSender().getId());
    }

    @Test
    public void testFindAllFriendsByUser() {
        List<FriendshipProjection> friends = rep.findAllFriendsByUser(new User(3));
        assertThat(friends).hasSize(3).allMatch((FriendshipProjection fd) -> fd.getUserId() != 3);
    }

}
