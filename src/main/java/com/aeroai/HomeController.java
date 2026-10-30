
package com.aeroai;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("firstName", session.getAttribute("aeroName"));
        model.addAttribute("activeModule", "dashboard");
        model.addAttribute("appPageTitle", "Dashboard");
        return "dashboard";
    }
    @GetMapping("/login")
    public String login() { return "login"; }
    @GetMapping("/api/status")
@ResponseBody
public Map<String, Object> status() {
    Map<String, Object> data = new HashMap<>();
    data.put("spo2", 98);
    data.put("breathingRate", 16);
    data.put("pulse", 72);
    data.put("mode", "SIMULATED");
    return data;
}
}
