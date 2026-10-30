package com.aeroai.web;

import com.aeroai.model.RegisteredUser;
import com.aeroai.repository.RegisteredUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    private final RegisteredUserRepository users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LoginController(RegisteredUserRepository users) { this.users = users; }

    @PostMapping("/login")
    public String authenticate(@RequestParam String email, @RequestParam String password,
                               HttpServletRequest request, Model model) {
        RegisteredUser user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            model.addAttribute("loginError", "Email or password was not recognized.");
            return "login";
        }
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute("aeroUser", user.getId());
        session.setAttribute("aeroName", user.getFirstName());
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/login?signout";
    }
}
