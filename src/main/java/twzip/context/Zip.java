package twzip.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import twzip.model.Address;
import twzip.model.Cas;
import twzip.model.Dao;
import twzip.model.Village;
import twzip.model.Zip33;

/**
 * 取得地址之郵遞區號
 *
 * @author Kent Yeh
 */
public class Zip implements InitializingBean {

    private org.springframework.context.ApplicationContext ctx;
    private Dao dao;
    private SpelExpressionParser spel;
    Address rootAddr;
    EvaluationContext spelCtx;

    @Autowired
    public void setCtx(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Autowired
    public void setRootAddr(Address rootAddr) {
        this.rootAddr = rootAddr;
    }

    @Autowired
    public void setDao(Dao dao) {
        this.dao = dao;
    }

    @Autowired
    public void setSpel(SpelExpressionParser spel) {
        this.spel = spel;
    }

    private static final Logger logger = LogManager.getLogger(Zip.class);
    Pattern pVillageEnd = Pattern.compile("[\\x{9109}\\x{93AE}\\x{6751}\\x{91CC}]$");
    Pattern pSpacies = Pattern.compile("\\s+");
    Pattern pNum = Pattern.compile("\\d+");
    Pattern pEnd = Pattern.compile("^[\\x{865F}\\x{6A13}\\x{5C64}Ff]");//開頭為號或樓
    Pattern pSection = Pattern.compile("(\\d+)\\x{6BB5}");//xx段
    Pattern pLn = Pattern.compile("(\\d+)\\x{9130}");//xx鄰
    Pattern pLi = Pattern.compile("^(\\P{M}{2,}[\\x{91CC}\\x{6751}])[^\\x{8857}]\\p{InCJKUnifiedIdeographs}{2,}");//[里村][^街]

    private static final Map<String, String> abbrCity = new HashMap<>();

    static {
        abbrCity.put("南投市", "南投縣");
        abbrCity.put("員林市", "彰化縣");
        abbrCity.put("太保市", "嘉義縣");
        abbrCity.put("宜蘭市", "宜蘭縣");
        abbrCity.put("屏東市", "屏東縣");
        abbrCity.put("彰化市", "彰化縣");
        abbrCity.put("斗6市", "雲林縣");
        abbrCity.put("朴子市", "嘉義縣");
        abbrCity.put("竹北市", "新竹縣");
        abbrCity.put("台東市", "台東縣");
        abbrCity.put("花蓮市", "花蓮縣");
        abbrCity.put("苗栗市", "苗栗縣");
        abbrCity.put("頭份市", "苗栗縣");
        abbrCity.put("馬公市", "澎湖縣");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        spelCtx = new StandardEvaluationContext(rootAddr);
    }

    private <T> void B2A(List<T> a, List<T> b) {
        if (a.isEmpty() || (!b.isEmpty() && b.size() < a.size())) {
            a.clear();
            a.addAll(b);
        }
    }

    public List<Zip33> distinct(List<Zip33> zips) {
        Set<String> zipcodes = new HashSet<>();
        for (Iterator<Zip33> itor = zips.iterator(); itor.hasNext();) {
            Zip33 zip = itor.next();
            if (zipcodes.contains(zip.getZipcode())) {
                itor.remove();
            } else {
                zipcodes.add(zip.getZipcode());
            }
        }
        return zips;
    }

    public List<Zip33> deFull(List<Zip33> zips) {
        List<Zip33> others = new ArrayList<>(zips);
        for (Iterator<Zip33> itor = others.iterator(); itor.hasNext();) {
            Zip33 zip = itor.next();
            if (zip.getScope().contains("全") && !zip.getScope().endsWith("附號全")) {
                itor.remove();
            }
        }
        others = distinct(others);
        return others.size() == 1 ? others : zips;
    }

    public List<Cas> deLike(List<Cas> cases) {
        Cas cas = null;
        for (Iterator<Cas> itor = cases.iterator(); itor.hasNext();) {
            if (cas == null) {
                cas = itor.next();
            } else {
                Cas curr = itor.next();
                if (cas.getArea().equals(curr.getArea())
                        && cas.getStreet().contains(curr.getStreet())) {
                    itor.remove();
                }
            }
        }
        return cases;
    }

    /**
     * 取得地址的郵遞區號
     *
     * @param address 地址
     * @return
     */
    public List<Zip33> getZip33(String address) {
        if (address.length() < 9) {
            return Collections.EMPTY_LIST;
        }
        address = rootAddr.normailize(address);
        for (Map.Entry<String, String> abbr : abbrCity.entrySet()) {
            if (address.startsWith(abbr.getKey())) {
                address = abbr.getValue() + address;
                break;
            }
        }
        //先用頭3個字作測試，找出市屬郵遞區號
        String city = address.substring(0, 3), area = "", vil = "";
        List<Cas> cases = dao.findCasByCity(city);
        if (cases.isEmpty()) {
            //找不到時再用頭2個字作測試，找出市屬郵遞區號
            city = address.substring(0, 2);
            cases = dao.findCasByCity(city);
            //找不到再找區域
            if (cases.isEmpty()) {
                cases = dao.findCasByArea(city);
            }
        }

        for (Village v : dao.findVilByCity(city)) {
            if (area.isEmpty() && address.contains(v.getArea())) {
                area = v.getArea();
            }
            if (area.equals(v.getArea()) && address.contains(v.getVil())) {
                vil = v.getVil();
                break;
            }
        }
        List<Cas> narrowCases = new ArrayList<>();
        if (!vil.isEmpty()) {
            for (Cas cas : cases) {
                //用村里過濾一次，以減少處理筆數
                if (vil.equals(cas.getVillage()) && address.contains(cas.getStreet())) {
                    narrowCases.add(cas);
                }
            }
            //地址移除村里
            for (Cas cas : narrowCases) {
                int pos = address.indexOf(cas.getVillage());
                address = address.substring(pos + cas.getVillage().length());
                break;
            }
        }
        if (narrowCases.isEmpty()) {
            //用鄉鎮市區過濾一次，以減少處理筆數
            for (Cas cas : cases) {
                if (address.contains(cas.getArea())) {
                    narrowCases.add(cas);
                }
            }
            if (!narrowCases.isEmpty()) {
                //地址移除鄉鎮市區
                for (Cas cas : narrowCases) {
                    int pos = address.indexOf(cas.getArea());
                    address = address.substring(pos + cas.getArea().length());
                    break;
                }
                B2A(cases, narrowCases);
            }
        } else {
            B2A(cases, narrowCases);
        }
        //若是都沒有就不用玩了
        if (cases.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        //移除可能的村里
        if (!vil.isEmpty()) {
            int pos = address.indexOf(vil);
            if (pos > -1) {
                address = address.substring(pos + vil.length());
            }
        }
        Matcher m = pLn.matcher(address);
        int ln = m.find() ? Integer.parseInt(m.group(1), 10) : 0;
        if (ln > 0) {
            //dd鄰從來不會出現在addinfo內，所以移除以免干擾
            address = address.substring(0, m.start()) + address.substring(m.end());
        }
        //街道過濾
        if (cases.size() > 1) {
            for (Iterator<Cas> iter = cases.iterator(); iter.hasNext();) {
                Cas cas = iter.next();
                if (!cas.getStreet().isEmpty() && !address.contains(cas.getStreet())) {
                    iter.remove();
                }
            }
            //可能碰到Empty Street的尷尬
            if (cases.size() > 1) {
                narrowCases = new ArrayList<>();
                for (Cas cas : cases) {
                    if (!cas.getStreet().isEmpty()) {
                        narrowCases.add(cas);
                    }
                    if (narrowCases.size() > 1) {
                        break;
                    }
                }
                if (narrowCases.size() == 1) {
                    B2A(cases, narrowCases);
                }
            }
        }
        //dd段過濾
        m = pSection.matcher(address);
        if (m.find()) {
            int secno = Integer.parseInt(m.group(1), 10);
            narrowCases = new ArrayList<>();
            for (Iterator<Cas> iter = cases.iterator(); iter.hasNext();) {
                Cas cas = iter.next();
                if (cas.getSec() == secno) {
                    narrowCases.add(cas);
                }
            }
            B2A(cases, narrowCases);
            address = address.substring(m.end());
        }
        //若是都沒有就不用玩了
        if (cases.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        if (cases.size() > 1) {
            Collections.sort(cases, (o1, o2) -> {
                return o2.getStreet().length() - o1.getStreet().length();
            });
            deLike(cases);
        }
        List<Zip33> zips = new ArrayList<>();
        for (Cas cas : cases) {
            List<Zip33> qrys = findZip33(cas, address);
            for (Zip33 zip : qrys) {
                zip.setCas(cas);
                zips.add(zip);
            }
        }
        return distinct(zips);
    }

    public List<Zip33> findZip33(Cas cas, String address) {
        List<Zip33> zips = dao.findZipByCas(cas);
        if (zips.size() > 1) {
            //找大宗段,應避免像"松江路" "雙 190號至 192號" 的大宗段為"松江"
            String bigAddr = address;
            Matcher m = pNum.matcher(address);
            while (m.find()) {
                bigAddr = address.substring(m.end());
            }
            bigAddr = pEnd.matcher(bigAddr).find() ? bigAddr.substring(1) : bigAddr;//開頭為號或樓
            List<Zip33> sinkZips = sinkZips = new ArrayList<>();
            for (Zip33 zip : zips) {
                if (!zip.getRecognition().isEmpty() && bigAddr.contains(zip.getRecognition())) {
                    sinkZips.add(zip);
                }
            }
            if (!sinkZips.isEmpty()) {
                B2A(zips, sinkZips);
                sinkZips = new ArrayList<>();
            }
            Address paddr = ctx.getBean(Address.class, address);
            if (zips.size() > 1) {
                if (!paddr.getBuild().isEmpty()) {
                    for (Iterator<Zip33> itor = zips.iterator(); itor.hasNext();) {
                        if (!itor.next().getExpress().contains("build")) {
                            itor.remove();
                        }
                    }
                }
            }
            if (zips.size() > 1) {
                List<Zip33> narrowZips = new ArrayList<>(zips.size());
                List<Zip33> boundryZips = new ArrayList<>(zips.size());
                for (Zip33 zip : zips) {
                    Expression exp = null;
                    try {
                        exp = spel.parseExpression(zip.getExpress());
                        rootAddr.assign(paddr);
                        if (Boolean.TRUE.equals(exp.getValue(spelCtx, Boolean.class))) {
                            narrowZips.add(zip);
                            boolean sink = zip.isSinkable();
                            if (sink) {
                                sink = sink && (zip.getLane() == null || zip.getLane() == paddr.getLanef());
                                sink = sink && (zip.getAlley() == null || zip.getAlley() == paddr.getAlleyf());
                                sink = sink && (zip.getParnums() == null || (zip.getParnums() <= paddr.getNumberf() && paddr.getNumberf() <= zip.getParnume()));
                                sink = sink && (zip.getFloors() == null || (zip.getFloors() <= paddr.getFloor() && paddr.getFloor() <= zip.getFloore()));
                                if (sink) {
                                    sinkZips.add(zip);
                                }
                            }
                        }
                        if (paddr.canDowngrade()) {
                            rootAddr.assign(paddr.downgrade());
                            if (Boolean.TRUE.equals(exp.getValue(spelCtx, Boolean.class))) {
                                boundryZips.add(zip);
                            }
                        }
                    } catch (SpelParseException ex) {
                        logger.error(ex.getMessage(), ex);
                        break;
                    }
                }
                if (!sinkZips.isEmpty()) {
                    zips = distinct(sinkZips);
                    sinkZips = new ArrayList<>();
                    for (Zip33 zip : zips) {
                        if (zip.getParnumRng() != null || zip.getFloorRng() != null) {
                            sinkZips.add(zip);
                        }
                    }
                    if (sinkZips.isEmpty()) {
                        return zips;
                    } else {
                        if (sinkZips.size() == 1) {
                            return sinkZips;
                        } else {
                            zips = new ArrayList<>(sinkZips);
                            Collections.sort(zips, (z1, z2) -> {
                                return z1.compareRng(z2);
                            });
                            sinkZips = new ArrayList<>();
                            Zip33 target = null;
                            for (Zip33 zip : zips) {
                                if (sinkZips.isEmpty()) {
                                    sinkZips.add(zip);
                                    target = zip;
                                } else {
                                    int c = target.compareRng(zip);
                                    if (c == 0) {
                                        sinkZips.add(zip);
                                    } else if (c > 0) {
                                        sinkZips.clear();
                                        sinkZips.add(zip);
                                        target = zip;
                                    }
                                }
                            }
                            return sinkZips.isEmpty() ? zips : sinkZips;
                        }
                    }
                } else if (!narrowZips.isEmpty()) {
                    return narrowZips;
                } else {
                    return boundryZips;
                }
            }
        }
        return zips;
    }
}
