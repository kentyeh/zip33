package twzip.controller;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlHeading1;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * 僅使用HtmlUnit進行整合測試。
 * @author Kent Yeh
 */
@Test(groups = {"integrate"})
public class TestIntegratedWithHunitUnitOnly {

    private static Logger logger = LogManager.getLogger(TestIntegratedWithHunitUnitOnly.class);
    private int httpPort = 80;
    private String contextPath = "";
    private WebClient webClient;

    @BeforeClass
    @Parameters({"http.port", "contextPath"})
    public void setup(@Optional("http.port") int httpPort, @Optional("contextPath") String contextPath) {
        this.httpPort = httpPort;
        logger.debug("http port is {}", httpPort);
        this.contextPath = contextPath;
        webClient = new WebClient(BrowserVersion.FIREFOX_52);
        webClient.getOptions().setUseInsecureSSL(true);
    }

    @AfterClass
    public void tearDown() {
        if (this.webClient != null) {
            this.webClient.close();
        }
        logger.info("Finished !");
    }

    @Test(expectedExceptions = FailingHttpStatusCodeException.class)
    public void test404() throws IOException {
        String url = String.format("http://localhost:%d/%s/unknownpath/404.html", httpPort, contextPath);
        logger.debug("Integration Test: test404 with {}", url);
        HtmlPage page404 = webClient.getPage(url);
    }

    @Test
    public void testMyInfo() throws IOException {
        String url = String.format("http://localhost:%d/%s/user/myinfo", httpPort, contextPath);
        logger.debug("Test myinfo with {}", url);
        HtmlPage beforeInfoPage = webClient.getPage(url);
        HtmlForm form = beforeInfoPage.getFirstByXPath("//form");
        form.getInputByName("j_username").setValueAttribute("admin");
        form.getInputByName("j_password").setValueAttribute("admin");
        HtmlPage myInfoPage = form.getOneHtmlElementByAttribute("input", "type", "submit").click();
        HtmlHeading1 h1 = myInfoPage.getFirstByXPath("//h1");
        assertThat("Fail to get My Info", h1.getTextContent(), is(containsString("admin")));
    }

    @Test(dependsOnMethods = "testMyInfo")
    public void logout() throws IOException {
        String url = String.format("http://localhost:%d/%s/", httpPort, contextPath);
        logger.debug("Integration Test: logout with {}", url);
        HtmlPage homePage = webClient.getPage(url);
        HtmlForm form = homePage.getFirstByXPath("//form");
        homePage = form.getOneHtmlElementByAttribute("input", "type", "submit").click();
        logger.debug("logout redirect to {}", homePage.getUrl());
        assertThat("logout failed ", homePage.getUrl().toString(), is(containsString("/index")));
    }
}
