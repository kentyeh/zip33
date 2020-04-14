package twzip.model;

import java.io.BufferedReader;
import org.apache.logging.log4j.LogManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Kent Yeh
 */
public class PreDataUtil {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(PreDataUtil.class);

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
}
