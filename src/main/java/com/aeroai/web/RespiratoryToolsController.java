package com.aeroai.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Deterministic educational tools; none of these endpoints consumes sensor data. */
@RestController
public class RespiratoryToolsController {
    private static final Map<String, Map<String, String>> RELIABILITY_SCENARIOS = Map.of(
        "MOTION", Map.of("label", "Movement during measurement", "effect", "Movement can disturb the optical signal and produce unstable or implausible displayed values.", "action", "Sit still, keep the hand supported, and wait for a stable display before recording a sample."),
        "COLD_HAND", Map.of("label", "Cold hand", "effect", "Cold extremities can reduce local blood flow and make a finger measurement harder to obtain or less stable.", "action", "Warm the hand naturally, rest, and recheck according to the device instructions."),
        "POOR_CIRCULATION", Map.of("label", "Reduced finger perfusion", "effect", "Low peripheral blood flow can weaken the pulse signal available to a finger sensor.", "action", "Try a comfortable, warm finger and recheck; follow the device instructions."),
        "NAIL_PRODUCT", Map.of("label", "Nail polish or artificial nail", "effect", "Some nail products or placement conditions may interfere with light transmission for some devices.", "action", "Use a bare finger or another site only if the device instructions allow it, then recheck."),
        "POOR_FIT", Map.of("label", "Loose, misaligned, or wrong-size probe", "effect", "Poor alignment or fit can reduce signal quality and make readings inconsistent.", "action", "Reposition the probe as described by its manufacturer and wait for a stable reading."),
        "BRIGHT_LIGHT", Map.of("label", "Strong ambient light", "effect", "Bright external light may interfere with some optical sensors.", "action", "Shade the sensor from strong light if the manufacturer recommends it, then recheck."),
        "SKIN_TONE", Map.of("label", "Skin pigmentation related accuracy limitation", "effect", "Pulse oximeter accuracy can vary across devices and conditions; some devices may overestimate oxygen saturation, especially at lower saturations, and performance can differ with skin pigmentation.", "action", "Do not rely on this checklist to correct a number. Consider symptoms and qualified clinical advice; repeat or seek assessment according to the device instructions and care plan."),
        "SMOKING_CO", Map.of("label", "Possible carbon monoxide exposure", "effect", "A standard pulse oximeter cannot reliably distinguish oxyhemoglobin from carboxyhemoglobin, so a displayed value can be misleading after carbon monoxide exposure.", "action", "Do not use a repeat finger reading to rule out carbon monoxide exposure; seek urgent local emergency or poison-control guidance if exposure is suspected.")
    );

    @PostMapping("/api/reliability/check")
    public Map<String, Object> checkReliability(@RequestBody Map<String, Object> request) {
        Object raw = request.get("issues");
        if (!(raw instanceof List<?> issues) || issues.size() > RELIABILITY_SCENARIOS.size()) {
            throw bad("Choose a list of known measurement conditions.");
        }
        List<Map<String, String>> results = new ArrayList<>();
        for (Object issue : issues) {
            if (!(issue instanceof String key) || !RELIABILITY_SCENARIOS.containsKey(key)) {
                throw bad("One or more measurement conditions are not supported.");
            }
            Map<String, String> scenario = RELIABILITY_SCENARIOS.get(key);
            results.add(Map.of("code", key, "condition", scenario.get("label"),
                    "possibleEffect", scenario.get("effect"), "recommendedAction", scenario.get("action")));
        }
        boolean repeatCannotResolve = issues.contains("SMOKING_CO") || issues.contains("SKIN_TONE");
        boolean recheck = !results.isEmpty() && !repeatCannotResolve;
        String summary = results.isEmpty()
                ? "No listed measurement issue was selected; this does not establish that a real reading is reliable."
                : issues.contains("SMOKING_CO")
                    ? "A repeat finger reading cannot rule out carbon monoxide exposure; follow the urgent action described below."
                    : issues.contains("SKIN_TONE")
                        ? "Repeating a reading does not correct a device's skin-pigmentation accuracy limitation; use the context and guidance described below."
                        : "One or more selected conditions can affect reliability. Recheck under suitable conditions when appropriate.";
        return Map.of("scenarios", results, "recheckSuggested", recheck,
                "summary", summary,
                "limitation", "Educational scenarios only. This tool receives no sensor signal and cannot validate, correct, or certify a pulse-oximeter reading.");
    }

    @PostMapping("/api/air-quality/advice")
    public Map<String, Object> airQualityAdvice(@RequestBody Map<String, Object> request) {
        int aqi = integer(request, "aqi", 0, 500);
        String scale = String.valueOf(request.getOrDefault("scale", "INDIA_CPCB"));
        if (!scale.equals("INDIA_CPCB") && !scale.equals("US_EPA")) throw bad("Choose the India CPCB or U.S. EPA AQI scale.");
        String category, concern, precaution;
        if (scale.equals("INDIA_CPCB")) {
            if (aqi <= 50) { category = "Good"; concern = "The CPCB category indicates minimal impact."; precaution = "Continue checking official local updates if conditions change."; }
            else if (aqi <= 100) { category = "Satisfactory"; concern = "The CPCB category notes possible minor breathing discomfort for sensitive people."; precaution = "Sensitive people can monitor local conditions and reduce strenuous outdoor activity if they notice symptoms."; }
            else if (aqi <= 200) { category = "Moderate"; concern = "The CPCB category notes possible breathing discomfort for people with lung disease, and discomfort for people with heart disease, children, and older adults."; precaution = "Consider reducing prolonged or strenuous outdoor activity and follow local public-health guidance."; }
            else if (aqi <= 300) { category = "Poor"; concern = "The CPCB category notes possible breathing discomfort with prolonged exposure."; precaution = "Reduce prolonged outdoor exertion where practical, consider cleaner indoor air, and follow local public-health guidance."; }
            else if (aqi <= 400) { category = "Very Poor"; concern = "The CPCB category notes possible respiratory illness with prolonged exposure, with greater concern for people with lung or heart disease."; precaution = "Reduce outdoor exposure, consider cleaner indoor air, and follow local public-health guidance."; }
            else { category = "Severe"; concern = "The CPCB category notes possible respiratory effects even in healthy people and serious impacts for people with lung or heart disease."; precaution = "Avoid outdoor exertion where practical, move to cleaner indoor air, and follow local public-health guidance."; }
        } else {
            if (aqi <= 50) { category = "Good"; concern = "The AirNow category describes air quality as satisfactory with little or no risk."; precaution = "Continue checking local official updates if conditions change."; }
            else if (aqi <= 100) { category = "Moderate"; concern = "Unusually sensitive people may be at greater risk of effects."; precaution = "Unusually sensitive people can consider shorter, less intense outdoor activity if they notice symptoms."; }
            else if (aqi <= 150) { category = "Unhealthy for Sensitive Groups"; concern = "Members of sensitive groups may experience effects."; precaution = "Sensitive groups can reduce prolonged or strenuous outdoor activity and follow local guidance."; }
            else if (aqi <= 200) { category = "Unhealthy"; concern = "Some of the general public may experience effects; sensitive groups may experience more serious effects."; precaution = "Reduce prolonged or strenuous outdoor activity and follow local guidance."; }
            else if (aqi <= 300) { category = "Very Unhealthy"; concern = "The AirNow category indicates an increased risk of health effects for everyone."; precaution = "Reduce outdoor exertion, use cleaner indoor air when available, and follow local guidance."; }
            else { category = "Hazardous"; concern = "The AirNow category signals emergency conditions with increased likelihood of effects for everyone."; precaution = "Avoid outdoor exertion, move to cleaner indoor air when possible, and follow local emergency guidance."; }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inputAqi", aqi); result.put("category", category); result.put("possibleConcern", concern);
        result.put("practicalPrecaution", precaution); result.put("sourceMode", "MANUAL_INPUT"); result.put("scale", scale);
        result.put("scaleReference", scale.equals("INDIA_CPCB") ? "https://cpcb.nic.in/displaypdf.php?id=bmF0aW9uYWwtYWlyLXF1YWxpdHktaW5kZXgvQWJvdXRfQVFJLnBkZg" : "https://www.airnow.gov/aqi/aqi-basics/");
        result.put("sourceNote", "The value was entered by the user. Aero AI did not fetch or verify live air-quality data.");
        result.put("limitation", "AQI is a general outdoor air-quality index, not a personal exposure dose or medical assessment. Conditions vary by pollutant, location, time, and individual sensitivity.");
        return result;
    }

    @PostMapping("/api/alerts/evaluate")
    public Map<String, Object> evaluateAlerts(@RequestBody Map<String, Object> request) {
        int spo2 = integer(request, "spo2", 50, 100);
        int rate = integer(request, "breathingRate", 1, 80);
        int pulse = integer(request, "pulse", 20, 250);
        int spo2Floor = integer(request, "spo2Below", 50, 100, 94);
        int rateCeiling = integer(request, "rateAbove", 1, 80, 22);
        int pulseFloor = integer(request, "pulseBelow", 20, 250, 50);
        int pulseCeiling = integer(request, "pulseAbove", 20, 250, 120);
        if (pulseFloor >= pulseCeiling) throw bad("pulseBelow must be lower than pulseAbove.");
        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(rule("SpO2 lower-bound example", "spo2", spo2, "<", spo2Floor,
                "SpO2 sample is below the configured educational threshold."));
        rules.add(rule("Breathing-rate upper-bound example", "breathingRate", rate, ">", rateCeiling,
                "Breathing-rate sample is above the configured educational threshold."));
        rules.add(rule("Pulse lower-bound example", "pulse", pulse, "<", pulseFloor,
                "Pulse sample is below the configured educational threshold."));
        rules.add(rule("Pulse upper-bound example", "pulse", pulse, ">", pulseCeiling,
                "Pulse sample is above the configured educational threshold."));
        long matches = rules.stream().filter(rule -> Boolean.TRUE.equals(rule.get("matched"))).count();
        return Map.of("rules", rules, "matchedRules", matches,
                "summary", matches == 0 ? "No configured educational rule matched these inputs." : matches + " configured educational rule(s) matched these inputs.",
                "limitation", "Configurable teaching rules only. Inputs are user-entered and are not a diagnosis, validated clinical threshold, or emergency alert.");
    }

    private static Map<String, Object> rule(String name, String inputName, int input, String operator,
                                             int threshold, String matchedReason) {
        boolean matched = operator.equals("<") ? input < threshold : input > threshold;
        return Map.of("name", name, "input", inputName, "inputValue", input,
                "rule", inputName + " " + operator + " " + threshold,
                "calculation", input + " " + operator + " " + threshold + " = " + matched,
                "threshold", threshold, "matched", matched,
                "reason", matched ? matchedReason : "Input does not satisfy the configured comparison." );
    }

    private static int integer(Map<String, Object> request, String key, int min, int max) {
        return integer(request, key, min, max, null);
    }
    private static int integer(Map<String, Object> request, String key, int min, int max, Integer fallback) {
        Object value = request.get(key);
        if (value == null && fallback != null) return fallback;
        try {
            int number = Integer.parseInt(String.valueOf(value));
            if (number < min || number > max) throw new NumberFormatException();
            return number;
        } catch (RuntimeException exception) {
            throw bad("" + key + " must be an integer from " + min + " to " + max + ".");
        }
    }
    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
