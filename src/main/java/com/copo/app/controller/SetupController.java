package com.copo.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Setup controller — ONLY ACTIVE in 'dev' profile.
 * This entire controller is disabled in production (profile=prod).
 *
 * If you need to run setup in production, use the CLI:
 *   GET /faculty/init-passwords  (faculty-only, authenticated route)
 */
@Controller
@RequestMapping("/setup")
@Profile("dev")   // ← This disables ALL /setup/** endpoints in production
public class SetupController {

    private static final Logger logger = LoggerFactory.getLogger(SetupController.class);

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @GetMapping("/info")
    public String setupInfo(Model model) {
        model.addAttribute("setupResult",
            "Setup endpoints are only available in dev profile.\nActive profile: " + activeProfile);
        model.addAttribute("success", true);
        logger.info("Setup info page accessed in dev profile.");
        return "setup/result";
    }
}



