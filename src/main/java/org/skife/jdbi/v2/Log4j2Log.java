package org.skife.jdbi.v2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.skife.jdbi.v2.logging.FormattedLog;
import org.skife.jdbi.v2.logging.Log4JLog;

/**
 * log4j2 for <a href="http://jdbi.org/" target="_blank">JDBI</a>
 * 如果設定 Logger org.skife.jdbi.v2 的Level 為Debug，則會輸出執行Sql
 * @author Kent Yeh
 */
public class Log4j2Log extends FormattedLog {

    public final static Log4JLog DEFAULT = new Log4JLog();
    private final Logger logger;

    public Log4j2Log() {
        logger = LogManager.getLogger(DBI.class.getPackage().getName());
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected void log(String msg) {
        logger.debug(msg);
    }
}
