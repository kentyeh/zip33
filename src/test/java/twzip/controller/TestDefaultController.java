package twzip.controller;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import twzip.model.Zip33;

/**
 *
 * @author kent
 */
@WebAppConfiguration
@ContextConfiguration(classes = twzip.context.TestContext.class)
public class TestDefaultController extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LogManager.getLogger(TestDefaultController.class);
    WebApplicationContext wac;
    private MockMvc mockMvc;

    @Autowired
    public void setWac(WebApplicationContext wac) {
        this.wac = wac;
    }

    @BeforeClass
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).alwaysExpect(status().isOk()).build();
    }

    public <T> ResultMatcher asyncJsonPath(final String expression, final Matcher<T> matcher) {
        return (MvcResult result) -> {
            HttpServletRequest request = result.getRequest();
            assertThat("Async not started.", request.isAsyncStarted());
            Object res = result.getAsyncResult();
            assertThat("Not string return.", res, is(instanceOf(String.class)));
            new JsonPathExpectationsHelper(expression).assertValue((String) res, matcher);
        };
    }

    @DataProvider(name = "addrs", parallel = false)
    public String[][] addrs() {
        String[][] res = new String[15][2];
        res[0][0] = "806029";
        res[0][1] = "高雄市前鎮區翠亨北路399巷1號";
        res[1][0] = "407009";
        res[1][1] = "臺中市西屯區福林里008鄰西屯路三段１５９之１１巷９弄６號九樓之１";
        res[2][0] = "833171";
        res[2][1] = "高雄市鳥松區鳥松里009鄰松埔路１巷１０號";
        res[3][0] = "407009";
        res[3][1] = "臺中市西屯區福雅里023鄰西屯路三段１５１之７之１號";
        res[4][0] = "406047";
        res[4][1] = "臺中市北屯區松強里001鄰昌平路二段１２之１５巷１６號";
        res[5][0] = "542024";
        res[5][1] = "南投縣草屯鎮土城里003鄰中正路２１７之１２之１巷３８號";
        res[6][0] = "356003";
        res[6][1] = "苗栗縣後龍鎮埔頂里中心路98號";
        res[7][0] = "972003";
        res[7][1] = "花蓮縣秀林鄉崇德村3鄰崇德210號";
        res[8][0] = "100040";
        res[8][1] = "臺北市中正區板溪里001鄰牯嶺街９５巷２號七樓之１";
        res[9][0] = "506016";
        res[9][1] = "彰化縣福興鄉外埔村復興路30-11號";
        res[10][0] = "106219";
        res[10][1] = "臺北市中正區羅斯福路四段７４號地下一層";
        res[11][0] = "114064";
        res[11][1] = "臺北市內湖區堤頂大道２段13號";
        res[12][0] = "324009";
        res[12][1] = "桃園市平鎮區復旦路二段211巷49號";//因為有無段復旦路
        res[13][0] = "249652";
        res[13][1] = "新北市八里區商港六路53號9樓";//因為有無段復旦路
        res[14][0] = "827012";
        res[14][1] = "高雄市彌陀區南寮路光華巷30號";//因為南寮路有無巷也有奇怪的巷
        return res;
    }

    @Test(dataProvider = "addrs")
    public void testZip1(String zipcode, String addr) throws Exception {
        logger.debug("測試{}:{}", zipcode, addr);
        MvcResult mvcResult = mockMvc.perform(post("/").param("addr", addr)).andExpect(request().asyncStarted())
                .andExpect(request().asyncStarted()).andExpect(request().asyncResult(not(emptyOrNullString()))).andReturn();
        List<Zip33> zips = (List<Zip33>) mvcResult.getRequest().getAttribute("zips");
        assertThat("Error zip.", zips, is(hasSize(1)));
        assertThat("Error zip.", zips.get(0).getZipcode(), is(equalTo(zipcode)));
    }

    @Test
    public void testZip2() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/").param("addr", "花蓮縣鳳林鎮信義路248號")).andExpect(request().asyncStarted())
                .andExpect(request().asyncStarted()).andExpect(request().asyncResult(not(is(emptyOrNullString())))).andReturn();
        List<Zip33> zips = (List<Zip33>) mvcResult.getRequest().getAttribute("zips");
        assertThat("Error zip.", zips, anyOf(nullValue(), empty()));
    }

    @Test
    public void testZip3() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/").param("addr", "桃園市平鎮區平興里001鄰居易一區７號")).andExpect(request().asyncStarted())
                .andExpect(request().asyncStarted()).andExpect(request().asyncResult(not(is(emptyOrNullString())))).andReturn();
        List<Zip33> zips = (List<Zip33>) mvcResult.getRequest().getAttribute("zips");
        assertThat("Error zip.", zips, anyOf(nullValue(), empty()));
    }
}
