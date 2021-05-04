package twzip.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import twzip.model.Address;
import twzip.model.Dao;
import twzip.model.JdbiLog;

/**
 *
 * @author Kent Yeh
 */
public class Context {

    Jdbi jdbi;

    @Autowired
    public void setJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
        this.jdbi.installPlugin(new org.jdbi.v3.sqlobject.SqlObjectPlugin());
        this.jdbi.setSqlLogger(jdbiLog());
    }

    @Bean
    public JdbiLog jdbiLog() {
        return new JdbiLog();
    }
    @Bean(destroyMethod = "close")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Dao dao() {
        return jdbi.open().attach(Dao.class);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Address address() {
        Address res = new Address();
        res.setReplaces(replaceMap());
        return res;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Address address(String address) {
        Address res = Address.parse(address);
        res.setReplaces(replaceMap());
        return res;
    }

    @Bean("replaceMap")
    public Map<Pattern, String> replaceMap() {
        //<editor-fold defaultstate="collapsed" desc="地址替代">
        String one2nine = "[一二三四五六七八九]";
        Map<Pattern, String> replaces = new LinkedHashMap<>();
        replaces.put(Pattern.compile("[　\\s]+"), "");
        replaces.put(Pattern.compile("(?<=" + one2nine + ")十(?=" + one2nine + ")"), "");
        replaces.put(Pattern.compile("(?<=" + one2nine + ")十"), "0");
        replaces.put(Pattern.compile("十(?=" + one2nine + ")"), "1");
        replaces.put(Pattern.compile("十"), "10");
        replaces.put(Pattern.compile("０"), "0");
        replaces.put(Pattern.compile("[ㄧ一１]"), "1");
        replaces.put(Pattern.compile("[二２]"), "2");
        replaces.put(Pattern.compile("[三３]"), "3");
        replaces.put(Pattern.compile("[四４]"), "4");
        replaces.put(Pattern.compile("[五５]"), "5");
        replaces.put(Pattern.compile("[六６]"), "6");
        replaces.put(Pattern.compile("[七７]"), "7");
        replaces.put(Pattern.compile("[八８]"), "8");
        replaces.put(Pattern.compile("[九９]"), "9");
        replaces.put(Pattern.compile("[（]"), "(");
        replaces.put(Pattern.compile("[）]"), ")");
        replaces.put(Pattern.compile("[巿]"), "市");
        replaces.put(Pattern.compile("[褔]"), "福");
        replaces.put(Pattern.compile("[庒]"), "庄");
        replaces.put(Pattern.compile("[号]"), "號");
        replaces.put(Pattern.compile("[殻]"), "殼");
        replaces.put(Pattern.compile("[鳯]"), "鳳");
        replaces.put(Pattern.compile("[衆]"), "眾");
        replaces.put(Pattern.compile("[屛]"), "屏");
        replaces.put(Pattern.compile("[徳]"), "德");
        replaces.put(Pattern.compile("[陜]"), "陝");
        //<editor-fold defaultstate="collapsed" desc="郵局標準替換(僅第一個字)">
        replaces.put(Pattern.compile("[双]"), "雙");
        replaces.put(Pattern.compile("[邨]"), "屯");
        replaces.put(Pattern.compile("[刣臺]"), "台");
        replaces.put(Pattern.compile("[坟]"), "汶");
        replaces.put(Pattern.compile("[坂]"), "板");
        replaces.put(Pattern.compile("[芉]"), "竿");
        replaces.put(Pattern.compile("[壳]"), "殼");
        replaces.put(Pattern.compile("[尫]"), "尪");
        replaces.put(Pattern.compile("[坔]"), "湳");
        replaces.put(Pattern.compile("[𡶛]"), "卡");
        replaces.put(Pattern.compile("[拕]"), "托");
        replaces.put(Pattern.compile("[岺]"), "苓");
        replaces.put(Pattern.compile("[庙]"), "廟");
        replaces.put(Pattern.compile("[畑]"), "煙");
        replaces.put(Pattern.compile("[胆]"), "膽");
        replaces.put(Pattern.compile("[响]"), "響");
        replaces.put(Pattern.compile("[恒]"), "恆");
        replaces.put(Pattern.compile("[𫔘]"), "閂");
        replaces.put(Pattern.compile("[畓]"), "沓");
        replaces.put(Pattern.compile("[羗]"), "羌");
        replaces.put(Pattern.compile("[峯]"), "峰");
        replaces.put(Pattern.compile("[笋]"), "筍");
        replaces.put(Pattern.compile("[梹]"), "檳");
        replaces.put(Pattern.compile("[硘]"), "回");
        replaces.put(Pattern.compile("[梘]"), "見");
        replaces.put(Pattern.compile("[𦰡]"), "那");
        replaces.put(Pattern.compile("[躭]"), "耽");
        replaces.put(Pattern.compile("[脚]"), "腳");
        replaces.put(Pattern.compile("[猪]"), "豬");
        replaces.put(Pattern.compile("[𥿄]"), "紙");
        replaces.put(Pattern.compile("[啓]"), "啟");
        replaces.put(Pattern.compile("[亀]"), "龜");
        replaces.put(Pattern.compile("[戞]"), "戛");
        replaces.put(Pattern.compile("[鈎]"), "勾");
        replaces.put(Pattern.compile("[硦]"), "弄");
        replaces.put(Pattern.compile("[焿]"), "庚");
        replaces.put(Pattern.compile("[菓]"), "果");
        replaces.put(Pattern.compile("[湶]"), "泉");
        replaces.put(Pattern.compile("[堺]"), "界");
        replaces.put(Pattern.compile("[犂]"), "犁");
        replaces.put(Pattern.compile("[厦]"), "廈");
        replaces.put(Pattern.compile("[萡]"), "箔");
        replaces.put(Pattern.compile("[厨]"), "廚");
        replaces.put(Pattern.compile("[猫]"), "貓");
        replaces.put(Pattern.compile("[𡍼]"), "塗");
        replaces.put(Pattern.compile("[畬]"), "舍");
        replaces.put(Pattern.compile("[𦋐]"), "罩");
        replaces.put(Pattern.compile("[嵵]"), "時");
        replaces.put(Pattern.compile("[葱]"), "蔥");
        replaces.put(Pattern.compile("[竪]"), "豎");
        replaces.put(Pattern.compile("[塩]"), "鹽");
        replaces.put(Pattern.compile("[獇]"), "猐");
        replaces.put(Pattern.compile("[䧟]"), "陷");
        replaces.put(Pattern.compile("[槺]"), "康");
        replaces.put(Pattern.compile("[廍]"), "部");
        replaces.put(Pattern.compile("[皷]"), "鼓");
        replaces.put(Pattern.compile("[𢊬]"), "廩");
        replaces.put(Pattern.compile("[𨻶]"), "隙");
        replaces.put(Pattern.compile("[𩵺]"), "月");
        replaces.put(Pattern.compile("[𫆳]"), "曼");
        replaces.put(Pattern.compile("[蔴]"), "麻");
        replaces.put(Pattern.compile("[噍]"), "焦");
        replaces.put(Pattern.compile("[窰]"), "窯");
        replaces.put(Pattern.compile("[磘\\x{FFFA8}]"), "嗂");
        replaces.put(Pattern.compile("[関]"), "關");
        replaces.put(Pattern.compile("[鮘]"), "代");
        replaces.put(Pattern.compile("[磜]"), "祭");
        replaces.put(Pattern.compile("[𥕟]"), "漏");
        replaces.put(Pattern.compile("[𥕢]"), "槽");
        replaces.put(Pattern.compile("[舘]"), "館");
        replaces.put(Pattern.compile("[𡒸]"), "層");
        replaces.put(Pattern.compile("[藔]"), "寮");
        replaces.put(Pattern.compile("[壠]"), "壟");
        replaces.put(Pattern.compile("[\\x{FB952}]"), "齊");//米齊
        replaces.put(Pattern.compile("[𫙮]"), "桀");
        replaces.put(Pattern.compile("[鐤]"), "鼎");
        replaces.put(Pattern.compile("[𧃽]"), "應");
        replaces.put(Pattern.compile("[鷄]"), "雞");
        replaces.put(Pattern.compile("[欍]"), "舊");
        replaces.put(Pattern.compile("[𩻸]"), "逮");
        replaces.put(Pattern.compile("[鑛]"), "礦");
        replaces.put(Pattern.compile("[灧]"), "艷");
        replaces.put(Pattern.compile("[𣐤]"), "舊");
        replaces.put(Pattern.compile("[効]"), "效");
        replaces.put(Pattern.compile("[温]"), "溫");
        replaces.put(Pattern.compile("[敍]"), "敘");
        replaces.put(Pattern.compile("[𥔽]"), "塔");
        replaces.put(Pattern.compile("[卧]"), "臥");
        replaces.put(Pattern.compile("[凉]"), "涼");
        replaces.put(Pattern.compile("\\x{FB56F}"), "塭");//󻕯
        //</editor-fold>
        replaces.put(Pattern.compile("\\x{5730}\\x{4E0B}\\x{6A13}"), "-1樓");//地下樓
        replaces.put(Pattern.compile("\\x{5730}\\x{4E0B}(?=\\d+[\\x{6A13}Ff])"), "-");
        //</editor-fold>
        return replaces;
    }

    @Bean
    public SpelExpressionParser spel() {
        return new SpelExpressionParser(new SpelParserConfiguration(true, true));
    }

}
