package twzip.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * 村里行政區資料
 *
 * @author Kent Yeh
 */
public class Village {

    private final String city;
    private final String area;
    private final String vil;

    public Village(String city, String dist, String vil) {
        this.city = city;
        this.area = dist;
        this.vil = vil;
    }

    /**
     * 縣市
     *
     * @return 縣市
     */
    public String getCity() {
        return city;
    }

    /**
     * 縣市鄉鎮
     *
     * @return 縣市鄉鎮
     */
    public String getArea() {
        return area;
    }

    /**
     * 村里
     *
     * @return 村里
     */
    public String getVil() {
        return vil;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.city);
        hash = 11 * hash + Objects.hashCode(this.area);
        hash = 11 * hash + Objects.hashCode(this.vil);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Village other = (Village) obj;
        if (!Objects.equals(this.city, other.city)) {
            return false;
        }
        if (!Objects.equals(this.area, other.area)) {
            return false;
        }
        return Objects.equals(this.vil, other.vil);
    }

    @Override
    public String toString() {
        return city + area + vil;
    }

    /**
     * <a href="http://jdbi.org/" target="_blank">JDBI</a>用來對應資料表格village與物件的配接器
     */
    public static class VillageMapper implements RowMapper<Village> {

        @Override
        public Village map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Village(rs.getString("city"), rs.getString("area"), rs.getString("vil"));
        }

    }
}
