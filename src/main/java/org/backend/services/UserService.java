package org.backend.services;

import jakarta.transaction.Transactional;
import org.backend.models.Cart;
import org.backend.models.User;
import org.backend.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getByUsername(String username) {
        Optional<User> person_db = userRepo.getByUsername(username);
        return person_db.orElse(null);
    }

    @Transactional
    public void register(User user) {
        if (user.getLastName() == null)
            user.setLastName("");

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_CUSTOMER");
        userRepo.save(user);
    }
}
