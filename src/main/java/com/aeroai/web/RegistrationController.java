package com.aeroai.web;

import com.aeroai.model.RegisteredUser;
import com.aeroai.repository.RegisteredUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    private static final List<String> ROLES = List.of(
            "Student", "Researcher", "Healthcare Professional", "Engineer", "Other");
    private static final List<String> INTERESTS = List.of(
            "Respiratory Physiology", "Biomedical Engineering", "Computer Science",
            "Medical Research", "Mechanical Engineering", "Other");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    private final RegisteredUserRepository users;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public RegistrationController(RegisteredUserRepository users) {
        this.users = users;
    }

    @GetMapping
    public String registrationForm(Model model) {
        if (!model.containsAttribute("registration")) {
            model.addAttribute("registration", new RegistrationForm());
        }
        populateOptions(model);
        return "register";
    }

    @PostMapping
    public String register(@ModelAttribute("registration") RegistrationForm form,
                           Model model, RedirectAttributes redirectAttributes) {
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            populateOptions(model);
            return "register";
        }

        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalizedEmail)) {
            model.addAttribute("errors", Map.of("email", "An account with this email already exists."));
            populateOptions(model);
            return "register";
        }

        RegisteredUser user = new RegisteredUser(
                form.getFirstName().trim(), form.getLastName().trim(), normalizedEmail,
                passwordEncoder.encode(form.getPassword()), form.getRole(),
                blankToNull(form.getFieldOfInterest()));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // The unique database constraint also handles two registrations racing on the same email.
            model.addAttribute("errors", Map.of("email", "An account with this email already exists."));
            populateOptions(model);
            return "register";
        }

        redirectAttributes.addFlashAttribute("registrationSuccess",
                "Your AERO-AI demo account has been created. You can now continue to the login page.");
        return "redirect:/register/success";
    }

    @GetMapping("/success")
    public String registrationSuccess() {
        return "register-success";
    }

    private Map<String, String> validate(RegistrationForm form) {
        java.util.HashMap<String, String> errors = new java.util.HashMap<>();
        if (isBlank(form.getFirstName())) errors.put("firstName", "Enter your first name.");
        else if (form.getFirstName().trim().length() > 80) errors.put("firstName", "Use 80 characters or fewer.");
        if (isBlank(form.getLastName())) errors.put("lastName", "Enter your last name.");
        else if (form.getLastName().trim().length() > 80) errors.put("lastName", "Use 80 characters or fewer.");
        if (isBlank(form.getEmail()) || !EMAIL_PATTERN.matcher(form.getEmail().trim()).matches()) {
            errors.put("email", "Enter a valid email address.");
        } else if (form.getEmail().trim().length() > 254) {
            errors.put("email", "Email address is too long.");
        }
        if (form.getPassword() == null || form.getPassword().length() < 8) {
            errors.put("password", "Use at least 8 characters for your password.");
        } else if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.put("password", "Password must be no more than 72 UTF-8 bytes.");
        }
        if (form.getConfirmPassword() == null || !form.getConfirmPassword().equals(form.getPassword())) {
            errors.put("confirmPassword", "Passwords do not match.");
        }
        if (!ROLES.contains(form.getRole())) errors.put("role", "Select a valid role.");
        if (!isBlank(form.getFieldOfInterest()) && !INTERESTS.contains(form.getFieldOfInterest())) {
            errors.put("fieldOfInterest", "Select one of the listed fields of interest.");
        }
        if (!form.isDemoAcknowledged()) errors.put("demoAcknowledged", "Please confirm the research and education notice.");
        return errors;
    }

    private void populateOptions(Model model) {
        model.addAttribute("roles", ROLES);
        model.addAttribute("interests", INTERESTS);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    public static class RegistrationForm {
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private String confirmPassword;
        private String role;
        private String fieldOfInterest;
        private boolean demoAcknowledged;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getFieldOfInterest() { return fieldOfInterest; }
        public void setFieldOfInterest(String fieldOfInterest) { this.fieldOfInterest = fieldOfInterest; }
        public boolean isDemoAcknowledged() { return demoAcknowledged; }
        public void setDemoAcknowledged(boolean demoAcknowledged) { this.demoAcknowledged = demoAcknowledged; }
    }
}
