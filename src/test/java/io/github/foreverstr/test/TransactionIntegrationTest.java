package io.github.foreverstr.test;

import io.github.foreverstr.entity.User;
import io.github.foreverstr.test.service.TransactionalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionalService transactionalService;

    @Test
    @Transactional
    void testJormOperationInSpringTransaction() {
        User user = new User("TransactionUser", 30, "active");

        transactionalService.saveUser(user);
        assertNotNull(user.getId());

        User found = transactionalService.findUser(user.getId());
        assertNotNull(found);
        assertEquals("TransactionUser", found.getName());
    }

    @Test
    @Transactional
    void testRollbackInSpringTransaction() {
        User user = new User("RollbackUser", 25, "active");

        assertThrows(RuntimeException.class, () -> {
            transactionalService.saveAndRollback(user);
        });

        // 强制 flush 事务
        transactionalService.findUser(user.getId()); // 应该返回 null
    }
}