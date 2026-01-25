package com.linkwave.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/linkwave_test",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PlaceholderTests {

    @Test
    void contextLoads() {
        // Placeholder test to verify application context loads successfully
    }

}
