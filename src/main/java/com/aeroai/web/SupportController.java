package com.aeroai.web;

import com.aeroai.model.RegisteredUser;
import com.aeroai.model.SymptomAssessment;
import com.aeroai.repository.RegisteredUserRepository;
import com.aeroai.repository.SymptomAssessmentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Controller
public class SupportController {
    private final RegisteredUserRepository users;
    private final SymptomAssessmentRepository assessments;
    public SupportController(RegisteredUserRepository users, SymptomAssessmentRepository assessments) { this.users=users; this.assessments=assessments; }

    @GetMapping("/support/{section}")
    public String support(@PathVariable String section, HttpSession session, Model model) {
        if (!Set.of("profile","assistant","assessment","medicines","air-quality","notifications","knowledge").contains(section)) return "redirect:/dashboard";
        Long owner=(Long)session.getAttribute("aeroUser");
        RegisteredUser user=users.findById(owner).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        model.addAttribute("section",section); model.addAttribute("activeModule",section); model.addAttribute("appPageTitle", title(section));
        model.addAttribute("profile",user); model.addAttribute("assessments",assessments.findByOwnerIdOrderByCreatedAtDesc(owner));
        if(section.equals("knowledge")) return "knowledge";
        return "support";
    }
    @PostMapping("/support/profile")
    public String updateProfile(@RequestParam String firstName,@RequestParam String lastName,@RequestParam(required=false) String fieldOfInterest,HttpSession session,org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        if(firstName.isBlank()||firstName.trim().length()>80||lastName.isBlank()||lastName.trim().length()>80||fieldOfInterest!=null&&fieldOfInterest.length()>80) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use names up to 80 characters and an interest up to 80 characters.");
        RegisteredUser user=users.findById((Long)session.getAttribute("aeroUser")).orElseThrow(); user.setFirstName(firstName.trim());user.setLastName(lastName.trim());user.setFieldOfInterest(fieldOfInterest==null||fieldOfInterest.isBlank()?null:fieldOfInterest.trim());users.save(user);session.setAttribute("aeroName",user.getFirstName()); flash.addFlashAttribute("profileSaved",true); return "redirect:/support/profile";
    }
    @PostMapping("/api/assessment") @ResponseBody
    public Map<String,Object> assess(@RequestBody Map<String,Object> body,HttpSession session) {
        Object raw=body.get("symptoms"); if(!(raw instanceof List<?> list)||list.isEmpty()||list.size()>12) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select at least one symptom.");
        Set<String> allowed=Set.of("breathing_difficulty","blue_grey","confusion","fainting","chest_pain","breathlessness","chest_discomfort","wheeze","cough","fever","fatigue");
        List<String> values=new ArrayList<>(); for(Object x:list){if(!(x instanceof String s)||!allowed.contains(s))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported symptom selection.");values.add(s);}
        String severity=choice(body,"severity",Set.of("none","mild","moderate","severe"),"none");
        String onset=choice(body,"onset",Set.of("unknown","sudden","gradual"),"unknown");
        String duration=choice(body,"duration",Set.of("unknown","under_day","days","weeks"),"unknown");
        String progression=choice(body,"progression",Set.of("stable","worsening","improving"),"stable");
        String history=choice(body,"history",Set.of("none","asthma","other"),"none");
        boolean emergency=severity.equals("severe")||values.stream().anyMatch(Set.of("breathing_difficulty","blue_grey","confusion","fainting","chest_pain")::contains);
        boolean promptReview=onset.equals("sudden")||progression.equals("worsening")||severity.equals("moderate")||duration.equals("weeks");
        String urgency=emergency?"URGENT":promptReview?"PROMPT_REVIEW":"NON_EMERGENCY";
        String guidance=emergency?"One or more answers indicate a possible emergency warning sign. Call India's emergency number 112 now or seek emergency care immediately. Do not wait for this assessment or its saved result.":promptReview?"This rule-based screen found no selected emergency sign, but the reported onset, progression, severity, or duration warrants prompt advice from a qualified health professional. Seek urgent reassessment if breathing becomes severe, lips or skin look blue/grey, confusion, fainting, or severe chest pain occurs. A reassuring reading does not rule out illness.":"This rule-based screen found no selected emergency sign. It cannot rule out serious illness. Seek qualified medical advice if symptoms are severe, worsening, persistent, or concerning. Seek urgent reassessment for severe breathing difficulty, blue/grey lips or skin, confusion, fainting, or severe chest pain.";
        String summary=String.join(", ",values.stream().map(SupportController::label).toList())+"; severity="+severity+"; onset="+onset+"; duration="+duration+"; progression="+progression+"; history="+history;
        SymptomAssessment saved=assessments.save(new SymptomAssessment((Long)session.getAttribute("aeroUser"),summary,urgency,guidance));
        return Map.of("id",saved.getId(),"urgency",urgency,"guidance",guidance,"doctorSummary","Symptoms selected: "+summary+". Recorded in Aero AI on "+saved.getCreatedAt()+". This summary is user-reported and not a diagnosis.","createdAt",saved.getCreatedAt());
    }
    @PostMapping("/api/assistant") @ResponseBody
    public Map<String,String> assistant(@RequestBody Map<String,Object> body) {
        String q=String.valueOf(body.getOrDefault("question", "")).trim(); if(q.isBlank()||q.length()>500)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a question up to 500 characters.");
        String lower=q.toLowerCase(Locale.ROOT); String answer;
        if(lower.contains("emergency")||lower.contains("blue")||lower.contains("faint")||lower.contains("severe chest")||lower.contains("can't breathe")||lower.contains("cannot breathe")) answer="Severe breathing difficulty, blue or grey lips or skin, confusion, fainting, or severe chest pain need immediate emergency care. In India call 112. Do not wait for this assistant.";
        else if(lower.contains("medicine")||lower.contains("drug")||lower.contains("dose")) answer="I can provide general education only. Medicine choice and dose depend on the person and condition; ask a doctor or pharmacist, and follow the product label. Do not start, stop, or change prescribed treatment based on this response.";
        else if(lower.contains("oxygen")||lower.contains("spo2")||lower.contains("reading")) answer="A single home reading can be affected by device and measurement conditions. Consider how the person feels, follow the device instructions and care plan, and seek medical advice for concerning symptoms. A reading cannot guarantee safety.";
        else answer="I can explain general respiratory anatomy, breathing, gas exchange, air quality, or measurement limitations. I cannot diagnose, assess an emergency, or recommend personal treatment. Tell me which educational topic you want to understand.";
        return Map.of("label","Rule-based assistant","answer",answer);
    }
    @GetMapping("/api/assessment/history") @ResponseBody
    public List<SymptomAssessment> assessmentHistory(HttpSession session){return assessments.findByOwnerIdOrderByCreatedAtDesc((Long)session.getAttribute("aeroUser"));}
    @GetMapping("/emergency") public String emergency(){return "emergency";}
    private static String title(String s){return switch(s){case "air-quality"->"Air Quality Advisor";case "assessment"->"Symptom Assessment";case "medicines"->"Medicine Safety";case "notifications"->"Notifications";case "assistant"->"Respiratory Assistant";case "knowledge"->"Respiratory Knowledge Centre";default->"My Profile";};}
    private static String choice(Map<String,Object> body,String key,Set<String> allowed,String fallback){Object value=body.get(key);if(value==null)return fallback;String text=String.valueOf(value);if(!allowed.contains(text))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported value for "+key+".");return text;}
    private static String label(String s){return switch(s){case "breathing_difficulty"->"severe breathing difficulty";case "blue_grey"->"blue/grey lips or skin";case "confusion"->"confusion";case "fainting"->"fainting";case "chest_pain"->"severe chest pain";case "breathlessness"->"breathlessness";case "chest_discomfort"->"chest discomfort";case "wheeze"->"wheezing";case "cough"->"cough";case "fever"->"fever";default->"fatigue";};}
}
