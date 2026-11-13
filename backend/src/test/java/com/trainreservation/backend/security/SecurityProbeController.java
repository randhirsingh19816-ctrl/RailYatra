package com.trainreservation.backend.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityProbeController {

    @GetMapping("/api/admin/security-probe")
    String adminProbe() {
        return "admin";
    }
}
