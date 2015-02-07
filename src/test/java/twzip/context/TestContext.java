package twzip.context;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 *
 * @author kent Yeh
 */
@Configuration
@ImportResource({"classpath:testContext.xml"})
public class TestContext extends Context {

}
