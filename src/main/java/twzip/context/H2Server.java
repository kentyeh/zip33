package twzip.context;

import java.sql.SQLException;
import org.h2.tools.Server;

/**
 * 以 mvn jetty:run 直接執行時，啟用H2 database.<br/>
 * 應注意，若是正式環境使用，應當移到其它正式資料庫，
 * 畢竟H2 database 用久了會越來越大.<br/>
 * Compact H2 database請參考<a href="http://www.h2database.com/html/features.html#compacting" target="_blank">官方文件<a/>
 * @author Kent Yeh
 */
public class H2Server {

    public static void start(String args) throws SQLException {
        final Server server = Server.createTcpServer(args.split(",")).start();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                server.stop();
            }

        });
    }
}
