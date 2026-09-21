package com.lucho.tienda.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucho.tienda.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.lucho.tienda.constant.ErrorMessageConstants.*;
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    // Inject Spring's primary ObjectMapper to safely serialize LocalDateTime and records
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ACCESS_DENIED_MESSAGE,
                request.getRequestURI()
        );

        // Write and explicitly flush the buffer to ensure the response body is delivered
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}