package com.loanpro.ecommerce.karate.products;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductControllerKarateTest {

    @LocalServerPort
    int port;

    @Test
    void testProductEndpoints() {
        Results results = Runner.path("classpath:karate/products/products.feature")
                .systemProperty("server.port", String.valueOf(port))
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
