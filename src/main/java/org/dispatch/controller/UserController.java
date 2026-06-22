package org.dispatch.controller;

import org.dispatch.model.Role;
import org.dispatch.model.User;
import org.dispatch.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> data) {
        try {
            String username = (String) data.get("username");
            String password = (String) data.get("password");
            String fullName = (String) data.get("fullName");
            String roleStr = (String) data.get("role");

            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Пользователь с таким именем уже существует"));
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setRole(Role.valueOf(roleStr));
            user.setActive(true);

            User saved = userRepository.save(user);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            if (data.containsKey("fullName")) {
                String fullName = (String) data.get("fullName");
                if (fullName != null) {
                    user.setFullName(fullName);
                }
            }

            if (data.containsKey("role")) {
                String roleStr = (String) data.get("role");
                if (roleStr != null) {
                    user.setRole(Role.valueOf(roleStr));
                }
            }

            if (data.containsKey("active")) {
                Boolean active = (Boolean) data.get("active");
                if (active != null) {
                    user.setActive(active);
                }
            }

            if (data.containsKey("password") && data.get("password") != null && !((String) data.get("password")).isEmpty()) {
                user.setPassword(passwordEncoder.encode((String) data.get("password")));
            }

            User saved = userRepository.save(user);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}