package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.UserRegistrationDto;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            // Validate inputs
            if (username == null || username.trim().isEmpty() || 
                password == null || password.trim().isEmpty()) {
                model.addAttribute("error", "Username and password are required");
                return "login";
            }

            // Check if user exists in database
            Optional<User> userOptional = userRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }

            User user = userOptional.get();
            
            // Verify password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }
            
            // Store user in session
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getId());
            
            // Redirect to home page after successful login
            return "redirect:/";
            
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred during login: " + e.getMessage());
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userRegistration", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(
            UserRegistrationDto userRegistration, 
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            // Validate inputs
            if (userRegistration.getUsername() == null || userRegistration.getUsername().trim().isEmpty() ||
                userRegistration.getEmail() == null || userRegistration.getEmail().trim().isEmpty() ||
                userRegistration.getPassword() == null || userRegistration.getPassword().trim().isEmpty()) {
                model.addAttribute("error", "All required fields must be filled");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            // Check if passwords match
            if (!userRegistration.getPassword().equals(userRegistration.getConfirmPassword())) {
                model.addAttribute("error", "Passwords do not match");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            // Check if username already exists
            if (userRepository.findByUsername(userRegistration.getUsername()).isPresent()) {
                model.addAttribute("error", "Username already exists");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            // Check if email already exists
            if (userRepository.findByEmail(userRegistration.getEmail()).isPresent()) {
                model.addAttribute("error", "Email already exists");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            // Create new user
            User newUser = new User();
            newUser.setUsername(userRegistration.getUsername());
            newUser.setEmail(userRegistration.getEmail());
            newUser.setDisplayName(userRegistration.getDisplayName());
            newUser.setBio(userRegistration.getBio());
            
            // Hash password and save
            newUser.setPassword(passwordEncoder.encode(userRegistration.getPassword()));
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(newUser);
            
            // Redirect to login with success message
            redirectAttributes.addAttribute("success", true);
            return "redirect:/auth/login";
            
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred during registration: " + e.getMessage());
            model.addAttribute("userRegistration", userRegistration);
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Clear user session
        session.invalidate();
        return "redirect:/";
    }
}
