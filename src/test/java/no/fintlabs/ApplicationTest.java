package no.fintlabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationTest {

    @Test
    void testApplicationCreation() {
        Application application = new Application();

        assertNotNull(application);
    }
}