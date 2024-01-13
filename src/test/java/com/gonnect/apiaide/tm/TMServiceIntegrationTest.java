package com.gonnect.apiaide.tm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TMServiceIntegrationTest {

    @Autowired
    TMService tmService;

    @Test
    public void loadContext() {
        tmService.run();
    }


}
