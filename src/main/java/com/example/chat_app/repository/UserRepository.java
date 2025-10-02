package com.example.chat_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.chat_app.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long>{
        Optional<User> findByEmail(String email);
        Optional<User> findByUsername(String username);
}
