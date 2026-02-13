package org.scaas.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/secure")
    public String secure(Authentication authentication) {
        if (authentication == null) {
            return "Authentication is NULL";
        }
        return "User: " + authentication.getName() +
                " | Authorities: " + authentication.getAuthorities();
    }
}
