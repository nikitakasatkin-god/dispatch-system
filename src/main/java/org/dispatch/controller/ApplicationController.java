package org.dispatch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ApplicationController {

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/trips")
    public String trips() {
        return "trips";
    }

    @GetMapping("/trip-detail")
    public String tripDetail(@RequestParam Long id, Model model) {
        model.addAttribute("tripId", id);
        return "trip-detail";
    }

    @GetMapping("/references")
    public String references() {
        return "references";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
}