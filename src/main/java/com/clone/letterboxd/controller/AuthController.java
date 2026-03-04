package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.UserRegistrationDto;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.service.EmailService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;


    public AuthController(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.emailService = emailService;
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
            if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                model.addAttribute("error", "Username and password are required");
                return "login";
            }

            Optional<User> userOptional = userRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }

            User user = userOptional.get();
            
            log.debug("Login attempt for user: {}", username);
            log.debug("Stored hash length: {}", user.getPassword() != null ? user.getPassword().length() : 0);
            log.debug("Password match result: {}", passwordEncoder.matches(password, user.getPassword()));
            
            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.warn("Password mismatch for user: {}", username);
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }
            
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getId());
            
            
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

    // --- password recovery flows ---

    @GetMapping("/forgot")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot")
    public String handleForgotPassword(
            @RequestParam("email") String email,
            Model model,
            RedirectAttributes redirectAttributes) {
        String message = "If that email address is registered, an OTP has been sent.";
        try {
            Optional<User> opt = userRepository.findByEmail(email);
            if (opt.isPresent()) {
                User user = opt.get();
                // Generate 6-digit OTP
                String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
                user.setResetToken(otp);
                user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
                userRepository.save(user);

                emailService.sendOtpEmail(email, otp);
                
                redirectAttributes.addFlashAttribute("email", email);
                return "redirect:/auth/verify-otp";
            }
        } catch (Exception e) {
            log.warn("forgot password processing failed", e);
        }
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("message", "Please enter your email to continue.");
            return "redirect:/auth/forgot";
        }
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/auth/forgot";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam(value = "email", required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/auth/forgot";
        }
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty() || !otp.equals(opt.get().getResetToken()) || 
            opt.get().getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Invalid or expired OTP.");
            model.addAttribute("email", email);
            return "verify-otp";
        }
        
        // Success - redirect to password reset page with the OTP as a valid identifier
        return "redirect:/auth/reset?token=" + otp + "&email=" + email;
    }

    @GetMapping("/reset")
    public String resetPasswordPage(
            @RequestParam("token") String token, 
            @RequestParam(value = "email", required = false) String email,
            Model model) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty() || !token.equals(opt.get().getResetToken()) ||
                opt.get().getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Invalid session or token. Please start over.");
            return "forgot-password";
        }
        model.addAttribute("token", token);
        model.addAttribute("email", email);
        return "reset-password";
    }

    @PostMapping("/reset")
    public String handleResetPassword(
            @RequestParam("token") String token,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty() || !token.equals(opt.get().getResetToken())) {
            model.addAttribute("error", "Invalid session or token.");
            model.addAttribute("token", token);
            model.addAttribute("email", email);
            return "reset-password";
        }
        
        User user = opt.get();
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Reset token has expired.");
            return "reset-password";
        }
        
        if (password == null || password.trim().isEmpty() || password.length() < 8 ||
                !password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords must match and be at least 8 characters.");
            model.addAttribute("token", token);
            model.addAttribute("email", email);
            return "reset-password";
        }
        
        String encodedPassword = passwordEncoder.encode(password);
        log.info("Password reset for user: {}", user.getUsername());
        
        user.setPassword(encodedPassword);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Password updated. You can now login.");
        return "redirect:/auth/login";
    }

    @PostMapping("/register")
    public String handleRegister(
            UserRegistrationDto userRegistration, 
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            if (userRegistration.getUsername() == null || userRegistration.getUsername().trim().isEmpty() ||
                userRegistration.getEmail() == null || userRegistration.getEmail().trim().isEmpty() ||
                userRegistration.getPassword() == null || userRegistration.getPassword().trim().isEmpty()) {
                model.addAttribute("error", "All required fields must be filled");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            if (userRegistration.getPassword().length() < 8) {
                model.addAttribute("error", "Password must be at least 8 characters");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            if (!userRegistration.getPassword().equals(userRegistration.getConfirmPassword())) {
                model.addAttribute("error", "Passwords do not match");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            if (userRepository.findByUsername(userRegistration.getUsername()).isPresent()) {
                model.addAttribute("error", "Username already exists");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            if (userRepository.findByEmail(userRegistration.getEmail()).isPresent()) {
                model.addAttribute("error", "Email already exists");
                model.addAttribute("userRegistration", userRegistration);
                return "register";
            }

            User newUser = new User();
            newUser.setUsername(userRegistration.getUsername());
            newUser.setEmail(userRegistration.getEmail());
            newUser.setDisplayName(userRegistration.getDisplayName());
            newUser.setBio(userRegistration.getBio());
            
            if (userRegistration.getAvatarFile() != null && !userRegistration.getAvatarFile().isEmpty()) {
                newUser.setAvatarBytes(userRegistration.getAvatarFile().getBytes());
                newUser.setAvatarContentType(userRegistration.getAvatarFile().getContentType());
            }
            
            newUser.setPassword(passwordEncoder.encode(userRegistration.getPassword()));
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            // if no other users exist, make this account the administrator
            if (userRepository.count() == 0) {
                newUser.setIsAdmin(true);
            }
            userRepository.save(newUser);
            
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
        session.invalidate();
        return "redirect:/";
    }
}
