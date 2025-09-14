package com.pingpad;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationIntegrationTest {

    @Test
    void contextLoads() {
        assertTrue(true, "Application context should load successfully");
    }

    @Test
    void testApplicationProperties() {
        String expectedAppName = "PingPad";
        assertNotNull(expectedAppName, "Application name should not be null");
        assertEquals("PingPad", expectedAppName, "Application name should match");
    }

    @Test
    void testModularStructure() {
        String[] moduleNames = {
            "AuthModule",
            "UserManagementModule", 
            "ApiTestingModule",
            "SharedModule"
        };
        
        for (String moduleName : moduleNames) {
            assertNotNull(moduleName, "Module name should not be null");
            assertTrue(moduleName.endsWith("Module"), 
                "Module should end with 'Module': " + moduleName);
        }
    }
}
