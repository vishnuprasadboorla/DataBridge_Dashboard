package com.example.us1demo.controller;

import com.example.us1demo.entity.Users;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:63342")
@RequestMapping("/api")
public class Task_on_login {

    // In-memory database (to store user details with password and signup timestamp)
    private Map<String, UserDetails> userDatabase = new HashMap<>();

    // In-memory database for login statuses
    private Map<String, LoginDetails> loginStatusDatabase = new HashMap<>();

    // Model to store user details
    static class UserDetails {
        private String password;
        private LocalDateTime signupTimestamp;

        public UserDetails(String password, LocalDateTime signupTimestamp) {
            this.password = password;
            this.signupTimestamp = signupTimestamp;
        }

        public String getPassword() {
            return password;
        }

        public LocalDateTime getSignupTimestamp() {
            return signupTimestamp;
        }
    }

    // Model to store login status details
    static class LoginDetails {
        private String password; // Added password field
        private LocalDateTime loginTimestamp;

        public LoginDetails(String password, LocalDateTime loginTimestamp) {
            this.password = password;
            this.loginTimestamp = loginTimestamp;
        }

        public String getPassword() {
            return password;
        }

        public LocalDateTime getLoginTimestamp() {
            return loginTimestamp;
        }
    }

    // POST endpoint for user signup
    @PostMapping("/signup")
    public String signup(@RequestBody Users user) {
        if (userDatabase.containsKey(user.getUsername())) {
            return "Username already exists. Please choose another username.";
        }
        // Store password and current timestamp
        userDatabase.put(user.getUsername(), new UserDetails(user.getPassword(), LocalDateTime.now()));
        return "User signed up successfully!";
    }

    // POST endpoint for user login
    @PostMapping("/login")
    public String login(@RequestBody Users user) {
        UserDetails userDetails = userDatabase.get(user.getUsername());
        if (userDetails != null && userDetails.getPassword().equals(user.getPassword())) {
            // Add login details to the loginStatusDatabase
            loginStatusDatabase.put(user.getUsername(), new LoginDetails(user.getPassword(), LocalDateTime.now()));
            return "Login successful!";
        }
        return "Invalid username or password.";
    }

    // GET method to retrieve password and timestamp for a specific user
    @GetMapping("/getUserDetails/{username}")
    public String getUserDetails(@PathVariable String username) {
        UserDetails userDetails = userDatabase.get(username);
        if (userDetails != null) {
            return "User: " + username + ", Password: " + userDetails.getPassword() +
                    ", Signup Timestamp: " + userDetails.getSignupTimestamp();
        }
        return "Username not found!";
    }

    // GET method to retrieve all users and their details
    @GetMapping("/getAllUsers")
    public Map<String, Map<String, String>> getAllUsers() {
        Map<String, Map<String, String>> allUsers = new HashMap<>();
        for (Map.Entry<String, UserDetails> entry : userDatabase.entrySet()) {
            Map<String, String> details = new HashMap<>();
            details.put("password", entry.getValue().getPassword());
            details.put("signupTimestamp", entry.getValue().getSignupTimestamp().toString());
            allUsers.put(entry.getKey(), details);
        }
        return allUsers;
    }

    // GET method to retrieve all login statuses
    @GetMapping("/getAllLoginStatuses")
    public Map<String, Map<String, String>> getAllLoginStatuses() {
        Map<String, Map<String, String>> allLoginStatuses = new HashMap<>();
        for (Map.Entry<String, LoginDetails> entry : loginStatusDatabase.entrySet()) {
            Map<String, String> details = new HashMap<>();
            details.put("password", entry.getValue().getPassword());
            details.put("loginTimestamp", entry.getValue().getLoginTimestamp().toString());
            allLoginStatuses.put(entry.getKey(), details);
        }
        return allLoginStatuses;
    }
}
