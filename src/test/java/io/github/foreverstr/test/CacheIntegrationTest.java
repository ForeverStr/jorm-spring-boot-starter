package io.github.foreverstr.test;

import io.github.foreverstr.entity.User;
import io.github.foreverstr.session.FindSession;
import io.github.foreverstr.session.factory.Jorm;
import io.github.foreverstr.test.service.TransactionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class CacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TransactionalService transactionalService;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushAll();
    }

    @Test
    @Transactional
    void testQueryResultCached() {
        User user = new User("CachedUser", 40, "active");
        transactionalService.saveUser(user);

        FindSession session1 = new FindSession();
        List<User> result1 = session1.Where("user_name", "CachedUser").Find(User.class);
        assertEquals(1, result1.size());

        FindSession session2 = new FindSession();
        List<User> result2 = session2.Where("user_name", "CachedUser").Find(User.class);
        assertEquals(1, result2.size());

        assertFalse(Objects.requireNonNull(redisTemplate.keys("*")).isEmpty());
    }

    @Test
    @Transactional
    void testCacheEvictionOnUpdate() {
        User user = new User("ToUpdateUser", 50, "active");
        transactionalService.saveUser(user);

        FindSession findSession = new FindSession();
        findSession.Where("user_name", "ToUpdateUser").Find(User.class);

        transactionalService.updateUserAge(user.getId(), 60);

        List<User> result = findSession.Where("user_name", "ToUpdateUser").Find(User.class);
        assertEquals(60, result.get(0).getAge());
    }
}