package com.example.chat_app.service;

import com.example.chat_app.model.User;
import com.example.chat_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public User registerUser(String username,String email,String password){
        if(userRepository.findByEmail(email).isPresent()){
            throw new RuntimeException("Email already exists");
        }
        if(userRepository.findByUsername(username).isPresent()){
            throw new RuntimeException("Username already exists");
        }
        User user=User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .build();
        return userRepository.save(user);
    }

    public User authenticateUser(String email,String password){
        Optional<User> userOptional=userRepository.findByEmail(email);
        if(userOptional.isEmpty()) throw new RuntimeException("Invalid credentials");
        User user=userOptional.get();
        if(!passwordEncoder.matches(password,user.getPasswordHash())){
            throw new RuntimeException("Invalid Credentials");
        }
        return user;
    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    public Optional<User> getUserByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public void deleteUserById(Long id){
        userRepository.deleteById(id);
    }

    public void deleteAllUsers(){
        userRepository.deleteAll();
    }
}
