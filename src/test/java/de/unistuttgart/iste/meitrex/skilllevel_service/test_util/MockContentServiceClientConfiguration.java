package de.unistuttgart.iste.meitrex.skilllevel_service.test_util;

import de.unistuttgart.iste.meitrex.content_service.client.ContentServiceClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockContentServiceClientConfiguration {
    @Primary
    @Bean
    public ContentServiceClient getTestContentServiceClient() {
        return Mockito.mock(ContentServiceClient.class);
    }
}
