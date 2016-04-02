package twzip.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import twzip.model.Address;
import twzip.model.Dao;
import twzip.model.Post5;
import twzip.model.Village;

/**
 * 取得地址之郵遞區號
 *
 * @author Kent Yeh
 */
public class Zip implements InitializingBean {

    @Autowired
    @Qualifier("readOnlyDao")
    Dao dao;
    @Autowired
    SpelExpressionParser spel;
    Address rootAddr;
    EvaluationContext spelCtx;

    private static final Logger logger = LogManager.getLogger(Zip.class);
    Pattern pVillageEnd = Pattern.compile("[\\x{9109}\\x{93AE}\\x{6751}\\x{91CC}]$");
    Pattern pSpacies = Pattern.compile("\\s+");
    Pattern pSection = Pattern.compile("(\\d+)\\x{6BB5}");//xx段
    Pattern pLn = Pattern.compile("(\\d+)\\x{9130}");//xx鄰
    Pattern pLi = Pattern.compile("^(\\P{M}{2,}[\\x{91CC}\\x{6751}])[^\\x{8857}]\\p{InCJKUnifiedIdeographs}{2,}");//[里村][^街]

    @Override
    public void afterPropertiesSet() throws Exception {
        rootAddr = new Address();
        spelCtx = new StandardEvaluationContext(rootAddr);
    }

    private <T> void B2A(List<T>... lists) {
        if (lists.length > 1) {
            List<T> a = lists[0];
            List<T> b = lists[1];
            if (a.isEmpty() || (!b.isEmpty() && b.size() < a.size())) {
                a.clear();
                a.addAll(b);
            }
            for (int i = 1; i < lists.length; i++) {
                lists[i].clear();
            }
        }
    }

    /**
     * 取得地址的郵遞區號
     *
     * @param address 地址
     * @return
     */
    public List<Post5> getZip(String address) {
        address = Address.normailize(address);
        //先用頭3個字作測試，找出市屬郵遞區號
        String city = address.substring(0, 3), dist = "", vil = "";
        List<Post5> posts = dao.findByCity(city);
        if (posts.isEmpty()) {
            //找不到時再用頭2個字作測試，找出市屬郵遞區號
            city = address.substring(0, 2);
            posts = dao.findByCity(city);
        }
        for (Village v : dao.findVillages(city)) {
            if (dist.isEmpty() && address.contains(v.getDist())) {
                dist = v.getDist();
            }
            if (dist.equals(v.getDist()) && address.contains(v.getVil())) {
                vil = v.getVil();
                break;
            }
        }
        List<Post5> narrowPosts = new ArrayList<>();
        for (Post5 post : posts) {
            //再用鄉鎮市區過濾一次，以減少處理筆數
            if (address.contains(post.getArea())) {
                narrowPosts.add(post);
            }
        }
        if (!narrowPosts.isEmpty()) {
            B2A(posts, narrowPosts);
        }
        //若是都沒有就不用玩了
        if (posts.isEmpty()) {
            return posts;
        }
        if (address.charAt(2) == '縣' || address.charAt(2) == '市' || address.charAt(2) == '島' || address.charAt(2) == '台') {
            address = address.substring(3);
        }
        Matcher m = pLn.matcher(address);
        int ln = m.find() ? Integer.parseInt(m.group(1), 10) : 0;
        if (ln > 0) {
            //dd鄰從來不會出現在addinfo內，所以移除以干擾
            address = address.substring(0, m.start()) + address.substring(m.end());
        }
        m = pSection.matcher(address);
        int section = m.find() ? Integer.parseInt(m.group(1), 10) : 0;
        int pos = address.indexOf(vil);
        //addrInfo的片段，一定都要被地址內都找到才算數
        for (Post5 post : posts) {
            int idx = address.indexOf(post.getArea());
            String src = dist.isEmpty() ? address : idx == -1 ? address : address.substring(idx + post.getArea().length());
            //因為郵局的設定檔把有的"村"移除了
            String vil2 = vil.length()>2 && vil.charAt(vil.length() - 1) == '村' ? vil.substring(0, vil.length() - 1) : vil;
            if (!vil.isEmpty() && !post.getAddrinfo().contains(vil2) && pos > -1) {
                //移除村里干擾
                src = src.replaceFirst(vil, "");
            }
            String[] infos = post.getAddrinfo().split("\\s+");
            for (int i = 0; i < infos.length; i++) {
                idx = src.indexOf(infos[i]);
                if (i == 0) {
                    post.setFirstMatchesPos(idx);
                }
                if (idx == -1) {
                    if (pVillageEnd.matcher(infos[i]).find()) {
                        idx = infos[i].length();
                    } else {
                        break;
                    }
                } else {
                    src = src.substring(idx + infos[i].length());
                    if (i == infos.length - 1) {
                        post.setRemark(src);
                        narrowPosts.add(post);
                    }
                }
            }
        }
        //臺中市西屯區中清路四十三巷國泰一弄 vs 臺中市西屯區中清路　43巷全;
        if (narrowPosts.size() > 1) {
            Collections.sort(narrowPosts, new Comparator<Post5>() {

                @Override
                public int compare(Post5 o1, Post5 o2) {
                    return o1.getAddrinfo().length() == o2.getAddrinfo().length()
                            ? o1.getAddrinfo().compareTo(o2.getAddrinfo())
                            : o1.getAddrinfo().length() > o2.getAddrinfo().length() ? -1
                                    : 1;
                }
            });
            Post5 fp = narrowPosts.get(0);
            boolean maxmatched = true;
            posts.clear();
            posts.add(fp);
            for (int i = 1; i < narrowPosts.size(); i++) {
                if (fp.getAddrinfo().equals(narrowPosts.get(i).getAddrinfo())) {
                    posts.add(narrowPosts.get(i));
                } else if (fp.getAddrinfo().length() != narrowPosts.get(i).getAddrinfo().length()
                        || !fp.getAddrinfo().contains(narrowPosts.get(i).getAddrinfo())) {
                    maxmatched = false;
                    break;
                }
            }
            if (maxmatched) {
                if (posts.size() == 1) {
                    return posts;
                } else {
                    B2A(narrowPosts, posts);
                }
            }
        }
        int lane = 0;
        int alley = 0;
        float partialNumber = 0f;
        int floor = 0;
        if (narrowPosts.isEmpty()) {
            //沒有一個全數符合，不用再處理了
            return narrowPosts;
        } else if (narrowPosts.size() == 1) {
            return narrowPosts;
        } else {
            B2A(posts, narrowPosts);
            List<Post5> laneAlleyPosts = new ArrayList<>();
            List<Post5> laneOrAlleyPosts = new ArrayList<>();
            List<Post5> boundaryPosts = new ArrayList<>();
            List<Post5> dgBoundaryPosts = new ArrayList<>();
            for (Post5 post : posts) {
                Address addr = Address.parse(post.getRemark());
                if (lane == 0 && addr.getLane() > 0) {
                    lane = addr.getLane();
                }
                if (alley == 0 && addr.getAlley() > 0) {
                    alley = addr.getAlley();
                }
                if (partialNumber == 0 && addr.getAddnum() > 0) {
                    partialNumber = addr.getPartialNum();
                }
                if (floor == 0 && addr.getFloor() != 0) {
                    floor = addr.getFloor();
                }
                addr.setLn(ln);
//                if (post.getSec() == section) {
                try {
                    Expression exp = spel.parseExpression(post.getExpress());
                    rootAddr.assign(addr);
                    if (exp.getValue(spelCtx, Boolean.class)) {
                        //記著符合公式的設定
                        narrowPosts.add(post);
                        if (lane > 0 && post.getLane() != null && post.getLane().contains(String.format("^%d^", lane))
                                && alley > 0 && post.getAlley() != null && post.getAlley().contains(String.format("^%d^", alley))) {
                            laneAlleyPosts.add(post);
                        } else {
                            if (lane > 0 && post.getLane() != null && post.getLane().contains(String.format("^%d^", lane))) {
                                //額外有符合巷的設定
                                laneOrAlleyPosts.add(post);
                            }
                            if (alley > 0 && post.getAlley() != null && post.getAlley().contains(String.format("^%d^", alley))) {
                                //額外有符合弄的設定
                                laneOrAlleyPosts.add(post);
                            }
                        }
                    }
                    if (addr.canDowngrade()) {
                        rootAddr.assign(addr.downgrade());
                        if (exp.getValue(spelCtx, Boolean.class)) {
                            dgBoundaryPosts.add(post);
                        }
                    }
                    if (narrowPosts.isEmpty() && laneOrAlleyPosts.isEmpty()) {
                        exp = spel.parseExpression(post.getBoundary());
                        if (exp.getValue(spelCtx, Boolean.class)) {
                            boundaryPosts.add(post);
                        }
                    }
                } catch (SpelParseException | SpelEvaluationException | IllegalStateException ex) {
                    logger.error(ex.getMessage(), ex);
                    logger.fatal("can't parse [{}] with {}", addr, post);
                }
//                }
            }
            if (!laneAlleyPosts.isEmpty()) {
                if (laneAlleyPosts.size() == 1) {
                    return laneAlleyPosts;
                } else {
                    B2A(posts, laneAlleyPosts, laneOrAlleyPosts, boundaryPosts, dgBoundaryPosts);
                    narrowPosts.clear();
                }
            } else if (!laneOrAlleyPosts.isEmpty()) {
                if (laneOrAlleyPosts.size() == 1) {
                    return laneOrAlleyPosts;
                } else if (!laneAlleyPosts.isEmpty()) {
                    //若有符合巷弄的設定，以巷弄的設定優先
                    B2A(posts, laneOrAlleyPosts, laneAlleyPosts, boundaryPosts, dgBoundaryPosts);
                    narrowPosts.clear();
                }
            } else if (!narrowPosts.isEmpty()) {
                if (narrowPosts.size() == 1) {
                    return narrowPosts;
                } else {
                    B2A(posts, narrowPosts, laneAlleyPosts, laneOrAlleyPosts, boundaryPosts, dgBoundaryPosts);
                }
            } else if (!dgBoundaryPosts.isEmpty()) {
                if (boundaryPosts.size() == 1) {
                    return boundaryPosts;
                } else {
                    B2A(posts, dgBoundaryPosts, laneAlleyPosts, laneOrAlleyPosts, boundaryPosts);
                }
            } else if (!boundaryPosts.isEmpty()) {
                if (boundaryPosts.size() == 1) {
                    return boundaryPosts;
                } else {
                    B2A(posts, boundaryPosts, laneAlleyPosts, laneOrAlleyPosts, dgBoundaryPosts);
                }
            } else {
                //沒有一個公式符合，返回
                return narrowPosts;
            }
            //以附號進行測試
            if (partialNumber > 0) {
                for (Post5 post : posts) {
                    if (post.getParnums() != null && post.getParnume() != null
                            && partialNumber >= post.getParnums() && partialNumber <= post.getParnume()) {
                        narrowPosts.add(post);
                    }
                }
                if (narrowPosts.size() == 1) {
                    return narrowPosts;
                } else if (!narrowPosts.isEmpty()) {
                    B2A(posts, narrowPosts);
                }
            }
            //以樓層進行測試
            if (floor != 0) {
                for (Post5 post : posts) {
                    if (post.getFloors() != null && post.getFloore() != null
                            && floor >= post.getFloors() && floor <= post.getFloore()) {
                        narrowPosts.add(post);
                    }
                }
                if (narrowPosts.size() == 1) {
                    return narrowPosts;
                } else if (!narrowPosts.isEmpty()) {
                    B2A(posts, narrowPosts);
                }
            }
            narrowPosts.clear();
            //以符合長度最長者(剩餘最少者)為優先
            String src = posts.get(0).getRemark();
            int minlen = src.length();
            //找出剩餘最少者
            for (int i = 1; i < posts.size(); i++) {
                if (posts.get(i).getRemark().length() <= minlen) {
                    minlen = posts.get(i).getRemark().length();
                }
            }
            for (Post5 post : posts) {
                if (post.getRemark().length() == minlen) {
                    narrowPosts.add(post);
                }
            }
            if (narrowPosts.size() == 1) {
                return narrowPosts;
            } else if (!narrowPosts.isEmpty()) {
                B2A(posts, narrowPosts);
            }
            //以段進行測試
            if (section > 0) {
                for (Post5 post : posts) {
                    if (post.getSec() == section) {
                        narrowPosts.add(post);
                    }
                }
                if (narrowPosts.size() == 1) {
                    return narrowPosts;
                } else if (!narrowPosts.isEmpty()) {
                    B2A(posts, narrowPosts);
                }
            }
            int mc = 0;
            narrowPosts.clear();
            for (Post5 post : posts) {
                post.setRemark(pSpacies.matcher(post.getAddrinfo()).replaceAll(""));
                if (mc < post.getRemark().length()) {
                    mc = post.getRemark().length();
                }
            }
            for (Post5 post : posts) {
                if (post.getRemark().length() == mc) {
                    narrowPosts.add(post);
                }
            }
            if (narrowPosts.size() == 1) {
                return narrowPosts;
            } else {
                B2A(posts, narrowPosts);
            }
            //實在沒辦法，看看是否是同一個郵遞區號
            if (Post5.distinctZip(posts) != null) {
                Post5 post = posts.get(0);
                posts.clear();
                posts.add(post);
                return posts;
            } else {
                //90003屏東縣屏東市林森路1號 & 90047屏東縣屏東市林森路1巷全
                if (ln == 0) {
                    narrowPosts.clear();
                    for (Post5 post : posts) {
                        if (post.getLane() == null || post.getLane().isEmpty()) {
                            narrowPosts.add(post);
                        }
                    }
                }
                //沒輒，只好回傳
                return narrowPosts.isEmpty() ? posts : narrowPosts;
            }
        }
    }
}
