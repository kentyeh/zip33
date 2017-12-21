package twzip.context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Log4j2Log;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import twzip.model.Address;
import twzip.model.Dao;
import twzip.model.IntSecquencer;
import twzip.model.Post5;
import twzip.model.Village;
import zip4pos.util.function.Consumer;
import zip4pos.util.function.IntSupplier;

/**
 *
 * @author Kent Yeh
 */
public class Context implements InitializingBean, DisposableBean, ApplicationContextAware {

    private static final Logger logger = LogManager.getLogger(ApplicationContext.class);
    private org.springframework.context.ApplicationContext context;
    private PoolingHttpClientConnectionManager cm = null;

    @Autowired
    DataSource ds;
    @Value("#{appProperies}")
    Map<String, String> appProperies;

    @Override
    public void setApplicationContext(org.springframework.context.ApplicationContext ac) throws BeansException {
        context = ac;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String respath = appProperies.get("resfolder");
        try (Dao dao = context.getBean("readOnlyDao", Dao.class)) {
            Path post5sql = Paths.get(respath, "post5.sql");
            Path vilsql = Paths.get(respath, CrawlerRunner.VILSQL);
            CountDownLatch countDownLatch = null;
            if (Files.exists(vilsql) && (dao.tableExists("village") == null || dao.vilCount() == 0)) {
                logger.fatal("感謝 {} 提供村里行政區資料", CrawlerRunner.URL);
                resetVIlage(vilsql);
            } else if (dao.tableExists("village") == null || dao.vilCount() == 0) {
                logger.fatal("感謝 {} 提供村里行政區資料", CrawlerRunner.URL);
                CrawlerService service = context.getBean(CrawlerService.class);
                service.submit(context.getBean(CrawlerRunner.class));
                countDownLatch = service.getCountDownLatch();
            }
            if (Files.exists(post5sql) && (dao.tableExists("post5") == null || dao.post5Count() == 0)) {
                resetPost5(post5sql);
                if (!validDb()) {
                    logger.error("設定檔post5.sql表達式有誤，請修改後再重新執行");
                    throw new RuntimeException("設定檔post5.sql表達式有誤，請修改後再重新執行");
                }
            } else if (dao.tableExists("post5") == null || dao.post5Count() == 0) {
                //整理郵局檔案時，必須配合村里資料
                if (countDownLatch != null) {
                    countDownLatch.await();
                    resetVIlage(vilsql);
                }
                initPost5(Paths.get(respath, "twpostcode.xml"));
            }
            if (countDownLatch != null) {
                countDownLatch.await();
                resetVIlage(vilsql);
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        if (cm != null) {
            cm.shutdown();
        }
    }

    @Bean(destroyMethod = "close")
    public CloseableHttpClient httpclient() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).setSoKeepAlive(true).build();
        cm.setDefaultSocketConfig(socketConfig);
        cm.setSocketConfig(new HttpHost(CrawlerRunner.HOST, 80), socketConfig);
        ConnectionConfig connectionConfig = ConnectionConfig.custom().setCharset(Consts.UTF_8).build();
        cm.setDefaultConnectionConfig(connectionConfig);
        cm.setConnectionConfig(new HttpHost(CrawlerRunner.HOST, 80), ConnectionConfig.DEFAULT);
        CookieStore cookieStore = new BasicCookieStore();
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.BEST_MATCH)
                .setExpectContinueEnabled(true)
                .setStaleConnectionCheckEnabled(true)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
                .build();
        return HttpClients.custom().setConnectionManager(cm).setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(defaultRequestConfig).build();
    }

    @Bean
    public XPath xpath() {
        new DBI(ds).open(Dao.class);
        return XPathFactory.newInstance().newXPath();
    }

    @Bean(destroyMethod = "close")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Dao dao() {
        DBI dbi = new DBI(ds);
        dbi.setSQLLog(Log4j2Log.DEFAULT);
        return dbi.open(Dao.class);
    }

    @Bean(destroyMethod = "close")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Dao readOnlyDao() {
        DBI dbi = new DBI(new ConnectionFactory() {

            @Override
            public Connection openConnection() throws SQLException {
                Connection conn = ds.getConnection();
                conn.setReadOnly(true);
                return conn;
            }
        });
        dbi.setSQLLog(Log4j2Log.DEFAULT);
        return dbi.open(Dao.class);
    }

    @Bean
    public SpelExpressionParser spel() {
        return new SpelExpressionParser(new SpelParserConfiguration(true, true));
    }

    private void resetVIlage(Path vilsql) throws Exception {
        try (Dao dao = context.getBean("dao", Dao.class)) {
            dao.createVillage();
        }
        int ra = 0;
        String sql = "";
        logger.debug("重設村里資料庫");
        try (BufferedReader br = Files.newBufferedReader(vilsql, StandardCharsets.UTF_8);
                Handle h = new DBI(ds).open()) {
            String strLine;
            h.begin();
            while ((strLine = br.readLine()) != null) {
                sql += strLine;
                if (strLine.endsWith(";")) {
                    h.execute(sql);
                    sql = "";
                    if (++ra % 1000 == 0) {
                        h.commit();
                        h.begin();
                        logger.debug("已新增村里資料{}筆", ra);
                    }
                }
            }
            h.commit();
            if (ra % 1000 > 0) {
                logger.debug("共新增村里資料{}筆", ra);
            }

        }
    }

    private Integer[] findNumbers(String src) {
        Matcher m = Pattern.compile("\\d+").matcher(src);
        List<Integer> ints = new ArrayList<>();
        while (m.find()) {
            ints.add(Integer.valueOf(m.group()));
        }
        Integer[] res = new Integer[ints.size()];
        return ints.toArray(res);
    }

    private class Post5Handler extends DefaultHandler {

        Post5 post5 = null;
        Pattern pTai = Pattern.compile("\\x{81FA}");//臺
        Pattern pSpacies = Pattern.compile("[\\x{3000}\\s]+");
        String oriInfo = "";
        String field = "";
        Consumer<Post5> consumer;

        public Post5Handler(Consumer<Post5> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ("Zip32".equals(qName)) {
                post5 = new Post5();
                oriInfo = "";
            }
            field = qName;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            field = "";
            if ("Zip32".equals(qName) && consumer != null && post5 != null) {
                consumer.accept(post5);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String str = length == 0 ? "" : new String(ch, start, length);
            if (post5 != null) {
                switch (field) {
                    case "Zip5":
                        post5.setZipcode(str);
                        break;
                    case "City":
                        post5.setCity(pTai.matcher(str).replaceAll("台"));
                        oriInfo = pSpacies.matcher(str).replaceAll("");
                        break;
                    case "Area":
                        post5.setArea(Address.normailize(str));
                        oriInfo += pSpacies.matcher(str).replaceAll("");
                        break;
                    case "Road":
                        post5.setAddrinfo(Address.normailize(str));
                        post5.setOriInfo(oriInfo + pSpacies.matcher(str).replaceAll(""));
                        break;
                    case "Scope":
                        post5.setTailInfo(pSpacies.matcher(str).replaceAll(""));
                        break;
                    default:
                }
            }
        }
    }

    private void initPost5(Path post5xml) throws Exception {
        logger.info("初始化郵遞區號資料庫");
        if (!Files.exists(post5xml)) {
            logger.info("下載3+2碼郵遞區號XML檔(UTF-8)…");
            URL website = new URL(appProperies.get("post5url"));
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            try (FileOutputStream fos = new FileOutputStream(post5xml.toFile())) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        }
        if (Files.exists(post5xml)) {
            try (Dao dao = context.getBean("dao", Dao.class)) {
                final IntSupplier rowAffected = new IntSecquencer(0);
                dao.dropPost5();
                dao.createPost5();
                //<editor-fold defaultstate="collapsed" desc="Patterns">
                final Pattern pa = Pattern.compile("(\\P{M}[\\x{5340}\\x{9109}\\x{53F0}\\x{5E02}\\x{93AE}\\x{5CF6}])"
                        + "[^\\x{5340}\\x{9109}\\x{53F0}\\x{5E02}\\x{93AE}\\x{5CF6}]");//區鄉台市鎮島
                final Pattern pSection = Pattern.compile("(\\d+)\\x{6BB5}");//xx段
                final Pattern pDecimal = Pattern.compile("\\d+");
                final Pattern pArgue = Pattern.compile("%\\d\\$d");
                final Pattern pLbrackets = Pattern.compile("[(\\x{FF08}]");
                final Pattern pRbrackets = Pattern.compile("[)\\x{FF09}]");
                final Pattern pDecimalHead = Pattern.compile("^\\d.*");
                final Pattern pLi = Pattern.compile("(\\P{M}{2,}[\\x{91CC}\\x{6751}])[^\\x{8857}]\\p{InCJKUnifiedIdeographs}{2,}");//[里村][^街]
                //</editor-fold>
                String strLine;
                dao.begin();
                SAXParserFactory.newInstance().newSAXParser().parse(Files.newInputStream(post5xml, StandardOpenOption.READ), new Post5Handler(new Consumer<Post5>() {

                    @Override
                    public void accept(Post5 post) {
                        //修正設定檔錯誤
                        if ("同平2弄".equals(post.getAddrinfo())) {
                            post.setAddrinfo("同平巷二弄");
                        }
                        String ai = post.getAddrinfo();
                        for (Village v : dao.findVillages(post.getCity(), post.getArea())) {
                            if (ai.startsWith(v.getVil())) {
                                post.setAddrinfo(ai.substring(0, v.getVil().length()) + ' ' + ai.substring(v.getVil().length()));
                            }
                        }
                        String build = "";
                        Matcher m = pSection.matcher(post.getAddrinfo());
                        post.setSec(m.find() ? Integer.parseInt(m.group(1), 10) : 0);
                        m = pLbrackets.matcher(post.getTailInfo());
                        if (m.find()) {
                            build = pRbrackets.matcher(post.getTailInfo().substring(m.start() + 1)).replaceAll("");
                            post.setTailInfo(post.getTailInfo().substring(0, m.start()));
                        }
                        if (pDecimalHead.matcher(post.getTailInfo()).matches()) {
                            post.setTailInfo("　" + post.getTailInfo());
                        }
                        Map<String, String> tailPatternMap = tailPatternMap();
                        String ptn = tailPatternMap.get(pDecimal.matcher(post.getTailInfo()).replaceAll("0"));
                        if (ptn == null) {
                            logger.fatal("無法解析帶有 \"{}\" 的[{}]\n樣式[{}]", post.getTailInfo(), post.toString(),
                                    pDecimal.matcher(post.getTailInfo()).replaceAll("0"));
                        } else {
                            Object[] values = pArgue.matcher(ptn).find()
                                    ? findNumbers(post.getTailInfo()) : null;
                            if (values != null && values.length > 2) {
                                ptn = modify2NeighborPattern(post.getTailInfo(), ptn, values);
                            }
                            String[] ptns = values == null ? ptn.split(";") : String.format(ptn, values).split(";");
                            if (build != null && !build.isEmpty()) {
                                ptns[0] = ptns[0] + " and build == '" + build + "'";
                            }
                            post.setExpress(ptns[0]);
                            post.setLane("null".equals(ptns[1]) ? null : ptns[1]);
                            post.setAlley("null".equals(ptns[2]) ? null : ptns[2]);
                            post.setParnums("null".equals(ptns[3]) ? null : new BigDecimal(ptns[3]).setScale(3).floatValue());
                            post.setParnume("null".equals(ptns[4]) ? null : new BigDecimal(ptns[4]).setScale(3).floatValue());
                            post.setFloors("null".equals(ptns[5]) ? null : Integer.parseInt(ptns[5], 10));
                            post.setFloore("null".equals(ptns[6]) ? null : Integer.parseInt(ptns[6], 10));
                            post.setBoundary(ptns[7]);
                            dao.insertPost5(post);
                            int ra = rowAffected.getAsInt() + 1;
                            if (ra % 1000 == 0) {
                                dao.commit();
                                dao.begin();
                                logger.debug("已新增3+2碼郵遞區號{}筆", ra);
                            }
                        }
                        tailPatternMap.clear();
                    }
                }));
                dao.commit();
                int ra = rowAffected.getAsInt();
                if (ra % 1000 > 0) {
                    logger.debug("共新增3+2碼郵遞區號{}筆", ra);
                }
            }
            try (BufferedWriter wr = Files.newBufferedWriter(Paths.get(appProperies.get("resfolder"), "post5.sql"),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    Dao dao = context.getBean("readOnlyDao", Dao.class)) {
                Iterator<Post5> posts = dao.findAllPost5();
                while (posts.hasNext()) {
                    Post5 post = posts.next();
                    wr.write("insert into post5(zipcode,city,area,oriInfo,addrinfo,sec,tailinfo,lane,alley,parnums,parnume,floors,floore,express,boundary) values('");
                    wr.write(post.getZipcode());
                    wr.write("',\n'");
                    wr.write(post.getCity());
                    wr.write("','");
                    wr.write(post.getArea());
                    wr.write("','");
                    wr.write(post.getOriInfo());
                    wr.write("','");
                    wr.write(post.getAddrinfo());
                    wr.write("',");
                    wr.write(post.getSec() == null ? "null" : String.valueOf(post.getSec()));
                    wr.write(",");
                    if (post.getTailInfo() == null) {
                        wr.write("null");
                    } else {
                        wr.write("'");
                        wr.write(post.getTailInfo());
                        wr.write("',");
                    }
                    wr.write(post.getLane() == null ? "null," : "'" + post.getLane() + "',");
                    wr.write(post.getAlley() == null ? "null," : "'" + post.getAlley() + "',");
                    wr.write(post.getParnums() == null ? "null," : String.format("%.3f,", post.getParnums()));
                    wr.write(post.getParnume() == null ? "null," : String.format("%.3f,", post.getParnume()));
                    wr.write(post.getFloors() == null ? "null," : String.format("%d,", post.getFloors()));
                    wr.write(post.getFloore() == null ? "null," : String.format("%d,", post.getFloore()));
                    if (post.getExpress() == null) {
                        wr.write("null,");
                    } else {
                        wr.write("'");
                        wr.write(post.getExpress().replaceAll("'", "''"));
                        wr.write("',");
                    }
                    if (post.getBoundary() == null) {
                        wr.write("null);");
                    } else {
                        wr.write("'");
                        wr.write(post.getBoundary().replaceAll("'", "''"));
                        wr.write("');\n");
                    }

                }
            }
        } else {
            throw new RuntimeException("3+2碼郵遞區號XML檔(" + post5xml.toAbsolutePath() + ")不存在");
        }
    }

    private void resetPost5(Path post5sql) throws Exception {
        try (Dao dao = context.getBean("dao", Dao.class)) {
            dao.createPost5();
        }
        int ra = 0;
        String sql = "";
        logger.debug("重設郵遞區號資料庫");
        try (BufferedReader br = Files.newBufferedReader(post5sql, StandardCharsets.UTF_8);
                Handle h = new DBI(ds).open()) {
            String strLine;
            h.begin();
            while ((strLine = br.readLine()) != null) {
                sql += strLine;
                if (strLine.endsWith(";")) {
                    h.execute(sql);
                    sql = "";
                    if (++ra % 1000 == 0) {
                        h.commit();
                        h.begin();
                        logger.debug("已新增郵遞區號設定{}筆", ra);
                    }
                }
            }
            h.commit();
            if (ra % 1000 > 0) {
                logger.debug("共新增郵遞區號設定{}筆", ra);
            }
        }
    }

    private boolean validDb() {
        try (Dao dao = context.getBean("readOnlyDao", Dao.class)) {
            ExpressionParser parser = spel();
            Address addr = new Address();
            addr.setLane(1);
            addr.setAlley(1);
            addr.setNumber(1);
            addr.setAddnum(1);
            addr.setFloor(1);
            addr.setBuild("x");
            Iterator<Post5> posts = dao.findAllPost5();
            EvaluationContext spelCtx = new StandardEvaluationContext(addr);
            boolean valided = true;
            while (posts.hasNext()) {
                Post5 post = posts.next();
                try {
                    Expression exp = parser.parseExpression(post.getExpress());
                    exp.getValue(spelCtx, Boolean.class);
                    exp = parser.parseExpression(post.getBoundary());
                    exp.getValue(spelCtx, Boolean.class);
                } catch (SpelParseException | SpelEvaluationException ex) {
                    logger.error("SpelParseException:" + post.toString() + "\n" + post.getBoundary(), ex);
                    valided = false;
                    break;
                }
            }
            return valided;
        }
    }

    private String modify2NeighborPattern(String tail, String ptn, Object... value) {
        int pre, nxt;
        switch (tail) {
            case "單0之0號至0之0號":
            case "雙0之0號至0之0號":
                if (value[0].equals(value[2])) {
                    ptn = "addnum > " + (value[1].toString()) + " and addnum < " + (value[3].toString()) + " and " + ptn;
                }
            case "單0之0號至0號":
            case "單0之0號至0號含附號全":
            case "雙0之0號至0號":
            case "雙0之0號至0號含附號全":
                pre = (Integer) value[0];
                nxt = (Integer) value[2];
                return nxt - pre == 2 ? "laneAlley == 0 and " + ptn : ptn;
            case "單0號至0之0號":
            case "單0號至0號":
            case "單0號至0號含附號全":
            case "單0號至0號0樓以上":
            case "雙0號至0之0號":
            case "雙0號至0號":
            case "雙0號至0號含附號全":
                pre = (Integer) value[0];
                nxt = (Integer) value[1];
                return nxt - pre == 2 ? "laneAlley == 0 and " + ptn : ptn;
            case "連0之0號至0之0號":
                if (value[0].equals(value[2])) {
                    ptn = "addnum > " + (value[1].toString()) + " and addnum < " + (value[3].toString()) + " and " + ptn;
                }
            case "連0之0號至0號":
                pre = (Integer) value[0];
                nxt = (Integer) value[2];
                return nxt - pre == 1 ? "laneAlley == 0 and " + ptn : ptn;
            case "連0號至0之0號":
            case "連0號至0號":
            case "連0號至0號0樓以上":
                pre = (Integer) value[0];
                nxt = (Integer) value[1];
                return nxt - pre == 1 ? "laneAlley == 0 and " + ptn : ptn;
            case "　0之0至之0號":
            case "　0之0至之0號0樓以上":
            case "　0巷連0之0至之0號":
                return "addnum > " + (value[1].toString()) + " and " + ptn;
            default:
                return ptn;
        }
    }

    public Map<String, String> tailPatternMap() {
        Map<String, String> em = new HashMap<>();
        //<editor-fold defaultstate="collapsed" desc="填寫資料">
        em.put("全", "true;null;null;null;null;null;null;false");
        em.put("單0之0號以上", "laneNum %% 2 == 1 and (laneNum > %1$d or (laneAlley == 0 and number == %1$d and addnum >= %2$d))"
                + ";null;null;%1$d.%2$03d;%1$d.999;null;null;laneAlley == %1$d");
        em.put("單0之0號以下", "laneNum %% 2 == 1 and ((laneAlley>0 and laneAlley < %1$d) or (laneAlley == 0 and number < %1$d) or (laneAlley == 0 and number == %1$d and addnum <= %2$d))"
                + ";null;null;%1$d;%1$d.%2$03d;null;null;laneAlley == %1$d");
        em.put("單0之0號至0之0號", "laneNum %% 2 == 1 and ((laneAlley == 0 and number == %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum < %3$d) or (laneAlley < %3$d and number == %3$d and addnum <= %4$d))"
                + ";null;null;%1$d.%2$03d;%3$d.%4$03d;null;null;%1$d != %3$d and (laneAlley == %1$d or laneAlley == %3$d)");
        em.put("單0之0號至0號", "laneNum %% 2 == 1 and ((laneAlley == 0 and number == %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum <= %3$d))"
                + ";null;null;%1$d.%2$03d;%1$d.999;null;null;(laneAlley == %1$d or laneAlley == %3$d)");
        em.put("單0之0號至0號含附號全", "laneNum %% 2 == 1 and ((laneAlley == 0 and number == %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum < %3$d) or number == %3$d)"
                + ";null;null;%1$d.%2$03d;%3$d.999;null;null;(laneAlley == %1$d or laneAlley == %3$d)");
        em.put("單0巷以上", "laneNum %% 2 == 1 and (lane >= %1$d or (lane == 0 and alleyNum > %1$d));^%1$d^;null;null;null;null;null"
                + ";lane == 0 and alleyNum == %1$d");
        em.put("單0巷以下", "laneNum %% 2 == 1 and ((lane > 0  and lane <= %1$d) or (lane == 0 and alleyNum < %1$d));^%1$d^;null;null;null;null;null"
                + ";lane == 0 and alleyNum == %1$d");
        em.put("單0巷至0之0號", "laneNum %% 2 == 1 and (lane == %1$d or (laneNum >%1$d and laneNum < %2$d) or (laneAlley == 0 and number == %2$d and addnum <= %3$d))"
                + ";^%1$d^;null;%2$d;%2$d.%3$03d;null;null;((lane == 0 and alleyNum == %1$d) or laneAlley == %2$d)");
        em.put("單0巷至0巷", "laneNum %% 2 == 1 and (lane == %1$d or (laneNum > %1$d and laneNum < %2$d) or lane == %2$d)"
                + ";^%1$d^%2$d;null;null;null;null;null;lane == 0 and (alleyNum == %1$d or alleyNum == %2$d)");
        em.put("單0巷至0號", "laneNum %% 2 == 1 and (lane == %1$d or (laneNum > %1$d and laneNum <= %2$d))"
                + ";^%1$d^;null;null;null;null;null;((lane == 0 and alleyNum == %1$d) or laneAlley == %2$d)");
        em.put("單0巷至0號含附號全", "laneNum %% 2 == 1 and (lane == %1$d or (laneNum > %1$d and laneNum < %2$d) or (laneAlley == 0 and number == %2$d))"
                + ";^%1$d^;null;%2$d;%2$d.999;null;null;((lane == 0 and alleyNum == %1$d) or laneAlley == %2$d)");
        em.put("單0弄以上", "alleyNum %% 2 == 1 and (alley == %1$d or alleyNum > %1$d);null;^%1$d^;null;null;null;null;(lane == %1$d or (alley == 0 and number == %1$d))");
        em.put("單0弄以下", "alleyNum %% 2 == 1 and (alley == %1$d or alleyNum < %1$d);null;^%1$d^;null;null;null;null;(lane == %1$d or (alley == 0 and number == %1$d))");
        em.put("單0號以上", "laneNum %% 2 == 1 and laneNum >= %1$d;null;null;null;null;null;null;laneAlley == %1$d");
        em.put("單0號以下", "laneNum %% 2 == 1 and laneNum <= %1$d;null;null;null;null;null;null;laneAlley == %1$d");
        em.put("單0號含附號以下", "laneNum %% 2 == 1 and (laneNum < %1$d or number == %1$d);null;null;%1$d;%1$d.999;null;null;laneAlley == %1$d");
        em.put("單0號以下0樓以上", "laneNum %% 2 == 1 and laneNum <= %1$d and floor>= %2$d;null;null;null;null;%2$d;999;laneAlley == %1$d and floor>= %2$d");
        em.put("單0號至0之0號", "laneNum %% 2 == 1 and laneNum >= %1$d and (laneNum < %2$d or (laneAlley< %2$d and number == %2$d and addnum <= %3$d))"
                + ";null;null;%2$d;%2$d.%3$03d;null;null;(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("單0號至0巷", "laneNum %% 2 == 1 and ((laneNum >= %1$d and laneNum < %2$d) or (lane == %2$d));^%2$d^;null;null;null;null;null"
                + ";(laneAlley == %1$d or (lane == 0 and alleyNum == %2$d))");
        em.put("單0號至0號", "laneNum %% 2 == 1 and laneNum >= %1$d and laneNum <= %2$d;null;null;null;null;null;null"
                + ";(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("單0號至0號含附號全", "laneNum %% 2 == 1 and laneNum >= %1$d and (laneNum < %2$d or number == %2$d)"
                + ";null;null;%2$d;%2$d.999;null;null;(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("單0號至0號0樓以上", "laneNum %% 2 == 1 and laneNum >= %1$d and laneNum <= %2$d and floor >= %3$d"
                + ";null;null;null;null;%3$d;999;floor >= %3$d and (laneAlley == %1$d or laneAlley == %2$d)");
        em.put("單全", "laneNum % 2 == 1;null;null;null;null;null;null;false");
        em.put("雙0之0號以上", "laneNum %% 2 == 0 and (laneNum > %1$d or (laneAlley == 0 and number == %1$d and addnum >= %2$d))"
                + ";null;null;%1$d.%2$03d;%1$d.999;null;null;laneAlley == %1$d");
        em.put("雙0之0號以下", "laneNum %% 2 == 0 and ((laneAlley>0 and laneAlley < %1$d) or (laneAlley == 0 and number < %1$d) or (laneAlley == 0 and number == %1$d and addnum <= %2$d))"
                + ";null;null;%1$d;%1$d.%2$03d;null;null;laneAlley == %1$d");
        em.put("雙0之0號至0之0號", "laneNum %% 2 == 0 and ((laneAlley == 0 and number == %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum < %3$d) or (laneAlley < %3$d and number == %3$d and addnum <= %4$d))"
                + ";null;null;%1$d.%2$03d;%3$d.%4$03d;null;null;%1$d != %3$d and (laneAlley == %1$d or laneAlley == %3$d)");
        em.put("雙0之0號至0巷", "laneNum %% 2 == 0 and ((laneAlley == 0 and number ==  %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum < %3$d) or lane == %3$d)"
                + ";^%3$d^;null;%1$d.%2$03d;%1$d.999;null;null;(laneAlley == %1$d or (lane == 0 and alleyNum == %3$d))");
        em.put("雙0之0號至0號", "laneNum %% 2 == 0 and ((laneAlley == 0 and number ==  %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum <= %3$d))"
                + ";null;null;%1$d.%2$03d;%1$d.999;null;null;(laneAlley == %1$d or laneAlley == %3$d)");
        em.put("雙0之0號至0號含附號全", "laneNum %% 2 == 0 and ((laneAlley == 0 and number == %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum < %3$d) or number == %3$d)"
                + ";null;null;%1$d.%2$03d;%3$d.999;null;null;(laneAlley == %1$d or laneAlley == %3$d)");
        em.put("雙0巷以上", "laneNum %% 2 == 0 and (lane == %1$d or laneNum > %1$d);^%1$d^;null;null;null;null;null;lane == 0 and alleyNum == %1$d");
        em.put("雙0巷以下", "laneNum %% 2 == 0 and (lane == %1$d or laneNum < %1$d);^%1$d^;null;null;null;null;null;lane == 0 and alleyNum == %1$d");
        em.put("雙0巷至0之0號", "laneNum %% 2 == 0 and (lane == %1$d or (laneNum >%1$d and laneNum < %2$d) or (laneAlley == 0 and number == %2$d and addnum <= %3$d))"
                + ";^%1$d^;null;%2$d;%2$d.%3$03d;null;null;((lane == 0 and alleyNum == %1$d) or laneAlley == %2$d)");
        em.put("雙0巷至0巷", "laneNum %% 2 == 0 and (lane == %1$d or (laneNum > %1$d and laneNum < %2$d) or lane == %2$d);^%1$d^%2$d^;null;null;null;null;null"
                + ";lane == 0 and (alleyNum == %1$d or alleyNum == %2$d)");
        em.put("雙0巷至0號", "laneNum %% 2 == 0 and laneNum >= %1$d and (laneNum < %2$d or (laneAlley < %2$d and number == %2$d and addnum == 0))"
                + ";^%1$d^;null;null;null;null;null;((lane == 0 and alleyNum == %1$d) or laneAlley == %2$d)");
        em.put("雙0巷至0號含附號全", "laneNum %% 2 == 0 and (lane == %1$d or (laneNum > %1$d and laneNum < %2$d) or (laneAlley == 0 and number == %2$d))"
                + ";^%1$d^;null;%2$d;%2$d.999;null;null;((lane == 0 and alleyNum == %1$d) or laneAlley == %2$d)");
        em.put("雙0號以上", "laneNum %% 2 == 0 and laneNum >= %1$d;null;null;null;null;null;null;laneAlley == %1$d");
        em.put("雙0號以下", "laneNum %% 2 == 0 and laneNum <= %1$d;null;null;null;null;null;null;laneAlley == %1$d");
        em.put("雙0號含附號以下", "laneNum %% 2 == 0 and (laneNum < %1$d or number == %1$d);null;null;%1$d;%1$d.999;null;null;laneAlley == %1$d");
        em.put("雙0號至0之0號", "laneNum %% 2 == 0 and laneNum >= %1$d and (laneNum < %2$d or (laneAlley < %2$d and number == %2$d and addnum <= %3$d))"
                + ";null;null;%2$d;%2$d.%3$03d;null;null;(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("雙0號至0巷", "laneNum %% 2 == 0 and ((laneNum >= %1$d and laneNum < %2$d) or (lane == %2$d));^%2$d^;null;null;null;null;null"
                + ";(laneAlley == %1$d or (lane == 0 and alleyNum == %2$d))");
        em.put("雙0號至0號", "laneNum %% 2 == 0 and laneNum >= %1$d and laneNum <= %2$d;null;null;null;null;null;null;(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("雙0號至0號含附號全", "laneNum %% 2 == 0 and laneNum >= %1$d and (laneNum < %2$d or number == %2$d)"
                + ";null;null;%2$d;%2$d.999;null;null;(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("雙全", "laneNum % 2 == 0;null;null;null;null;null;null;false");
        em.put("連0之0號以上", "(laneNum > %1$d or (laneAlley == 0 and number == %1$d and addnum >= %2$d));null;null;%1$d.%2$03d;%1$d.999;null;null;laneAlley == %1$d");
        em.put("連0之0號以下", "((laneAlley>0 and laneAlley < %1$d) or (laneAlley == 0 and number < %1$d) or (laneAlley == 0 and number == %1$d and addnum <= %2$d))"
                + ";null;null;%1$d;%1$d.%2$03d;null;null;laneAlley == %1$d");
        em.put("連0之0號至0之0號", "((laneAlley == 0 and number == %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum < %3$d) or (laneAlley < %3$d and number == %3$d and addnum <= %4$d))"
                + ";null;null;%1$d.%2$03d;%3$d.%4$03d;null;null;%1$d != %3$d and (laneAlley == %1$d or laneAlley == %3$d)");
        em.put("連0之0號至0號", "((laneAlley == 0 and number ==  %1$d and addnum >= %2$d) or (laneNum > %1$d and laneNum <= %3$d))"
                + ";null;null;%1$d.%2$03d;%1$d.999;null;null;(laneAlley == %1$d or laneAlley == %3$d)");
        em.put("連0巷以上", "(lane == %1$d or laneNum > %1$d);^%1$d^;null;null;null;null;null;lane == 0 and alleyNum == %1$d");
        em.put("連0巷以下", "(lane == %1$d or laneNum < %1$d);^%1$d^;null;null;null;null;null;lane == 0 and alleyNum == %1$d");
        em.put("連0巷至0之0號", "(lane == %1$d or (laneNum >%1$d and laneNum < %2$d) or (laneAlley == 0 and number == %2$d and addnum <= %3$d))"
                + ";^%1$d^;null;%2$d;%2$d.%3$03d;null;null;((lane == 0 and alleyNum == %1$d) or laneAlley == %2$d)");
        em.put("連0巷至0巷", "(lane == %1$d or (laneNum > %1$d and laneNum < %2$d) or lane == %2$d);^%1$d^%2$d^;null;null;null;null;null"
                + ";lane == 0 and (alleyNum == %1$d or alleyNum == %2$d)");
        em.put("連0巷至0號", "laneNum >= %1$d and (laneNum < %2$d or (laneAlley < %2$d and number == %2$d and addnum == 0))"
                + ";^%1$d^;null;null;null;null;null;((lane == 0 and alleyNum == %1$d) or laneAlley == %2$d)");
        em.put("連0弄至0之0號", "(alley == %1$d or (alleyNum > %1$d and alleyNum < %2$d) or (laneAlley == 0 and number == %2$d and addnum <= %3$d))"
                + ";null;^%1$d^;%2$d;%2$d.%3$03d;null;null;((alley == 0 and (lane == %1$d or number == %1$d)) or laneAlley == %2$d)");
        em.put("連0號以上", "laneNum >= %1$d;null;null;null;null;null;null;laneAlley == %1$d");
        em.put("連0號以下", "laneNum <= %1$d;null;null;null;null;null;null;laneAlley == %1$d");
        em.put("連0號含附號以下", "(laneNum < %1$d or number == %1$d);null;null;%1$d;%1$d.999;null;null;laneAlley == %1$d");
        em.put("連0號至0之0號", "laneNum >= %1$d and (laneNum < %2$d or (laneAlley < %2$d and number == %2$d and addnum <= %3$d))"
                + ";null;null;%2$d;%2$d.%3$03d;null;null;(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("連0號至0巷", "((laneNum >= %1$d and laneNum < %2$d) or (lane == %2$d));^%2$d^;null;null;null;null;null"
                + ";(laneAlley == %1$d or (lane == 0 and alleyNum == %2$d))");
        em.put("連0號至0號", "laneNum >= %1$d and laneNum <= %2$d;null;null;null;null;null;null;(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("連0號至0號含附號全", "laneNum >= %1$d and (laneNum < %2$d or number == %2$d);null;null;%2$d;%2$d.999;null;null"
                + ";(laneAlley == %1$d or laneAlley == %2$d)");
        em.put("連0號至0號0樓以上", "laneNum >= %1$d and laneNum <= %2$d and floor >= %3$d;null;null;null;null;%3$d;999"
                + ";floor >= %3$d and (laneAlley == %1$d or laneAlley == %2$d)");
        em.put("　0之0至之0號", "number == %1$d and addnum >= %2$d and addnum <= %3$d;null;null;%1$d.%2$03d;%1$d.%3$03d;null;null;false");
        em.put("　0之0至之0號0樓以上", "number == %1$d and addnum >= %2$d and addnum <= %3$d and floor >= %4$d"
                + ";null;null;%1$d.%2$03d;%1$d.%3$03d;%4$d;999;false");
        em.put("　0之0號", "laneAlley == 0 and number == %1$d and addnum == %2$d;null;null;%1$d.%2$03d;%1$d.%2$03d;null;null;laneAlley == %1$d");
        em.put("　0之0號及以上附號", "laneAlley == 0 and number == %1$d and addnum >= %2$d;null;null;%1$d.%2$03d;%1$d.999;null;null;laneAlley == %1$d");
        em.put("　0巷0之0號", "lane == %1$d and number == %2$d and addnum == %3$d;^%1$d^;null;%2$d.%3$03d;%2$d.%3$03d;null;null;lane == 0 and alleyNum == %1$d");
        em.put("　0巷0弄0號", "lane == %1$d and alley == %2$d and number == %3$d;^%1$d^;^%2$d^;null;null;null;null;false");
        em.put("　0巷0弄全", "lane == %1$d and alleyNum == %2$d;^%1$d^;^%2$d^;null;null;null;null;laneAlley == 0 and number == %1$d");
        em.put("　0巷0弄單0號以上", "number %% 2 == 1 and lane == %1$d and alley == %2$d and number %% 2 == 1 and number >= %3$d"
                + ";^%1$d^;^%2$d^;null;null;null;null;false;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄單0號至0號", "number %% 2 == 1 and lane == %1$d and alley == %2$d and number %% 2 == 1 and (number >= %3$d or number <= %4$d)"
                + ";^%1$d^;^%2$d^;null;null;null;null;false;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄單0號以下", "number %% 2 == 1 and lane == %1$d and alley == %2$d and number %% 2 == 1 and number <= %3$d"
                + ";^%1$d^;^%2$d^;null;null;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄單全", "lane == %1$d and alley == %2$d and number %%2 == 1;^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄連0號以上", "lane == %1$d and alley == %2$d and number >= %3$d;^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄連0號以下", "lane == %1$d and alley == %2$d and number <= %3$d;^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄雙0號以上", "lane == %1$d and alley == %2$d and number %% 2 == 0 and number >= %3$d;^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄雙0號至0號", "lane == %1$d and alley == %2$d and number %% 2 == 0 and (number >= %3$d or number <= %4$d);^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄雙0號以下", "lane == %1$d and alley == %2$d and number %% 2 == 0 and number <= %3$d;^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0弄雙全", "lane == %1$d and alley == %2$d and number %% 2 == 0;^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷0號", "lane == %1$d and number == %2$d;^%1$d^;null;null;null;null;null"
                + ";((lane == 0 and alleyNum == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷0號含附號", "lane == %1$d and (alleyNum < %2$d or number == %2$d);^%1$d^;null;%2$d;%2$d.999;null;null"
                + ";((lane == 0 and alleyNum == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷全", "laneNum == %1$d;^%1$d^;null;null;null;null;null;lane == 0 and alleyNum == %1$d");
        em.put("　0巷單0之0號以下", "lane == %1$d and alleyNum %% 2 == 1 and ((alley > 0 and alley < %2$d) or (alley == 0 and number < %2$d) or (laneAlley == 0 and number == %2$d and addnum <%3$d))"
                + ";^%1$d^;null;%2$d;%2$d.%3$03d;null;null;((lane == 0 and alleyNum == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷單0弄以上", "lane == %1$d and alleyNum %% 2 == 1 and (alley == %2$d or alleyNum > %2$d)"
                + ";^%1$d^;^%2$d^;null;null;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷單0弄以下", "lane == %1$d and alleyNum %% 2 == 1 and (alley == %2$d or alleyNum < %2$d)"
                + ";^%1$d^;^%2$d^;null;null;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷單0弄至0弄", "lane == %1$d and alleyNum %% 2 == 1 and ((alley >= %2$d and alley <= %3$d) or (alley == 0 and number > %2$d and number < %3$d))"
                + ";^%1$d^;^%2$d^%3$d^;null;null;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and (number == %2$d or number == %3$d)))");
        em.put("　0巷單0號以上", "lane == %1$d and alleyNum %% 2 == 1 and alleyNum >= %2$d;^%1$d^;null;null;null;null;null"
                + ";lane == 0 and alleyNum == %1$d");
        em.put("　0巷單0號以下", "lane == %1$d and alleyNum %% 2 == 1 and alleyNum <= %2$d;^%1$d^;null;null;null;null;null"
                + ";lane == 0 and alleyNum == %1$d");
        em.put("　0巷單0號至0號", "lane == %1$d and alleyNum %% 2 == 1 and alleyNum >= %2$d and alleyNum <= %3$d"
                + ";^%1$d^;null;null;null;null;null;((lane == 0 and alleyNum == %1$d) or (lane == %1$d and (alley == %2$d or alley == %3$d)))");
        em.put("　0巷單全", "lane == %1$d and alleyNum %% 2 == 1;^%1$d^;null;null;null;null;null;lane == 0 and alleyNum == %1$d");
        em.put("　0巷連0之0至之0號", "lane == %1$d and number == %1$d and addnum >= %2$d and addnum <= %3$d"
                + ";^%1$d^;null;%2$d;%2$d.%3$03d;null;null;false");
        em.put("　0巷連0之0號以上", "lane == %1$d and (alleyNum > %1$d or (alleyNum == 0 and number == %1$d and addnum >= %2$d))"
                + ";^%1$d^;null;%2$d;%2$d.%3$03d;null;null;((lane == 0 and alleyNum == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷連0之0號以下", "lane == %1$d and ((alley > 0 and alley <%2$d) or (alley == 0 and number < %2$d) or (alley == 0  and number == %2$d and addnum <%3$d))"
                + ";^%1$d^;null;%2$d;%2$d.%3$03d;null;null;((lane == 0 and alleyNum == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷連0弄以上", "lane == %1$d and (alley == %2$d or alleyNum >%2$d);^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷連0弄以下", "lane == %1$d and (alley == %2$d or alleyNum <%2$d);^%1$d^;^%2$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷連0弄至0弄", "lane == %1$d and ((alley >= %2$d and alley <= %3$d) or (alley == 0 and number > %2$d and number < %3$d))"
                + ";^%1$d^;^%2$d^%3$d^;null;null;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and (number == %2$d or number == %3$d)))");
        em.put("　0巷連0弄至0號", "lane == %1$d and ((alley >= %2$d and alley <= %3$d) or (alley == 0 and number <= %3$d))"
                + ";^%1$d^;^%2$d^;null;null;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d) or (lane == %1$d and alley == %3$d))");
        em.put("　0巷連0號以上", "lane == %1$d and alleyNum >= %2$d;^%1$d^;null;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷連0號以下", "lane == %1$d and alleyNum <= %2$d;^%1$d^;null;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷連0號至0之0號", "lane == %1$d and alleyNum >= %2$d and (alleyNum < %3$d or (alley < %3$d and number == %3$d and addnum <= %4$d))"
                + ";^%1$d^;null;%3$d;%3$d.%4$03d;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and (alley == %2$d or alley == %3$d)))");
        em.put("　0巷連0號至0號", "lane == %1$d and alleyNum >= %2$d and alleyNum <= %3$d;^%1$d^;null;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and (alley == %2$d or alley == %3$d)))");
        em.put("　0巷雙0之0號以上", "lane == %1$d and alleyNum %% 2 == 0 and (alleyNum > %2$d or (alleyNum == 0 and number == %2$d and addnum >= %3$d))"
                + ";^%1$d^;null;%2$d.%3$03d;%2$d.999;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷雙0之0號以下", "lane == %1$d and alleyNum %% 2 == 0 and ((alley > 0 and alley < %2$d) or (alley == 0 and number < %2$d) or (alley == 0 and number == %2$d and addnum <%3$d))"
                + ";^%1$d^;null;%2$d;%2$d.%3$03d;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷雙0弄以上", "lane == %1$d and alleyNum %% 2 == 0 and (alley == %2$d or alleyNum > %2$d)"
                + ";^%1$d^;^%2$d^;null;null;null;null;((laneAlley == 0 && number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷雙0弄以下", "lane == %1$d and alleyNum %% 2 == 0 and (alley == %2$d or alleyNum < %2$d)"
                + ";^%1$d^;^%2$d^;null;null;null;null;((laneAlley == 0 && number == %1$d) or (lane == %1$d and alley == 0 and number == %2$d))");
        em.put("　0巷雙0弄至0弄", "lane == %1$d and alleyNum %% 2 == 0 and ((alley >= %2$d and alley <= %3$d) or (alley == 0 and number > %2$d and number < %3$d))"
                + ";^%1$d^;^%2$d^%3$d^;null;null;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == 0 and (number == %2$d or number == %3$d)))");
        em.put("　0巷雙0號以上", "lane == %1$d and alleyNum %% 2 == 0 and alleyNum >= %2$d;^%1$d^;null;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷雙0號以下", "lane == %1$d and alleyNum %% 2 == 0 and alleyNum <= %2$d;^%1$d^;null;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and alley == %2$d))");
        em.put("　0巷雙0號至0之0號", "lane == %1$d and alleyNum %% 2 == 0 and alleyNum >= %2$d and (alleyNum < %3$d or (alley < %3$d and number == %3$d and addnum <= %4$d))"
                + ";^%1$d^;null;%3$d;%3$d.%4$03d;null;null;((laneAlley == 0 and number == %1$d) or (lane == %1$d and (alley == %2$d or alley == %3$d)))");
        em.put("　0巷雙0號至0弄", "lane ==  %1$d and alleyNum %% 2 == 0 and alleyNum >= %2$d and alleyNum <= %3$d;^%1$d^;^%3$d^;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and (alley == %2$d or (alley == 0 and number == %3$d))))");
        em.put("　0巷雙0號至0號", "lane ==  %1$d and alleyNum %% 2 == 0 and alleyNum >= %2$d and alleyNum <= %3$d;^%1$d^;null;null;null;null;null"
                + ";((laneAlley == 0 and number == %1$d) or (lane == %1$d and (alley == %2$d or alley == %3$d)))");
        em.put("　0巷雙全", "lane == %1$d and alleyNum %% 2 == 0;^%1$d^;null;null;null;null;null;lane == 0 and alleyNum == %1$d");
        em.put("　0弄全", "alleyNum == %1$d;null;^%1$d^;null;null;null;null;alley == 0 and number == %1$d");
        em.put("　0弄單0之0號以上", "alley == %1$d and number %% 2 == 1 and (number > %2$d or (number == %2$d and addnum >= %3$d))"
                + ";null;^%1$d^;%2$d.%3$03d;%2$d.999;null;null;alley == 0 and number == %1$d");
        em.put("　0弄單0號以下", "alley == %1$d and number %% 2 == 1 and number <= %2$d;null;^%1$d^;null;null;null;null;alley == 0 and number == %1$d");
        em.put("　0弄雙0號以上", "alley == %1$d and number %% 2 == 0 and number >= %2$d;null;^%1$d^;null;null;null;null;alley == 0 and number == %1$d");
        em.put("　0弄雙0號以下", "alley == %1$d and number %% 2 == 0 and number <= %2$d;null;^%1$d^;null;null;null;null;alley == 0 and number == %1$d");
        em.put("　0號", "laneAlley == 0 and number == %1$d;null;null;null;null;null;null;laneAlley == %1$d");
        em.put("　0號含附號", "laneAlley == 0 and number == %1$d;null;null;%1$d;%1$d.999;null;null;laneAlley == %1$d");
        em.put("　0號0樓", "laneAlley == 0 and number == %1$d and floor == %2$d;null;null;null;null;%2$d;%2$d;laneAlley == %1$d and floor == %2$d");
        em.put("　0號0樓以上", "laneAlley == 0 and number == %1$d and floor >= %2$d;null;null;null;null;%2$d;999;laneAlley == %1$d and floor >= %2$d");
        em.put("　0號0樓以下", "laneAlley == 0 and number == %1$d and floor <= %2$d;null;null;null;null;0;%2$d;laneAlley == %1$d and floor <= %2$d");
        em.put("　0號0至0樓", "laneAlley == 0 and number == %1$d and floor >= %2$d and floor <= %3$d;null;null;null;null;%2$d;%3$d;laneAlley == %1$d and floor >= %2$d and floor <= %3$d");
        em.put("　0號北棟", "laneAlley == 0 and number == %1$d and build == '北棟';null;null;null;null;null;null;false");
        em.put("　0號南棟", "laneAlley == 0 and number == %1$d and build == '南棟';null;null;null;null;null;null;false");
        em.put("　0號地下0樓", "laneAlley == 0 and number == %1$d and floor == -%2$d;null;null;null;null;-%2$d;-%2$d;laneAlley == %1$d and floor == -%2$d");
        em.put("　0號至0之0號", "laneAlley == 0 and ((number >= %1$d and number < %2$d) or (number == %2$d and addnum <= %3$d))"
                + ";null;null;%2$d;%2$d.%3$03d;null;null;(laneAlley == %1$d or laneAlley == %2$d )");
        em.put("　0鄰", "ln == %1$d;null;null;null;null;null;null;false");
        em.put("　0附號全", "laneAlley == 0 and number == %1$d and addnum > 0;null;null;%1$d.001;%1$d.999;null;null;false");
        em.put("　0樓全", "floor == %1$d ;null;null;null;null;%1$d;%1$d;false");
        //</editor-fold>
        return em;
    }
}
