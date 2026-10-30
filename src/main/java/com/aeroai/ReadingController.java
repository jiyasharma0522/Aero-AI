
package com.aeroai;

import com.aeroai.model.RespiratoryReading;
import com.aeroai.repository.RespiratoryReadingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/readings")
public class ReadingController {

    private final RespiratoryReadingRepository repository;

    public ReadingController(RespiratoryReadingRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public RespiratoryReading saveReading(
            @RequestBody ReadingInput input, HttpSession session) {
        if (input.spo2() < 50 || input.spo2() > 100
                || input.breathingRate() < 1 || input.breathingRate() > 80
                || input.pulse() < 20 || input.pulse() > 250) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use SpO2 50–100%, breathing rate 1–80/min, and pulse 20–250 bpm for this demonstration.");
        }
        // Bind only value fields. Never persist a client supplied ID, owner, or timestamp.
        RespiratoryReading reading = new RespiratoryReading(input.spo2(), input.breathingRate(), input.pulse());
        String origin = input.dataOrigin() == null ? "USER_ENTERED_UNSPECIFIED" : input.dataOrigin();
        if (!java.util.Set.of("SIMULATED", "USER_MEASURED", "USER_ENTERED_UNSPECIFIED").contains(origin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose SIMULATED or USER_MEASURED as the reading source.");
        }
        reading.setDataOrigin(origin);
        reading.setRecordedAt(java.time.LocalDateTime.now());
        reading.setOwnerId((Long) session.getAttribute("aeroUser"));
        return repository.save(reading);
    }

    @GetMapping
    public List<RespiratoryReading> getReadings(HttpSession session) {
        return repository.findByOwnerIdOrderByRecordedAtDesc((Long) session.getAttribute("aeroUser"));
    }

    @PostMapping("/compare")
    public Map<String, Object> compare(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            Long owner = (Long) session.getAttribute("aeroUser");
            long firstId = Long.parseLong(String.valueOf(request.get("firstId")));
            long secondId = Long.parseLong(String.valueOf(request.get("secondId")));
            if (firstId == secondId) throw new IllegalArgumentException();
            RespiratoryReading first = repository.findByIdAndOwnerId(firstId, owner)
                    .orElseThrow(() -> new IllegalArgumentException());
            RespiratoryReading second = repository.findByIdAndOwnerId(secondId, owner)
                    .orElseThrow(() -> new IllegalArgumentException());
            int spo2Delta = second.getSpo2() - first.getSpo2();
            int rateDelta = second.getBreathingRate() - first.getBreathingRate();
            int pulseDelta = second.getPulse() - first.getPulse();
            return Map.of("first", first, "second", second,
                    "changes", Map.of("spo2PercentagePoints", spo2Delta,
                            "breathingRatePerMinute", rateDelta, "pulseBpm", pulseDelta),
                    "explanation", "Change is sample B minus sample A. These are user-entered simulation values, not sensor-validated measurements or a clinical trend.");
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose two different saved samples belonging to your account.");
        }
    }

    public record ReadingInput(int spo2, int breathingRate, int pulse, String dataOrigin) { }
}
