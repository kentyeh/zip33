package twzip.controller;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
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
        webClient = new WebClient(BrowserVersion.FIREFOX);
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
    public void testZip() throws IOException {
        String url = String.format("http://localhost:%d/%s/", httpPort, contextPath);
        logger.debug("Test index with {}", url);
        HtmlPage beforeInfoPage = webClient.getPage(url);
        HtmlForm form = beforeInfoPage.getFirstByXPath("//form");
        form.getInputByName("addr").setValueAttribute("台北市萬華區大理街132之10號");
        HtmlPage zipPage = form.getOneHtmlElementByAttribute("input", "type", "submit").click();
        HtmlTableCell td = zipPage.getFirstByXPath("//td[@id='zipcode']");
        assertThat("Fail to zip", td.getTextContent(), is(containsString("108021")));
    }

}
