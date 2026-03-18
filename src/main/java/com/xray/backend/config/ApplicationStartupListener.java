package com.xray.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger log = LoggerFactory.getLogger(ApplicationStartupListener.class);

  @Override
  public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
    Environment env = event.getApplicationContext().getEnvironment();
    String port = env.getProperty("local.server.port", "unknown");
    String address = env.getProperty("server.address", "0.0.0.0");
    log.info("🚀 Backend started successfully - listening on {}:{}", address, port);
  }
}
