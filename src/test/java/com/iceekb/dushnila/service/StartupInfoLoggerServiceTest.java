package com.iceekb.dushnila.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartupInfoLoggerServiceTest {

    @Test
    void run_doesNotThrow_whenBuildPropertiesAbsent() {
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> provider = (ObjectProvider<BuildProperties>) mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        StartupInfoLoggerService svc = new StartupInfoLoggerService(provider);
        assertDoesNotThrow(() -> svc.run(null));
    }

    @Test
    void run_doesNotThrow_whenBuildPropertiesPresent() {
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> provider = (ObjectProvider<BuildProperties>) mock(ObjectProvider.class);

        Properties props = new Properties();
        props.put("name", "Dushnila");
        props.put("version", "1.2.3");
        BuildProperties bp = new BuildProperties(props);
        when(provider.getIfAvailable()).thenReturn(bp);

        StartupInfoLoggerService svc = new StartupInfoLoggerService(provider);
        assertDoesNotThrow(() -> svc.run(null));
    }
}


