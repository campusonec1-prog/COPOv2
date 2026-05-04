package com.copo.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class RoleFilter implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RoleFilter.class);

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        if (session == null) {
            log.debug("No session found, redirecting to login. URI: {}", request.getRequestURI());
            response.sendRedirect("/login?error=sessionExpired");
            return false;
        }

        String role = (String) session.getAttribute("role");
        String uri = request.getRequestURI();

        // Faculty-only routes
        if (uri.startsWith("/faculty") || uri.startsWith("/faculty-marks") ||
            uri.startsWith("/batches") ||
            uri.startsWith("/departments") || uri.startsWith("/copo")) {

            log.debug("Faculty-only URI accessed: {}", uri);
            if (!"faculty".equals(role)) {
                log.warn("Unauthorized access attempt on faculty URI [{}] by role [{}]", uri, role);
                response.sendRedirect("/login?error=unauthorized");
                return false;
            }
        }

        // Student-only routes
        if (uri.startsWith("/student-marks")) {
            log.debug("Student-only URI accessed: {}", uri);
            if (!"student".equals(role)) {
                log.warn("Unauthorized access attempt on student URI [{}] by role [{}]", uri, role);
                response.sendRedirect("/login?error=unauthorized");
                return false;
            }
        }

        // Shared URIs — require either role
        if ((uri.startsWith("/subjects") || uri.startsWith("/questions") || uri.startsWith("/students")) &&
            !(role != null && (role.equals("faculty") || role.equals("student")))) {

            log.warn("Unauthorized access on shared URI [{}] by role [{}]", uri, role);
            response.sendRedirect("/login?error=unauthorized");
            return false;
        }

        return true;
    }
}
