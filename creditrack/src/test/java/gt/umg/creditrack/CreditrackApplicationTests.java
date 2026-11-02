package gt.umg.creditrack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIf("gt.umg.creditrack.CreditrackApplicationTests#hayConfiguracionLocal")
class CreditrackApplicationTests {

    static boolean hayConfiguracionLocal() {
        return CreditrackApplicationTests.class.getResource("/application-dev.properties") != null;
    }

    @Test
    void contextLoads() {
    }
}
