package io.github.foreverstr.test.service;

import io.github.foreverstr.entity.User;
import io.github.foreverstr.session.FindSession;
import io.github.foreverstr.session.SaveSession;
import io.github.foreverstr.session.UpdateSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalService {

    @Transactional
    public void saveUser(User user) {
        try (SaveSession session = new SaveSession()) {
            session.save(user);
        }
    }

    @Transactional
    public User findUser(Long id) {
        try (FindSession session = new FindSession()) {
            return session.Where("id", id).Find(User.class).stream().findFirst().orElse(null);
        }
    }

    @Transactional
    public void updateUserAge(Long id, int newAge) {
        try (UpdateSession session = new UpdateSession()) {
            session.Model(User.class)
                    .Where("id", id)
                    .Set("age", newAge)
                    .Update();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAndRollback(User user) {
        saveUser(user);
        throw new RuntimeException("Test rollback");
    }
}