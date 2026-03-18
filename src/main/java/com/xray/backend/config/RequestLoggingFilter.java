package com.xray.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String clientIp = getClientIp(request);
    String method = request.getMethod();
    String uri = request.getRequestURI();
    long start = System.currentTimeMillis();

    try {
      filterChain.doFilter(request, response);
      long duration = System.currentTimeMillis() - start;
      log.info("[REQUEST] {} {} | IP: {} | {} {} | {}ms",
          method, uri, clientIp, response.getStatus(), response.getContentType(), duration);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - start;
      log.error("[REQUEST] {} {} | IP: {} | ERROR after {}ms: {}",
          method, uri, clientIp, duration, e.getMessage(), e);
      throw e;
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isEmpty()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
