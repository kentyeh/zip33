package twzip.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 儲存地址的數值資料
 *
 * @author Kent Yeh
 */
public class Address {

    private static final long serialVersionUID = 5452467653824560048L;
    private int ln = 0;
    private int lane = 0;
    private int addLane = 0;
    private int alley = 0;
    private int addAlley = 0;
    private int number = 0;
    private int addnum = 0;
    private String build = "";
    private int floor = 0;

    private static final Map<Pattern, String> replaces;

    private static final Pattern p71 = Pattern.compile("(\\d+)\\x{4E4B}(\\d+)\\x{4E4B}\\d+\\x{5DF7}");//xx之xx之xx巷
    private static final Pattern p72 = Pattern.compile("(\\d+)\\x{4E4B}(\\d+)\\x{5DF7}");//xx之xx巷
    private static final Pattern p73 = Pattern.compile("(\\d+)\\x{5DF7}");//巷
    private static final Pattern p81 = Pattern.compile("(\\d+)\\x{4E4B}(\\d+)\\x{5F04}");//xx之xx弄
    private static final Pattern p82 = Pattern.compile("(\\d+)\\x{5F04}");//弄
    private static final Pattern p91 = Pattern.compile("(\\d+)[\\x{4E4B}\\x{FF0D}-](\\d+)\\x{865F}[\\x{4E4B}\\x{FF0D}-]\\d+[^\\x{6A13}Ff]");//xx 之 xx 號 之 xx[^樓]
    private static final Pattern p92 = Pattern.compile("(\\d+)\\x{865F}[\\x{4E4B}\\x{FF0D}-](\\d+)[^\\x{6A13}Ff]");//xx 號之 xx[^樓]
    private static final Pattern p93 = Pattern.compile("(\\d+)[\\x{4E4B}\\x{FF0D}-](\\d+)[\\x{4E4B}\\x{FF0D}-]\\d+\\x{865F}");//xx 之 xx 之 xx號
    private static final Pattern p94 = Pattern.compile("(\\d+)[\\x{4E4B}\\x{FF0D}-](\\d+)\\x{865F}");//xx 之 xx號
    private static final Pattern p95 = Pattern.compile("(\\d+)\\x{865F}");//xx 號
    private static final Pattern pF1 = Pattern.compile("(\\p{InCJKUnifiedIdeographs}+)?\\x{5730}\\x{4E0B}(-?\\d+)\\x{6A13}");//樓
    private static final Pattern pF2 = Pattern.compile("(\\p{InCJKUnifiedIdeographs}+[^\\x{6A13}])?[bB](-?\\d+)[fF]?");
    private static final Pattern pF3 = Pattern.compile("(\\p{InCJKUnifiedIdeographs}+)?(-?\\d+)[\\x{6A13}fF]");
    private static final Pattern pNoNum = Pattern.compile("[^\\x{865F}].*\\d+$");//缺少號又以數字結尾

    static {
        replaces = new LinkedHashMap<>();
        //<editor-fold defaultstate="collapsed" desc="replace patterns">
        String one2nine = "[\\x{4E00}\\x{4E8C}\\x{4E09}\\x{56DB}\\x{4E94}\\x{516D}\\x{4E03}\\x{516B}\\x{4E5D}]";
        replaces.put(Pattern.compile("[\\x{3000}\\s]+"), "");
        replaces.put(Pattern.compile("(?<=" + one2nine + ")\\x{5341}(?=" + one2nine + ")"), "");
        replaces.put(Pattern.compile("(?<=" + one2nine + ")\\x{5341}"), "0");
        replaces.put(Pattern.compile("\\x{5341}(?=" + one2nine + ")"), "1");
        replaces.put(Pattern.compile("\\x{5341}"), "10");
        replaces.put(Pattern.compile("\\x{FF10}"), "0");
	replaces.put(Pattern.compile("[\\x{3127}\\x{4E00}\\x{FF11}]"), "1");
        replaces.put(Pattern.compile("[\\x{4E8C}\\x{FF12}]"), "2");
        replaces.put(Pattern.compile("[\\x{4E09}\\x{FF13}]"), "3");
        replaces.put(Pattern.compile("[\\x{56DB}\\x{FF14}]"), "4");
        replaces.put(Pattern.compile("[\\x{4E94}\\x{FF15}]"), "5");
        replaces.put(Pattern.compile("[\\x{516D}\\x{FF16}]"), "6");
        replaces.put(Pattern.compile("[\\x{4E03}\\x{FF17}]"), "7");
        replaces.put(Pattern.compile("[\\x{516B}\\x{FF18}]"), "8");
        replaces.put(Pattern.compile("[\\x{4E5D}\\x{FF19}]"), "9");
        replaces.put(Pattern.compile("\\x{FF08}"), "(");
        replaces.put(Pattern.compile("\\x{FF09}"), ")");
        replaces.put(Pattern.compile("\\x{81FA}"), "台");
        replaces.put(Pattern.compile("\\x{5553}"), "啟");
        replaces.put(Pattern.compile("\\x{5869}"), "鹽");
        replaces.put(Pattern.compile("\\x{62D5}"), "托");
        replaces.put(Pattern.compile("\\x{5ECD}"), "廓");
        replaces.put(Pattern.compile("\\x{78D8}"), "窯");
        replaces.put(Pattern.compile("\\x{7AB0}"), "窯");
        replaces.put(Pattern.compile("\\x{8218}"), "館");
        replaces.put(Pattern.compile("\\x{90A8}"), "村");
        replaces.put(Pattern.compile("\\x{5CEF}"), "峰");
        replaces.put(Pattern.compile("\\x{7AEA}"), "豎");
        replaces.put(Pattern.compile("\\x{7F97}"), "羌");
        replaces.put(Pattern.compile("\\x{7067}"), "灩");
        replaces.put(Pattern.compile("\\x{811A}"), "腳");
        replaces.put(Pattern.compile("\\x{920E}"), "鉤");
        replaces.put(Pattern.compile("\\x{7282}"), "犁");
        replaces.put(Pattern.compile("\\x{5DFF}"), "市");
        replaces.put(Pattern.compile("\\x{8914}"), "福");
        replaces.put(Pattern.compile("\\x{6E76}"), "泉");
        replaces.put(Pattern.compile("\\x{83D3}"), "果");
        replaces.put(Pattern.compile("\\x{5754}"), "湳");
        replaces.put(Pattern.compile("\\x{5742}"), "板");
        replaces.put(Pattern.compile("\\x{5E99}"), "廟");
        replaces.put(Pattern.compile("\\x{6B0D}"), "舊");
        replaces.put(Pattern.compile("\\x{78DC}"), "祭");
        replaces.put(Pattern.compile("\\x{7866}"), "弄");
        replaces.put(Pattern.compile("\\x{7B0B}"), "尹");
        replaces.put(Pattern.compile("\\x{713F}"), "庚");
        replaces.put(Pattern.compile("\\x{8EAD}"), "耽");
        replaces.put(Pattern.compile("\\x{9DC4}"), "雞");
        replaces.put(Pattern.compile("\\x{9B98}"), "代");
        replaces.put(Pattern.compile("\\x{5223}"), "台");
        replaces.put(Pattern.compile("\\x{53A6}"), "夏");
        replaces.put(Pattern.compile("\\x{53CC}"), "雙");
        replaces.put(Pattern.compile("\\x{5D75}"), "時");
        replaces.put(Pattern.compile("\\x{5E92}"), "庄");
        replaces.put(Pattern.compile("\\x{6898}"), "見");
        replaces.put(Pattern.compile("\\x{69FA}"), "康");
        replaces.put(Pattern.compile("\\x{7858}"), "回");
        replaces.put(Pattern.compile("\\x{8471}"), "蔥");
        replaces.put(Pattern.compile("\\x{5C2B}"), "尪");
        replaces.put(Pattern.compile("\\x{8289}"), "竿");
        replaces.put(Pattern.compile("\\x{53A8}"), "廚");
        replaces.put(Pattern.compile("\\x{53F7}"), "號");
        replaces.put(Pattern.compile("\\x{732A}"), "豬");
        replaces.put(Pattern.compile("\\x{58E0}"), "壟");
        replaces.put(Pattern.compile("\\x{7551}"), "煙");
        replaces.put(Pattern.compile("\\x{9424}"), "鼎");
        replaces.put(Pattern.compile("\\x{732B}"), "貓");
        replaces.put(Pattern.compile("\\x{6BBB}"), "殼");
        replaces.put(Pattern.compile("\\x{5CBA}"), "苓");
        replaces.put(Pattern.compile("\\x{8534}"), "麻");
        replaces.put(Pattern.compile("\\x{575F}"), "汶");
        replaces.put(Pattern.compile("\\x{270FD}"), "應");
        replaces.put(Pattern.compile("\\x{26C21}"), "那");
        replaces.put(Pattern.compile("\\x{21D9B}"), "卡");
        replaces.put(Pattern.compile("\\x{2555F}"), "漏");
        //以下為置換常常混淆的詞
        replaces.put(Pattern.compile("\\x{82B1}\\x{9023}"), "花蓮");
        replaces.put(Pattern.compile("\\d+(?=\\x{5C6F})"), "村");
        replaces.put(Pattern.compile("\\x{65B0}\\x{5C6F}"), "新村");
        replaces.put(Pattern.compile("\\x{79D1}\\x{5C6F}"), "科村");
        replaces.put(Pattern.compile("\\x{69B4}\\x{5C6F}"), "榴村");
        replaces.put(Pattern.compile("\\x{4EC1}\\x{5FB7}\\x{5C6F}"), "仁德村");
        replaces.put(Pattern.compile("\\x{5C45}\\x{5357}\\x{5C6F}"), "居南村");
        replaces.put(Pattern.compile("\\x{7D20}\\x{5FC3}\\x{5C6F}"), "素心村");
        replaces.put(Pattern.compile("\\x{5730}\\x{4E0B}\\x{6A13}"), "-1樓");
        replaces.put(Pattern.compile("\\x{5730}\\x{4E0B}(?=\\d+[\\x{6A13}Ff])"), "-");
        //</editor-fold>
    }

    /**
     * 將地址中的罕用字換成一般常用的字
     *
     * @param src
     * @return
     */
    public static String normailize(String src) {
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
            res.setAddLane(Integer.parseInt(m.group(2), 10));
            addr = addr.substring(m.end());
        }
        m = p72.matcher(addr);
        if (m.find()) {
            res.setLane(Integer.parseInt(m.group(1), 10));
            res.setAddLane(Integer.parseInt(m.group(2), 10));
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
            res.setAddAlley(Integer.parseInt(m.group(2), 10));
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
            res.setAddnum(Integer.parseInt(m.group(2), 10));
            addr = addr.substring(m.end());
        } else {
            m = p95.matcher(addr);
            if (m.find()) {
                res.setNumber(Integer.parseInt(m.group(1), 10));
                addr = addr.substring(m.end());
            }
        }
        m = pF1.matcher(addr);
        if (m.find()) {
            if (m.group(1) != null) {
                res.setBuild(m.group(1));
            }
            res.setFloor(-1 * Integer.parseInt(m.group(2), 10));
            addr = addr.substring(m.end());
            if (!addr.isEmpty()) {
                res.setBuild(addr.trim().replaceAll("\\(", "").replaceAll("\\)", ""));
            }
        } else {
            m = pF2.matcher(addr);
            if (m.find()) {
                if (m.group(1) != null) {
                    res.setBuild(m.group(1));
                }
                res.setFloor(-1 * Integer.parseInt(m.group(2), 10));
                addr = addr.substring(m.end());
                if (!addr.isEmpty()) {
                    res.setBuild(addr.trim().replaceAll("\\(", "").replaceAll("\\)", ""));
                }
            } else {
                m = pF3.matcher(addr);
                if (m.find()) {
                    if (m.group(1) != null) {
                        res.setBuild(m.group(1));
                    }
                    res.setFloor(Integer.parseInt(m.group(2), 10));
                    addr = addr.substring(m.end());
                    if (!addr.isEmpty()) {
                        res.setBuild(addr.trim().replaceAll("\\(", "").replaceAll("\\)", ""));
                    }
                } else if (addr.length() > 1) {
                    res.setBuild(addr.trim().replaceAll("\\(", "").replaceAll("\\)", ""));
                }

            }
        }
//        if (!addr.isEmpty() && (res.getBuild() == null || res.getBuild().isEmpty())) {
//        if (!addr.isEmpty()) {
//            res.pushBuild(addr.replaceAll("\\(", "").replaceAll("\\)", ""));
//        }
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
    public int getAddLane() {
        return addLane;
    }

    /**
     * 巷之附巷
     *
     * @param addLane 巷之附巷
     */
    public void setAddLane(int addLane) {
        this.addLane = addLane;
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
    public int getAddAlley() {
        return addAlley;
    }

    /**
     * 弄之附弄
     *
     * @param addAlley 弄之附弄
     */
    public void setAddAlley(int addAlley) {
        this.addAlley = addAlley;
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
     * 巷弄號，有巷則回傳巷，無則回傳{@link #getAlleyNum() 弄號}
     *
     * @return 巷弄號
     */
    public int getLaneNum() {
        return lane > 0 ? lane : alley > 0 ? alley : number;
    }

    /**
     * 門號與附號組成的浮點數表示值.<br/>
     * 例如 3之5號表示為 3.005
     *
     * @return
     */
    public float getPartialNum() {
        return new BigDecimal(String.format("%d.%03d", number, addnum))
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
     * 巷弄號，有巷則回傳巷，無則回傳弄
     *
     * @return
     */
    public int getLaneAlley() {
        return lane > 0 ? lane : alley;
    }

    /**
     * 附號
     *
     * @return 附號
     */
    public int getAddnum() {
        return addnum;
    }

    /**
     * 附號
     *
     * @param addnum 附號
     */
    public void setAddnum(int addnum) {
        this.addnum = addnum;
    }

    /**
     * 建物，如南棟、北棟
     *
     * @return 建物
     */
    public String getBuild() {
        return build;
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
        return addLane > 0 || addAlley > 0;
    }

    /**
     * 將巷或弄降階為門號以在無符合的情況下，試著比對
     *
     * @return
     */
    public Address downgrade() {
        if (addLane > 0) {
            Address res = new Address();
            res.assign(this);
            res.setNumber(lane);
            res.setAddnum(addLane);
            res.setLane(0);
            res.setAddLane(0);
            return res;
        } else if (addAlley > 0) {
            Address res = new Address();
            res.assign(this);
            res.setNumber(alley);
            res.setAddnum(addAlley);
            res.setAlley(0);
            res.setAddAlley(0);
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
        this.setAddnum(other.getAddnum());
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
            if (addLane > 0) {
                sb.append("之").append(addLane);
            }
            sb.append("巷");
        }
        if (alley > 0) {
            sb.append(",alley= ").append(alley);
            if (addAlley > 0) {
                sb.append("之").append(addAlley);
            }
            sb.append("弄");
        }
        if (addnum > 0) {
            sb.append(",no2= ").append(number).append("之").append(addnum).append("號");
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
