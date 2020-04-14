package twzip.context;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.jdbi.v3.core.ConnectionFactory;

/**
 *
 * @author Kent Yeh
 */
public class ROConnectionFactory implements ConnectionFactory {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ROConnectionFactory.class);

    private DataSource ds;

    public ROConnectionFactory(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Connection openConnection() throws SQLException {
        Connection conn = ds.getConnection();
        conn.setReadOnly(true);
        return conn;
    }

    @Override
    public void closeConnection(Connection toClose) {
        try {
            toClose.close();
        } catch (SQLException ex) {
            logger.error("關閉連線失敗：" + ex.getMessage(), ex);
        }
    }
}
