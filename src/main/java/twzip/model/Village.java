package twzip.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

/**
 * 村里行政區資料
 *
 * @author Kent Yeh
 */
public class Village {

    private String city;
    private String dist;
    private String vil;
    private static final Pattern pTai = Pattern.compile("\\x{81FA}");//臺

    public Village(String city, String dist, String vil) {
        this.city = pTai.matcher(city).replaceAll("台");
        this.dist = Address.normailize(dist);
        this.vil = Address.normailize(vil);
    }
    /**
     * 縣市
     * @return 縣市
     */
    public String getCity() {
        return city;
    }
    /**
     * 縣市
     * @param city 縣市
     */
    public void setCity(String city) {
        this.city = city == null ? null : pTai.matcher(city).replaceAll("台");
    }
    /**
     * 縣市鄉鎮
     * @return 縣市鄉鎮
     */
    public String getDist() {
        return dist;
    }
    /**
     * 縣市鄉鎮
     * @param dist 縣市鄉鎮
     */
    public void setDist(String dist) {
        this.dist = dist == null ? null : Address.normailize(dist);
    }
    /**
     * 村里
     * @return 村里
     */
    public String getVil() {
        return vil;
    }
    /**
     * 村里
     * @param vil 村里
     */
    public void setVil(String vil) {
        this.vil = vil == null ? null : Address.normailize(vil);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.city);
        hash = 11 * hash + Objects.hashCode(this.dist);
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
        if (!Objects.equals(this.dist, other.dist)) {
            return false;
        }
        if (!Objects.equals(this.vil, other.vil)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return city + dist + vil;
    }

    /**
     * <a href="http://jdbi.org/" target="_blank">JDBI</a>用來對應資料表格village與物件的配接器
     */
    public static class DistrictMapper implements ResultSetMapper<Village> {

        @Override
        public Village map(int i, ResultSet rs, StatementContext sc) throws SQLException {
            return new Village(rs.getString("city"), rs.getString("dist"), rs.getString("vil"));
        }

    }
}
