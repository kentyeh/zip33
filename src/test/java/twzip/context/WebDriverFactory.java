package twzip.context;

import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;

/**
 *
 * @author Kent Yeh
 */
public class WebDriverFactory {

    public static enum Brwoser {

        FIREFOX,
        CHROME,
        SAFARI,
        IE,
        HTMLUNIT,
    }

    public static WebDriver getInstance() {
        return getInstance(Brwoser.HTMLUNIT);
    }

    public static WebDriver getInstance(Brwoser browser, long implicitlyWaitSecs) {
        WebDriver driver = getInstance(browser);
        driver.manage().timeouts().implicitlyWait(implicitlyWaitSecs, TimeUnit.SECONDS);
        return driver;
    }
    
    public static WebDriver getInstance(Brwoser browser) {
        switch (browser) {
            case FIREFOX:
                DesiredCapabilities fc = DesiredCapabilities.firefox();
                fc.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                return new FirefoxDriver(fc);
            case CHROME:
                DesiredCapabilities cc = DesiredCapabilities.chrome();
                cc.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                return new ChromeDriver(cc);
            case SAFARI:
                DesiredCapabilities cs = DesiredCapabilities.safari();
                cs.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                return new SafariDriver(cs);
            case IE:
                DesiredCapabilities ci = DesiredCapabilities.internetExplorer();
                ci.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                return new SafariDriver(ci);
            default:
                DesiredCapabilities ch = DesiredCapabilities.htmlUnit();
                ch.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                return new HtmlUnitDriver(ch);
        }
    }
}
