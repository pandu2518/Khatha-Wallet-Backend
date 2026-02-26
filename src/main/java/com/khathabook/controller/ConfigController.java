package com.khathabook.controller;

import com.khathabook.model.FeatureFlag;
import com.khathabook.repository.FeatureFlagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @GetMapping("/flags")
    public Map<String, Boolean> getFeatureFlags() {
        List<FeatureFlag> flags = featureFlagRepository.findAll();
        Map<String, Boolean> flagMap = new HashMap<>();
        for (FeatureFlag flag : flags) {
            flagMap.put(flag.getFlagKey(), flag.isEnabled());
        }
        return flagMap;
    }

    // Optional: Endpoint to create or toggle a flag (for admin use)
    @PostMapping("/flags")
    public FeatureFlag setFeatureFlag(@RequestBody FeatureFlag flag) {
        return featureFlagRepository.findByFlagKey(flag.getFlagKey())
                .map(existing -> {
                    existing.setEnabled(flag.isEnabled());
                    existing.setDescription(flag.getDescription());
                    return featureFlagRepository.save(existing);
                })
                .orElseGet(() -> featureFlagRepository.save(flag));
    }
}
