package twzip.model;

import java.util.Iterator;
import java.util.List;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

/**
 *
 * @author Kent Yeh
 */
public interface Dao extends Transactional<Dao>, AutoCloseable {

    /**
     * 判斷資料表格是否存在
     *
     * @param tbl 表格名稱
     * @return
     */
    @SqlQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME= UPPER(:tbl)")
    String tableExists(@Bind("tbl") String tbl);

    /**
     * 刪除郵遞區號表格
     */
    @SqlUpdate("DROP TABLE IF EXISTS post5")
    void dropPost5();

    /**
     * 建立郵遞區號表格
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS post5 ("
            + "pid bigint auto_increment primary key,"
            + "zipcode varchar(5),"
            + "city    varchar(3),"
            + "area varchar(4),"
            + "oriInfo varchar(30),"
            + "addrinfo varchar(30),"
            + "sec int,"
            + "tailinfo varchar(50),"
            + "lane varchar(10),"
            + "alley varchar(10),"
            + "parnums DECIMAL(8, 3),"
            + "parnume DECIMAL(8, 3),"
            + "floors int,"
            + "floore int,"
            + "express text,"
            + "boundary text)")
    void createPost5();

    /**
     * 刪除行政區村里表格
     */
    @SqlUpdate("DROP TABLE IF EXISTS village")
    void dropVillage();

    /**
     * 建立行政區村里表格
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS village("
            + "city varchar(3) not null,"
            + "dist varchar(4) not null,"
            + "vil varchar(5) not null)")
    void createVillage();

    /**
     * 新增村里資料
     *
     * @param district 村里資料
     * @return
     */
    @SqlUpdate("INSERT INTO village(city,dist,vil) values( :city, :dist, :vil)")
    int insertVillage(@BindBean Village district);

    /**
     * 計算村里資料筆數
     *
     * @return
     */
    @SqlQuery("SELECT COUNT(8) FROM village")
    long vilCount();

    /**
     * 計算郵遞區號設定筆數
     *
     * @return
     */
    @SqlQuery("SELECT COUNT(8) FROM post5")
    long post5Count();

    /**
     * 新增郵遞區號設定
     *
     * @param post5
     * @return
     */
    @SqlUpdate("INSERT INTO post5(zipcode,city,area,oriInfo,addrinfo,sec,tailInfo,lane,"
            + "alley,parnums,parnume,floors,floore,express,boundary)"
            + " values(:zipcode, :city, :area, :oriInfo, :addrinfo, :sec, :tailInfo, :lane,"
            + " :alley, :parnums, :parnume, :floors, :floore, :express, :boundary)")
    @GetGeneratedKeys
    long insertPost5(@BindBean Post5 post5);

    /**
     * 以縣市取得村里資料
     *
     * @param city 縣市
     * @return
     */
    @SqlQuery("SELECT * FROM village WHERE city like :city || '%'")
    @RegisterMapper(Village.DistrictMapper.class)
    List<Village> findVillages(@Bind("city") String city);

    /**
     * 取縣市鄉鎮區之村里
     *
     * @param city 縣市
     * @param dist 鄉鎮區
     * @return
     */
    @SqlQuery("SELECT * FROM village WHERE city = :city AND dist = :dist")
    @RegisterMapper(Village.DistrictMapper.class)
    List<Village> findVillages(@Bind("city") String city, @Bind("dist") String dist);

    /**
     * 取得所有郵遞區號設定
     *
     * @return
     */
    @RegisterMapper(Post5.Post5Mapper.class)
    @SqlQuery("SELECT * FROM POST5 ORDER BY PID")
    Iterator<Post5> findAllPost5();

    /**
     * 以縣市取得郵遞區號設定
     *
     * @param city
     * @return
     */
    @RegisterMapper(Post5.Post5Mapper.class)
    @SqlQuery("SELECT * FROM POST5 WHERE city like :city ||'%' ORDER BY PID")
    List<Post5> findByCity(@Bind("city") String city);

    /**
     * 關閉 {@link java.sql.Connection Connection}連線
     */
    @Override
    void close();

}
