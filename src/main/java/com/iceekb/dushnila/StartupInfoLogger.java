package com.iceekb.dushnila;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupInfoLogger implements ApplicationRunner {

    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Override
    public void run(ApplicationArguments args) {
        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        if (bp == null) {
            log.info("Starting application (version: unknown)");
            return;
        }
        log.info("Starting {} v{}", bp.getName(), bp.getVersion());
    }
}


