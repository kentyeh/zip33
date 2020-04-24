package twzip.model;

import java.io.BufferedReader;
import org.apache.logging.log4j.LogManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Kent Yeh
 */
public class PreDataUtil {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(PreDataUtil.class);
    private static final Pattern maptn = Pattern.compile("<lucene>(.+)</lucene>");
    private static final Map<Pattern, String> replaces = new LinkedHashMap<>();

    static {
        replaces.put(Pattern.compile("０"), "0");
        replaces.put(Pattern.compile("１"), "1");
        replaces.put(Pattern.compile("２"), "2");
        replaces.put(Pattern.compile("３"), "3");
        replaces.put(Pattern.compile("４"), "4");
        replaces.put(Pattern.compile("５"), "5");
        replaces.put(Pattern.compile("６"), "6");
        replaces.put(Pattern.compile("７"), "7");
        replaces.put(Pattern.compile("８"), "8");
        replaces.put(Pattern.compile("９"), "9");
    }

    public static void process(String folder) {
        Path currdir = Paths.get(folder);
        if (Files.isDirectory(currdir)) {
            try {
                boolean networkAvailable = true;
                InetAddress[] addresses = InetAddress.getAllByName("raw.githubusercontent.com");
                for (InetAddress address : addresses) {
                    if (!networkAvailable) {
                        networkAvailable = address.isReachable(3000);
                        if (networkAvailable) {
                            break;
                        }
                    }
                }
                Path sqlPath = currdir.resolve("zip33.sql");
                if (!networkAvailable) {
                    if (!Files.exists(sqlPath)) {
                        logger.fatal("無法連線:raw.githubusercontent.com且SQL檔不存在");
                    }
                } else {
                    if (!Files.exists(sqlPath)) {
                        Files.deleteIfExists(currdir.resolve("zip33.trace.db"));
                        Files.deleteIfExists(currdir.resolve("zip33.mv.db"));
                        downloadSql(sqlPath);
                    } else {
                        String ver = Files.lines(sqlPath).findFirst().get();
                        if (ver.startsWith("--")) {
                            ver = ver.substring(2);
                        }
                        if (ver.compareTo(readVersion()) < 0) {
                            Files.deleteIfExists(currdir.resolve("zip33.trace.db"));
                            Files.deleteIfExists(currdir.resolve("zip33.mv.db"));
                            downloadSql(sqlPath);
                        }
                    }
                }
            } catch (IOException ex) {
                logger.error("\"目錄:\"" + folder + "\"預處理發生問題:" + ex.getMessage(), ex);
            }
        } else {
            logger.error("{} 不是目錄", folder);
        }
    }

    private static void downloadSql(Path sqlPath) throws IOException {
        logger.info("下載SQL檔");
        String sqlloc = "https://raw.githubusercontent.com/kentyeh/zip33-data/master/zip33.sql";
        URL sqlurl = new URL(sqlloc);
        try (ReadableByteChannel rbcChanel = Channels.newChannel(sqlurl.openStream());
                FileOutputStream sqlOS = new FileOutputStream(sqlPath.toFile())) {
            sqlOS.getChannel().transferFrom(rbcChanel, 0, Long.MAX_VALUE);
        }
    }

    private static String readVersion() throws IOException {
        String verloc = "https://raw.githubusercontent.com/kentyeh/zip33-data/master/version.txt";
        URL verurl = new URL(verloc);
        URLConnection conn = verurl.openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().findFirst().get();
        }
    }

    public static List<String> mapSearch(String address) {
        try {
            String url = String.format("https://api.nlsc.gov.tw/MapSearch/ContentSearch?word=%s&mode=AutoComplete&count=10&feedback=XML&center=120.265470%%2C22.673172",
                    URLEncoder.encode(URLEncoder.encode(address, "UTF-8"), "UTF-8"));
            URL obj = new URL(url);
            System.setProperty("https.protocols", "TLSv1.2");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("GET");
            con.setRequestProperty("Host", "api.nlsc.gov.tw");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:70.0) Gecko/20100101 Firefox/70.0");
            con.setRequestProperty("Accept", "application/xml, text/xml, */*; q=0.01");
            con.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.8,en-US;q=0.5,en;q=0.3");
            con.setRequestProperty("Referer", "https://maps.nlsc.gov.tw/T09/mapshow.action?In_type=web");
            con.setRequestProperty("Origin", "https://maps.nlsc.gov.tw");
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                List<String> res = new ArrayList<>();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        Matcher m = maptn.matcher(line);
                        if (m.find()) {
                            for (Map.Entry<Pattern, String> e : replaces.entrySet()) {
                                line = e.getKey().matcher(line).replaceAll(e.getValue());
                            }
                            res.add(line);
                        }
                    }
                    return res;
                }
            } else {
                return Collections.EMPTY_LIST;
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return Collections.EMPTY_LIST;

        }
    }
}
