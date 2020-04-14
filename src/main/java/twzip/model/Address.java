package twzip.model;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 儲存地址的數值資料
 *
 * @author Kent Yeh
 */
public class Address implements Serializable {

    private static final long serialVersionUID = 5452467653824560048L;
    private int ln = 0;
    private int lane = 0;
    private int extLane = 0;
    private int alley = 0;
    private int extAlley = 0;
    private int number = 0;
    private int extnum = 0;
    private String build = "";
    private int floor = 0;

    private Map<Pattern, String> replaces;

    private static final Pattern p71 = Pattern.compile("(\\d+)\\x{4E4B}(\\d+)\\x{4E4B}\\d+\\x{5DF7}");//xx之xx之xx巷
    private static final Pattern p72 = Pattern.compile("(\\d+)\\x{4E4B}(\\d+)\\x{5DF7}");//xx之xx巷
    private static final Pattern p73 = Pattern.compile("(\\d+)\\x{5DF7}");//巷
    private static final Pattern p81 = Pattern.compile("(\\d+)\\x{4E4B}(\\d+)\\x{5F04}");//xx之xx弄
    private static final Pattern p82 = Pattern.compile("(\\d+)\\x{5F04}");//弄
    private static final Pattern p91 = Pattern.compile("(\\d+)[\\x{4E4B}\\x{FF0D}-](\\d+)\\x{865F}[\\x{4E4B}\\x{FF0D}-]\\d+[^樓層Ff]");//xx 之 xx 號 之 xx[^樓]
    private static final Pattern p92 = Pattern.compile("(\\d+)\\x{865F}[\\x{4E4B}\\x{FF0D}-](\\d+)[^\\x{6A13}\\x{5C64}Ff]");//xx 號之 xx[^樓層Ff]
    private static final Pattern p93 = Pattern.compile("(\\d+)[\\x{4E4B}\\x{FF0D}-](\\d+)[\\x{4E4B}\\x{FF0D}-]\\d+\\x{865F}");//xx 之 xx 之 xx號
    private static final Pattern p94 = Pattern.compile("(\\d+)[\\x{4E4B}\\x{FF0D}-](\\d+)\\x{865F}");//xx 之 xx號
    private static final Pattern p95 = Pattern.compile("(\\d+)\\x{865F}");//xx 號
    private static final Pattern pF1 = Pattern.compile("(\\p{InCJKUnifiedIdeographs}+)?地下(-?\\d+)[\\x{6A13}\\x{5C64}Ff]");//樓層Ff
    private static final Pattern pF2 = Pattern.compile("(\\p{InCJKUnifiedIdeographs}+[^\\x{6A13}])?[bB](-?\\d+)[fF]?");
    private static final Pattern pF3 = Pattern.compile("(\\p{InCJKUnifiedIdeographs}+)?(-?\\d+)[\\x{6A13}\\x{5C64}Ff]");
    private static final Pattern pNoNum = Pattern.compile("[^\\x{865F}].*\\d+$");//缺少號又以數字結尾
    private static final Pattern pNSB = Pattern.compile("([\\x{5357}\\x{5317}])\\x{68DF}");//南北棟

    public void setReplaces(Map<Pattern, String> replaces) {
        this.replaces = replaces;
    }

    /**
     * 將地址中的罕用字換成一般常用的字
     *
     * @param src
     * @return
     */
    public String normailize(String src) {
        for (Map.Entry<Pattern, String> e : replaces.entrySet()) {
            src = e.getKey().matcher(src).replaceAll(e.getValue());
        }
        return pNoNum.matcher(src).matches() ? src + "號" : src;
    }

    /**
     * 拆解地址
     *
     * @param address 已正規化的地址
     * @return
     */
    public static Address parse(String address) {
        String addr = address;
        Address res = new Address();
        Matcher m = p71.matcher(addr);
        if (m.find()) {
            res.setLane(Integer.parseInt(m.group(1), 10));
            res.setExtLane(Integer.parseInt(m.group(2), 10));
            addr = addr.substring(m.end());
        }
        m = p72.matcher(addr);
        if (m.find()) {
            res.setLane(Integer.parseInt(m.group(1), 10));
            res.setExtLane(Integer.parseInt(m.group(2), 10));
            addr = addr.substring(m.end());
        }
        m = p73.matcher(addr);
        if (m.find()) {
            res.setLane(Integer.parseInt(m.group(1), 10));
            addr = addr.substring(m.end());
        }
        m = p81.matcher(addr);
        if (m.find()) {
            res.setAlley(Integer.parseInt(m.group(1), 10));
            res.setExtAlley(Integer.parseInt(m.group(2), 10));
            addr = addr.substring(m.end());
        } else {
            m = p82.matcher(addr);
            if (m.find()) {
                res.setAlley(Integer.parseInt(m.group(1), 10));
                addr = addr.substring(m.end());
            }
        }
        m = p91.matcher(addr);
        if (!m.find()) {
            m = p92.matcher(addr);
            if (!m.find()) {
                m = p93.matcher(addr);
                if (!m.find()) {
                    m = p94.matcher(addr);
                } else {
                    m.reset();
                }
            } else {
                m.reset();
            }
        } else {
            m.reset();
        }
        if (m.find()) {
            res.setNumber(Integer.parseInt(m.group(1), 10));
            res.setExtnum(Integer.parseInt(m.group(2), 10));
            addr = addr.substring(m.end());
        } else {
            m = p95.matcher(addr);
            if (m.find()) {
                res.setNumber(Integer.parseInt(m.group(1), 10));
                addr = addr.substring(m.end());
            }
        }
        m = pNSB.matcher(addr);
        if (m.find()) {
            res.setBuild(m.group(1) + "棟");
        }
        m = pF1.matcher(addr);
        if (m.find()) {
            res.setFloor(-1 * Integer.parseInt(m.group(2), 10));
        } else {
            m = pF2.matcher(addr);
            if (m.find()) {
                res.setFloor(-1 * Integer.parseInt(m.group(2), 10));
            } else {
                m = pF3.matcher(addr);
                if (m.find()) {
                    res.setFloor(Integer.parseInt(m.group(2), 10));
                }
            }
        }
        return res;

    }

    public Address() {
    }

    /**
     * 鄰
     *
     * @return 鄰
     */
    public int getLn() {
        return ln;
    }

    /**
     * 鄰
     *
     * @param ln 鄰
     */
    public void setLn(int ln) {
        this.ln = ln;
    }

    /**
     * 巷
     *
     * @return 巷
     */
    public int getLane() {
        return lane;
    }

    /**
     * 巷
     *
     * @param lane 巷
     */
    public void setLane(int lane) {
        this.lane = lane;
    }

    /**
     * 巷之附巷
     *
     * @return 巷之附巷
     */
    public int getExtLane() {
        return extLane;
    }

    /**
     * 巷之附巷
     *
     * @param extLane 巷之附巷
     */
    public void setExtLane(int extLane) {
        this.extLane = extLane;
    }

    public float getLanef() {
        return new BigDecimal(String.format("%d.%03d", lane, extLane))
                .setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    /**
     * 弄
     *
     * @return 弄
     */
    public int getAlley() {
        return alley;
    }

    /**
     * 弄
     *
     * @param alley 弄
     */
    public void setAlley(int alley) {
        this.alley = alley;
    }

    /**
     * 弄之附弄
     *
     * @return 弄之附弄
     */
    public int getExtAlley() {
        return extAlley;
    }

    /**
     * 弄之附弄
     *
     * @param extAlley 弄之附弄
     */
    public void setExtAlley(int extAlley) {
        this.extAlley = extAlley;
    }

    public float getAlleyf() {
        return new BigDecimal(String.format("%d.%03d", alley, extAlley))
                .setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    /**
     * 門號
     *
     * @return 門號
     */
    public int getNumber() {
        return number;
    }

    /**
     * 門號
     *
     * @param number 門號
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * 門號與附號組成的浮點數表示值.<br/>
     * 例如 3之5號表示為 3.005
     *
     * @return
     */
    public float getNumberf() {
        return new BigDecimal(String.format("%d.%03d", number, extnum))
                .setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    /**
     * 弄號，有弄則回傳弄，無則回傳門號
     *
     * @return 弄號
     */
    public int getAlleyNum() {
        return alley > 0 ? alley : number;
    }

    /**
     * 附號
     *
     * @return 附號
     */
    public int getExtnum() {
        return extnum;
    }

    /**
     * 附號
     *
     * @param extnum 附號
     */
    public void setExtnum(int extnum) {
        this.extnum = extnum;
    }

    /**
     * 建物，如南棟、北棟
     *
     * @return 建物
     */
    public String getBuild() {
        return build == null ? "" : build;
    }

    /**
     * 建物，如南棟、北棟
     *
     * @param build 建物
     */
    public void setBuild(String build) {
        this.build = build;
    }

    /**
     * 樓層
     *
     * @return 樓層
     */
    public int getFloor() {
        return floor;
    }

    /**
     * 樓層
     *
     * @param floor 樓層
     */
    public void setFloor(int floor) {
        this.floor = floor;
    }

    /**
     * 是否有巷或弄
     *
     * @return 是否有巷或弄
     */
    public boolean canDowngrade() {
        return lane > 0 || alley > 0;
    }

    /**
     * 將巷或弄降階為門號以在無符合的情況下，試著比對
     *
     * @return
     */
    public Address downgrade() {
        if (lane > 0) {
            Address res = new Address();
            res.assign(this);
            res.setNumber(lane);
            res.setExtnum(extLane);
            res.setLane(0);
            res.setExtLane(0);
            return res;
        } else if (alley > 0) {
            Address res = new Address();
            res.assign(this);
            res.setNumber(alley);
            res.setExtnum(extAlley);
            res.setAlley(0);
            res.setExtAlley(0);
            return res;
        } else {
            return this;
        }
    }

    /**
     * 將其它addr物件值指定給自已
     *
     * @param other 其它addr物件
     * @return 自已addr物件
     */
    public Address assign(Address other) {
        this.setLn(other.getLn());
        this.setLane(other.getLane());
        this.setAlley(other.getAlley());
        this.setNumber(other.getNumber());
        this.setExtnum(other.getExtnum());
        this.setBuild(other.getBuild());
        this.setFloor(other.getFloor());
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (ln > 0) {
            sb.append(ln).append("鄰");
        }
        if (lane > 0) {
            sb.append(",lane= ").append(lane);
            if (extLane > 0) {
                sb.append("之").append(extLane);
            }
            sb.append("巷");
        }
        if (alley > 0) {
            sb.append(",alley= ").append(alley);
            if (extAlley > 0) {
                sb.append("之").append(extAlley);
            }
            sb.append("弄");
        }
        if (extnum > 0) {
            sb.append(",no2= ").append(number).append("之").append(extnum).append("號");
        } else {
            sb.append(",no= ").append(number).append("號");
        }
        if (build != null && !build.isEmpty()) {
            sb.append(",b= ").append(build);

        }
        if (floor != 0) {
            sb.append(",f= ").append(floor).append("樓");
        }
        return sb.toString();
    }

}
