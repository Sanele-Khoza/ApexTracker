package com.apextracker.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.apextracker.common.ApiException;
import com.apextracker.user.User;

@Service
public class CurrentUserService {

    public Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public User get() {
        Authentication auth = authentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw ApiException.unauthorized("You must be logged in");
        }
        return user;
    }
}