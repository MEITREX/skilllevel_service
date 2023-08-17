package de.unistuttgart.iste.gits.skilllevel_service.test_util;

import de.unistuttgart.iste.gits.skilllevel_service.service.ContentServiceClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockContentServiceClientConfiguration {
    @Primary
    @Bean
    public ContentServiceClient getTestContentServiceClient() {
        final ContentServiceClient client = Mockito.mock(ContentServiceClient.class);

        return client;
    }
}
