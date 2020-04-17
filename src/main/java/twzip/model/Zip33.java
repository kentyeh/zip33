package twzip.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 *
 * @author Kent Yeh
 */
public class Zip33 implements Serializable {

    private static final long serialVersionUID = -1522266669944375562L;

    private long zipid;
    private long casid;
    private String zipcode;
    private String recognition;
    private String scope;
    private Float lane;
    private Float alley;
    private Float parnums;
    private Float parnume;
    private Integer floors;
    private Integer floore;
    private String express;
    private String boundary;

    private Cas cas;

    public Zip33() {
    }

    public long getZipid() {
        return zipid;
    }

    public void setZipid(long zipid) {
        this.zipid = zipid;
    }

    public long getCasid() {
        return casid;
    }

    public void setCasid(long casid) {
        this.casid = casid;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getRecognition() {
        return recognition == null ? "" : recognition;
    }

    public void setRecognition(String recognition) {
        this.recognition = recognition;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String info) {
        this.scope = info;
    }

    public Float getLane() {
        return lane;
    }

    public void setLane(Float lane) {
        this.lane = lane;
    }

    public Float getAlley() {
        return alley;
    }

    public void setAlley(Float alley) {
        this.alley = alley;
    }

    public Float getParnums() {
        return parnums;
    }

    public void setParnums(Float parnums) {
        this.parnums = parnums;
    }

    public Float getParnume() {
        return parnume;
    }

    public void setParnume(Float parnume) {
        this.parnume = parnume;
    }

    public Float getParnumRng() {
        if ("單全".equals(scope) || "雙全".equals(scope) || "全".equals(scope)) {
            return Float.MAX_VALUE;
        } else if (this.parnums == null) {
            return null;
        } else {
            BigDecimal s = BigDecimal.valueOf(this.parnums).setScale(3, RoundingMode.HALF_UP);
            BigDecimal e = BigDecimal.valueOf(this.parnume).setScale(3, RoundingMode.HALF_UP);
            return e.subtract(s).floatValue();
        }
    }

    private int compareFloor(Zip33 other) {
        if (getFloorRng() == null && other.getFloorRng() == null) {
            return 0;
        } else if (other.getFloorRng() == null) {
            return Integer.MIN_VALUE;
        } else if (getFloorRng() == null) {
            return Integer.MAX_VALUE;
        } else {
            return getFloorRng() - other.getFloorRng();
        }
    }

    private int compareNumber(Zip33 other) {
        if (getParnumRng() == null && other.getParnumRng() == null) {
            return 0;
        } else if (other.getParnumRng() == null) {
            return Integer.MIN_VALUE;
        } else if (getParnumRng() == null) {
            return Integer.MAX_VALUE;
        } else {
            return getParnumRng().compareTo(other.getParnumRng());
        }
    }

    public int compareRng(Zip33 other) {
        int c = compareNumber(other);
        return c == 0 ? compareFloor(other) : c;
    }

    public Integer getFloors() {
        return floors;
    }

    public void setFloors(Integer floors) {
        this.floors = floors;
    }

    public Integer getFloore() {
        return floore;
    }

    public void setFloore(Integer floore) {
        this.floore = floore;
    }

    public Integer getFloorRng() {
        if (this.floors == null) {
            return null;
        } else {
            return this.floore - this.floors;
        }
    }

    public String getExpress() {
        return express;
    }

    public void setExpress(String express) {
        this.express = express;
    }

    public String getBoundary() {
        return boundary;
    }

    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    public boolean isSinkable() {
        return lane != null || alley != null || parnums != null || floors != null
                || "單全".equals(scope) || "雙全".equals(scope) || "全".equals(scope);
    }

    public Cas getCas() {
        return cas;
    }

    public void setCas(Cas cas) {
        this.cas = cas;
    }

    public static class Zip33Mapper implements RowMapper<Zip33> {

        @Override
        public Zip33 map(ResultSet rs, StatementContext ctx) throws SQLException {
            Zip33 res = new Zip33();
            res.setZipid(rs.getLong("zipid"));
            res.setCasid(rs.getLong("casid"));
            res.setZipcode(rs.getString("zipcode"));
            res.setRecognition(rs.getString("recognition"));
            res.setScope(rs.getString("scope"));
            BigDecimal bdv = rs.getBigDecimal("lane");
            res.setLane(bdv == null ? null : bdv.setScale(3, RoundingMode.HALF_UP).floatValue());
            bdv = rs.getBigDecimal("alley");
            res.setAlley(bdv == null ? null : bdv.setScale(3, RoundingMode.HALF_UP).floatValue());
            bdv = rs.getBigDecimal("parnums");
            res.setParnums(bdv == null ? null : bdv.setScale(3, RoundingMode.HALF_UP).floatValue());
            bdv = rs.getBigDecimal("parnume");
            res.setParnume(bdv == null ? null : bdv.setScale(3, RoundingMode.HALF_UP).floatValue());
            int iv = rs.getInt("floors");
            res.setFloors(rs.wasNull() ? null : iv);
            iv = rs.getInt("floore");
            res.setFloore(rs.wasNull() ? null : iv);
            res.setExpress(rs.getString("express"));
            res.setBoundary(rs.getString("boundary"));
            return res;
        }
    }

}
