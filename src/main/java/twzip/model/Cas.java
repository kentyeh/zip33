package twzip.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 *
 * @author Kent Yeh
 */
public class Cas implements Serializable {

    private static final long serialVersionUID = -2704656862876892818L;
    private final long casid;
    private final String city;
    private final String area;
    private final String road;
    private final String village;
    private final String street;
    private final long sec;

    public Cas(long casid, String city, String area, String road, String village, String street, long sec) {
        this.casid = casid;
        this.city = city;
        this.area = area;
        this.road = road;
        this.village = village;
        this.street = street;
        this.sec = sec;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public long getCasid() {
        return casid;
    }

    public String getCity() {
        return city;
    }

    public String getArea() {
        return area;
    }

    public String getRoad() {
        return road;
    }

    public String getVillage() {
        return village;
    }

    public String getStreet() {
        return street;
    }

    public long getSec() {
        return sec;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (int) (this.casid ^ (this.casid >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return String.format("%d.%s:%s:%s:%s", casid, city, area, village, road);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cas other = (Cas) obj;
        return this.casid == other.casid;
    }

    public static class CasMapper implements RowMapper<Cas> {

        @Override
        public Cas map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Cas(rs.getLong("casid"), rs.getString("city"), rs.getString("area"),
                    rs.getString("road"), rs.getString("village"), rs.getString("street"), rs.getLong("sec"));
        }
    }
}
