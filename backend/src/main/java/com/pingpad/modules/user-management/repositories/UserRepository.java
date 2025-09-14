package com.pingpad.modules.user_management.repositories;

import com.pingpad.modules.user_management.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGithubLogin(String login);
}
