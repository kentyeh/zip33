package twzip.context;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import twzip.model.Village;

/**
 * 擷取 <a href="hsinchu-meng-733082.middle2.me" target="_blank">hsinchu-meng-733082.middle2.me</a>網頁上的行政區資料
 * @author Kent Yeh
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Lazy
public class CrawlerRunner implements Runnable, ApplicationContextAware {
    
    private static final Logger logger = LogManager.getLogger(CrawlerRunner.class);
    public static final String HOST = "hsinchu-meng-733082.middle2.me";
    public static final String URL = "http://" + HOST + "/";
    public static final String VILSQL = "village.sql";
    private ApplicationContext ctx;
    private String areacode, city, dist;
    private int level = 0;
    @Autowired
    private XPath xpath;
    @Autowired
    private CloseableHttpClient httpclient;
    @Autowired
    CrawlerService crawlerService;
    @Value("#{appProperies}")
    Map<String, String> appProperies;
    
    public void setAreacode(String areacode) {
        this.areacode = areacode;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public void setDist(String dist) {
        this.dist = dist;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.ctx = ac;
    }
    
    @Override
    public void run() {
        HttpGet httpget = new HttpGet(URL + (areacode == null ? "" : "area/" + areacode));
        try {
            httpclient.execute(httpget, new ResponseHandler<Void>() {
                
                @Override
                public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    Tidy tidy = new Tidy();
                    tidy.setXmlOut(true);
                    tidy.setShowWarnings(false);
                    tidy.setInputEncoding("UTF8");
                    tidy.setQuiet(true);
                    tidy.setForceOutput(false);
                    tidy.setHideComments(true);
                    tidy.setTidyMark(false);
                    tidy.setHideEndTags(false);
                    HttpEntity entity = response.getEntity();
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        Document doc = tidy.parseDOM(entity.getContent(), new OutputStream() {
                            
                            @Override
                            public void write(int b) throws IOException {
                                
                            }
                        });
                        try {
                            XPathExpression expr = xpath.compile(".//ol[not(@class)]/li/a");
                            NodeList anchors = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                            for (int i = 0; i < anchors.getLength(); i++) {
                                Node anchor = anchors.item(i);
                                String name = anchor.getFirstChild().getNodeValue();
                                NamedNodeMap nnn = anchors.item(i).getAttributes();
                                String code = nnn.getNamedItem("href").getNodeValue();
                                String[] vs = name.split("\\s+");
                                name = vs[vs.length - 1];
                                vs = code.split("\\/");
                                code = vs[vs.length - 1];
                                if (level == 0) {
                                    city = name;
                                } else if (level == 1) {
                                    dist = name;
                                }
                                if (code.length() < 8) {
                                    CrawlerRunner acr = ctx.getBean(CrawlerRunner.class);
                                    acr.setLevel(level + 1);
                                    acr.setCity(city);
                                    acr.setDist(dist);
                                    acr.setAreacode(code);
                                    crawlerService.submit(acr);
                                }
                                if (level == 2) {
                                    Village vil = new Village(city, dist, name);
                                    try (BufferedWriter wr = Files.newBufferedWriter(Paths.get(appProperies.get("resfolder"), VILSQL), StandardCharsets.UTF_8,
                                            StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                                        wr.write("insert into village(city,dist,vil)values('" + vil.getCity()
                                                + "','" + vil.getDist() + "','" + vil.getVil() + "');\n");
                                        logger.debug("取得\"{}\"", vil);
                                    }
                                }
                            }
                        } catch (XPathExpressionException ex) {
                            logger.error(String.format("解析行政區[%s]發生錯誤:%s", areacode, ex.getMessage()), ex);
                        }
                    } else {
                        logger.error("無法取得行政區[{}]內容", areacode);
                    }
                    return null;
                }
            });
        } catch (IOException ex) {
            logger.error(ex);
        }
    }
    
}
