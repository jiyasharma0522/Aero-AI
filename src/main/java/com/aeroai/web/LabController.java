package com.aeroai.web;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/lab")
public class LabController {
    private static final Map<String, String[]> MODULES = Map.of(
        "anatomy", new String[]{"Respiratory anatomy", "Explore the structures that move air and support breathing.", "Select an anatomical structure to read its role."},
        "gas-exchange", new String[]{"Alveolar gas exchange", "See how oxygen and carbon dioxide diffuse across the alveolar-capillary interface.", "Conceptual particles illustrate diffusion; they are not measurements."},
        "circulation", new String[]{"Heart and circulation", "Follow the modeled route between lungs, heart, and body tissues.", "Pathway colors show a teaching model of oxygen-rich and oxygen-poor blood."},
        "analytics", new String[]{"Respiratory analytics", "Review stored readings and deterministic educational thresholds.", "Alerts describe the entered sample only; they do not diagnose disease."},
        "device", new String[]{"Virtual respiratory device lab", "Adjust a virtual airflow model and observe its calculated output.", "This software model does not connect to or control medical equipment."},
        "integration", new String[]{"Human-machine integration", "Connect the virtual airway model to the virtual device model.", "Connection status exists only inside this educational simulation."},
        "experiments", new String[]{"Digital twin experiment lab", "Change model parameters, run scenarios, and save results to the local H2 database.", "This simplified educational twin is not clinically validated."}
    );
    @GetMapping("/{module}")
    public String module(@PathVariable String module, Model model) {
        String[] details = MODULES.get(module);
        if (details == null) return "redirect:/dashboard";
        model.addAttribute("moduleId", module);
        model.addAttribute("activeModule", module);
        model.addAttribute("moduleTitle", details[0]);
        model.addAttribute("appPageTitle", details[0]);
        model.addAttribute("moduleDescription", details[1]);
        model.addAttribute("moduleNote", details[2]);
        return "lab";
    }
}
