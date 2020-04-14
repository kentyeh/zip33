package twzip.context;

import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

/**
 *
 * @author Kent Yeh
 */
public class WebDriverFactory {

    public static enum Brwoser {

        FIREFOX,
        CHROME,
        SAFARI,
        EDGE,
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
                FirefoxProfile firefoxProfile = new FirefoxProfile();
                firefoxProfile.setPreference("media.navigator.streams.fake", true);
                FirefoxOptions ffOptions = new FirefoxOptions();
                ffOptions.setProfile(firefoxProfile);
                ffOptions.setAcceptInsecureCerts(true);
                ffOptions.setBinary("/usr/bin/firefox");
                return new FirefoxDriver(ffOptions);
            case CHROME:
                ChromeOptions cOptions = new ChromeOptions();
                cOptions.setAcceptInsecureCerts(true);
                cOptions.setBinary("/usr/bin/google-chrome");
                cOptions.addArguments("use-fake-ui-for-media-stream");
                cOptions.addArguments("use-fake-device-for-media-stream");
                return new ChromeDriver(cOptions);
            case SAFARI:
                DesiredCapabilities cs = DesiredCapabilities.safari();
                cs.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                SafariOptions sOptions = new SafariOptions(cs);
                sOptions.setCapability("autoclose", true);
                sOptions.setCapability("headless", true);
                sOptions.setUseTechnologyPreview(true);
                return new SafariDriver(sOptions);
            case EDGE:
                EdgeOptions eOptions = new EdgeOptions();
                eOptions.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                eOptions.setCapability("autoclose", true);
                eOptions.setCapability("headless", true);
                eOptions.setCapability("avoidProxy", true);
                return new EdgeDriver(eOptions);
            default:
                DesiredCapabilities ch = DesiredCapabilities.htmlUnit();
                ch.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                return new HtmlUnitDriver(ch);
        }
    }
}
