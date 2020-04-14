package twzip.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import twzip.context.WebDriverFactory;

/**
 *
 * @author Kent Yeh
 */
@Test(groups = {"integrate"})
public class TestIntegration {

    private static Logger logger = LogManager.getLogger(TestIntegration.class);
    private int httpPort = 80;
    private String contextPath = "";
    private WebDriver driver;

    @BeforeClass
    @Parameters({"http.port", "contextPath"})
    public void setup(@Optional("http.port") int httpPort, @Optional("contextPath") String contextPath) {
        this.httpPort = httpPort;
        logger.debug("http port is {}", httpPort);
        this.contextPath = contextPath;
        driver = WebDriverFactory.getInstance(WebDriverFactory.Brwoser.HTMLUNIT);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testZip() {
        String url = String.format("http://localhost:%d/%s", httpPort, contextPath);
        logger.debug("Test zip with {}", url);
        driver.get(url);
        WebElement form =  driver.findElement(By.tagName("form"));
        form.findElement(By.name("addr")).sendKeys("台北市萬華區大理街132之10號");
        form.submit();
        WebElement zip = driver.findElement(By.xpath("//td[@id='zipcode']"));
        assertThat("Fail to get My Info", zip.getText(), is(containsString("108021")));
    }
}
