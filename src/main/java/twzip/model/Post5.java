package twzip.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

/**
 * 郵遞區號設定資料
 *
 * @author Kent Yeh
 */
public class Post5 {

    private long pid;
    private String zipcode;
    private String city;
    private String area;
    private String village;
    private String oriInfo;
    private String addrinfo;
    private Integer sec;
    private String tailInfo;
    private String express;
    private String boundary;
    private String lane;
    private String alley;
    private Float parnums;
    private Float parnume;
    private Integer floors;
    private Integer floore;
    private int firstMatchesPos;
    private String redundant;
    private String remark;

    /**
     * 取出郵遞區號設定資料中所有的郵遞區號
     *
     * @param posts 郵遞區號設定資料
     * @return
     */
    public static Collection<Long> ids(Collection<Post5> posts) {
        List<Long> list = new ArrayList<>();
        if (posts == null || posts.isEmpty()) {
            list.add(Long.MIN_VALUE);
            return list;
        }
        for (Post5 post : posts) {
            list.add(post.getPid());
        }
        return list;
    }

    /**
     * 查看郵遞區號設定資料是否全部為相同之郵遞區號
     *
     * @param posts
     * @return
     */
    public static String distinctZip(Collection<Post5> posts) {
        String zip = null;
        for (Post5 post : posts) {
            if (zip == null) {
                zip = post.getZipcode();
            } else if (!zip.equals(post.getZipcode())) {
                return null;
            }
        }
        return zip;
    }

    public Post5() {
    }

    public Post5(long pid) {
        this.pid = pid;
    }

    /**
     * 郵遞區號設定資料表格主鍵
     *
     * @return
     */
    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    /**
     * 取得郵遞區號
     *
     * @return 郵遞區號
     */
    public String getZipcode() {
        return zipcode;
    }

    /**
     * 設定郵遞區號
     *
     * @param zipcode
     */
    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    /**
     * 取得縣市
     *
     * @return 縣市
     */
    public String getCity() {
        return city;
    }

    /**
     * 設定縣市
     *
     * @param city 縣市
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * 取得鄉市縣鎮
     *
     * @return 鄉市縣鎮
     */
    public String getArea() {
        return area;
    }

    /**
     * 設定鄉市縣鎮
     *
     * @param area 鄉市縣鎮
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * 取得村里(Optional,因應有的Road必須以村里區分)
     *
     * @return 村里
     */
    public String getVillage() {
        return village == null ? "" : village;
    }

    /**
     * 設定村里(因應有的Road必須以村里區分)
     *
     * @param village 村里
     */
    public void setVillage(String village) {
        this.village = village;
    }

    /**
     * 郵局的原始設定資料
     *
     * @return 郵局的原始設定資料
     */
    public String getOriInfo() {
        return oriInfo;
    }

    /**
     * 郵局的原始設定資料
     *
     * @param oriInfo 郵局的原始設定資料
     */
    public void setOriInfo(String oriInfo) {
        this.oriInfo = oriInfo;
    }

    /**
     * 郵遞區號設定資料的街道資料
     *
     * @return 街道資料
     */
    public String getAddrinfo() {
        return addrinfo;
    }

    /**
     * 郵遞區號設定資料的街道資料
     *
     * @param addrinfo 街道資料
     */
    public void setAddrinfo(String addrinfo) {
        this.addrinfo = addrinfo;
    }

    /**
     * 段
     *
     * @return 段
     */
    public Integer getSec() {
        return sec;
    }

    /**
     * 段
     *
     * @param sec 段
     */
    public void setSec(Integer sec) {
        this.sec = sec;
    }

    /**
     * 郵遞區號設定資料的門牌設定
     *
     * @return 門牌設定
     */
    public String getTailInfo() {
        return tailInfo;
    }

    /**
     * 郵遞區號設定資料的門牌設定
     *
     * @param tailInfo 門牌設定
     */
    public void setTailInfo(String tailInfo) {
        this.tailInfo = tailInfo;
    }

    /**
     * 門牌設定資料的表達示
     *
     * @return 表達示
     */
    public String getExpress() {
        return express;
    }

    /**
     * 門牌設定資料的表達示
     *
     * @param express 表達示
     */
    public void setExpress(String express) {
        this.express = express;
    }

    /**
     * 門牌設定資料邊界的 號/巷 相容表達示
     *
     * @return 號/巷 相容表達示
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * 門牌設定資料邊界的 號/巷 相容表達示
     *
     * @param boundary 號/巷 相容表達示
     */
    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    /**
     * 鄰
     *
     * @return 鄰
     */
    public String getLane() {
        return lane;
    }

    /**
     * 鄰
     *
     * @param lane 鄰
     */
    public void setLane(String lane) {
        this.lane = lane;
    }

    /**
     * 弄
     *
     * @return 弄
     */
    public String getAlley() {
        return alley;
    }

    /**
     * 弄
     *
     * @param alley 弄
     */
    public void setAlley(String alley) {
        this.alley = alley;
    }

    /**
     * 起始門牌設定資料的號與附號組成的浮點數表示值.<br/>
     * 例如 3之5號表示為 3.005
     *
     * @return 號與附號組成的浮點數表示值
     */
    public Float getParnums() {
        return parnums;
    }

    /**
     * 起始門牌設定資料的號與附號組成的浮點數表示值
     *
     * @param parnums 號與附號組成的浮點數表示值
     */
    public void setParnums(Float parnums) {
        this.parnums = parnums;
    }

    /**
     * 門牌設定資料未尾的號與附號組成的浮點數表示值.<br/>
     * 例如 3之5號表示為 3.005
     *
     * @return 號與附號組成的浮點數表示值
     */
    public Float getParnume() {
        return parnume;
    }

    /**
     * 門牌設定資料未尾的號與附號組成的浮點數表示值
     *
     * @param parnume 號與附號組成的浮點數表示值
     */
    public void setParnume(Float parnume) {
        this.parnume = parnume;
    }

    /**
     * 門牌設定資料的起始樓層，地下層為負值.
     *
     * @return 樓層
     */
    public Integer getFloors() {
        return floors;
    }

    /**
     * 門牌設定資料的起始樓層，地下層為負值.
     *
     * @param floors 樓層
     */
    public void setFloors(Integer floors) {
        this.floors = floors;
    }

    /**
     * 門牌設定資料的未尾樓層，地下層為負值.
     *
     * @return 樓層
     */
    public Integer getFloore() {
        return floore;
    }

    /**
     * 門牌設定資料的未尾樓層，地下層為負值.
     *
     * @param floore 樓層
     */
    public void setFloore(Integer floore) {
        this.floore = floore;
    }

    /**
     * 解析地址時暫存用
     *
     * @return
     */
    public int getFirstMatchesPos() {
        return firstMatchesPos;
    }

    /**
     * 解析地址時暫存用
     *
     * @param firstMatchesPos
     */
    public void setFirstMatchesPos(int firstMatchesPos) {
        this.firstMatchesPos = firstMatchesPos;
    }

    /**
     * 解析地址時暫存用
     *
     * @return
     */
    public String getRedundant() {
        return redundant;
    }

    /**
     * 解析地址時暫存用
     *
     * @param redundant
     */
    public void setRedundant(String redundant) {
        this.redundant = redundant;
    }

    /**
     * 解析地址時暫存用
     *
     * @return
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 解析地址時暫存用
     *
     * @param remark
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (int) (this.pid ^ (this.pid >>> 32));
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
        final Post5 other = (Post5) obj;
        return this.pid == other.pid;
    }

    @Override
    public String toString() {
        return "[" + zipcode + "]" + city + " " + area + " - " + addrinfo
                + " - " + oriInfo + tailInfo + " \"" + express + "\" boundary near to \"" + boundary + "\"";
    }

    /**
     * <a href="http://jdbi.org/" target="_blank">JDBI</a>用來對應資料表格post5與物件的配接器
     */
    public static class Post5Mapper implements ResultSetMapper<Post5> {

        @Override
        public Post5 map(int i, ResultSet rs, StatementContext sc) throws SQLException {
            Post5 post5 = new Post5(rs.getLong("pid"));
            post5.setZipcode(rs.getString("zipcode"));
            post5.setCity(rs.getString("city"));
            post5.setArea(rs.getString("area"));
            post5.setVillage(rs.getString("village"));
            post5.setOriInfo(rs.getString("oriInfo"));
            post5.setAddrinfo(rs.getString("addrinfo"));
            int iv = rs.getInt("sec");
            post5.setSec(rs.wasNull() ? null : iv);
            post5.setTailInfo(rs.getString("tailInfo"));
            post5.setExpress(rs.getString("express"));
            post5.setBoundary(rs.getString("boundary"));
            post5.setLane(rs.getString("lane"));
            post5.setAlley(rs.getString("alley"));
            float fv = rs.getFloat("parnums");
            post5.setParnums(rs.wasNull() ? null : fv);
            fv = rs.getFloat("parnume");
            post5.setParnume(rs.wasNull() ? null : fv);
            iv = rs.getInt("floors");
            post5.setFloors(rs.wasNull() ? null : iv);
            iv = rs.getInt("floore");
            post5.setFloore(rs.wasNull() ? null : iv);
            return post5;
        }

    }
}
