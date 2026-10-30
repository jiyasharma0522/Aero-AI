package com.aeroai.web;

import com.aeroai.model.ExperimentRun;
import com.aeroai.repository.ExperimentRunRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {
    private final ExperimentRunRepository runs;
    public ExperimentController(ExperimentRunRepository runs) { this.runs = runs; }
    @GetMapping public List<ExperimentRun> history(HttpSession session) {
        return runs.findByOwnerIdOrderByCreatedAtDesc((Long) session.getAttribute("aeroUser"));
    }
    @PostMapping public ExperimentRun save(@RequestBody Map<String, Object> input, HttpSession session) {
        try {
            String name = String.valueOf(input.getOrDefault("name", "Experiment")).trim();
            int rate = Integer.parseInt(String.valueOf(input.get("respiratoryRate")));
            int volume = Integer.parseInt(String.valueOf(input.get("tidalVolumeMl")));
            int compliance = Integer.parseInt(String.valueOf(input.get("compliance")));
            if (name.isBlank() || name.length() > 80 || rate < 6 || rate > 40 || volume < 200 || volume > 1000 || compliance < 10 || compliance > 100)
                throw new IllegalArgumentException();
            ExperimentRun run = new ExperimentRun(name, rate, volume, compliance);
            run.setOwnerId((Long) session.getAttribute("aeroUser"));
            return runs.save(run);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a name up to 80 characters, rate 6–40, volume 200–1000 mL, and compliance 10–100.");
        }
    }

    @PostMapping("/compare")
    @Transactional
    public Map<String, Object> compare(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            Map<String, Object> inputA = object(request.get("scenarioA"));
            Map<String, Object> inputB = object(request.get("scenarioB"));
            ExperimentRun a = scenario(inputA, (Long) session.getAttribute("aeroUser"));
            ExperimentRun b = scenario(inputB, (Long) session.getAttribute("aeroUser"));
            a = runs.save(a); b = runs.save(b);
            double difference = b.getSimulatedVentilation() - a.getSimulatedVentilation();
            return Map.of("scenarioA", result(a), "scenarioB", result(b),
                    "differenceLPerMin", difference,
                    "differenceFormula", b.getSimulatedVentilation() + " - " + a.getSimulatedVentilation() + " = " + difference,
                    "modelFormula", "respiratory rate × (tidal volume in mL ÷ 1000) = modeled L/min",
                    "assumptions", List.of("Tidal volume is treated as delivered volume for each modeled breath.",
                            "Dead space, gas exchange, respiratory mechanics, and individual physiology are not modeled.",
                            "Compliance is saved as a scenario parameter but does not alter this simplified equation."),
                    "limitation", "This is a simplified educational calculation, not a patient-specific or clinically validated prediction.");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide two named scenarios with rate 6–40/min, volume 200–1000 mL, and compliance 10–100.");
        }
    }

    @GetMapping(value = "/report", produces = "text/csv")
    public ResponseEntity<String> comparisonReport(@RequestParam long firstId,
                                                    @RequestParam long secondId,
                                                    HttpSession session) {
        try {
            Long owner = (Long) session.getAttribute("aeroUser");
            if (firstId == secondId) throw new IllegalArgumentException();
            ExperimentRun a = runs.findByIdAndOwnerId(firstId, owner).orElseThrow(IllegalArgumentException::new);
            ExperimentRun b = runs.findByIdAndOwnerId(secondId, owner).orElseThrow(IllegalArgumentException::new);
            double delta = b.getSimulatedVentilation() - a.getSimulatedVentilation();
            StringBuilder csv = new StringBuilder("Scenario,Rate per minute,Tidal volume mL,Compliance index,Modeled ventilation L/min\r\n");
            csv.append(csv(a.getName())).append(',').append(a.getRespiratoryRate()).append(',').append(a.getTidalVolumeMl()).append(',').append(a.getCompliance()).append(',').append(a.getSimulatedVentilation()).append("\r\n");
            csv.append(csv(b.getName())).append(',').append(b.getRespiratoryRate()).append(',').append(b.getTidalVolumeMl()).append(',').append(b.getCompliance()).append(',').append(b.getSimulatedVentilation()).append("\r\n");
            csv.append("Difference B-A,,,,").append(delta).append("\r\n");
            csv.append("Formula,").append(csv("respiratory rate × (tidal volume in mL ÷ 1000) = modeled L/min")).append(",,,\r\n");
            csv.append("Assumption,").append(csv("Tidal volume is treated as delivered volume for each modeled breath.")).append(",,,\r\n");
            csv.append("Assumption,").append(csv("Dead space, gas exchange, respiratory mechanics, and individual physiology are not modeled.")).append(",,,\r\n");
            csv.append("Assumption,").append(csv("Compliance is saved as a scenario parameter but does not alter this simplified equation.")).append(",,,\r\n");
            csv.append("Limit,").append(csv("Educational calculation only; not a patient-specific or clinically validated prediction.")).append(",,,\r\n");
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aero-ai-what-if-comparison.csv\"")
                    .body(csv.toString());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose two different saved scenarios belonging to your account.");
        }
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException();
        Map<String, Object> result = new java.util.HashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }
    private static ExperimentRun scenario(Map<String, Object> input, Long ownerId) {
        String name = String.valueOf(input.getOrDefault("name", "")).trim();
        int rate = Integer.parseInt(String.valueOf(input.get("respiratoryRate")));
        int volume = Integer.parseInt(String.valueOf(input.get("tidalVolumeMl")));
        int compliance = Integer.parseInt(String.valueOf(input.get("compliance")));
        if (name.isBlank() || name.length() > 80 || rate < 6 || rate > 40 || volume < 200 || volume > 1000 || compliance < 10 || compliance > 100)
            throw new IllegalArgumentException();
        ExperimentRun run = new ExperimentRun(name, rate, volume, compliance);
        run.setOwnerId(ownerId);
        return run;
    }
    private static Map<String, Object> result(ExperimentRun run) {
        return Map.of("id", run.getId(), "name", run.getName(),
                "respiratoryRate", run.getRespiratoryRate(), "tidalVolumeMl", run.getTidalVolumeMl(),
                "compliance", run.getCompliance(), "simulatedVentilation", run.getSimulatedVentilation(),
                "calculation", run.getRespiratoryRate() + " × (" + run.getTidalVolumeMl() + " ÷ 1000) = " + run.getSimulatedVentilation() + " L/min");
    }
    private static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
