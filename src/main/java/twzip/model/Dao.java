package twzip.model;

import java.util.List;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 *
 * @author Kent Yeh
 */
public interface Dao extends SqlObject,AutoCloseable {

    @SqlQuery("SELECT * FROM zipcas WHERE city like :city||'%'")
    @RegisterRowMapper(Cas.CasMapper.class)
    List<Cas> findCasByCity(@Bind("city") String city);
    
    @SqlQuery("SELECT * FROM zipcas WHERE area like :area||'%'")
    @RegisterRowMapper(Cas.CasMapper.class)
    List<Cas> findCasByArea(@Bind("area") String area);
    
    @SqlQuery("select * from village WHERE city like :city||'%'")
    @RegisterRowMapper(Village.VillageMapper.class)
    List<Village> findVilByCity(@Bind("city") String city);

    @SqlQuery("select * from zip6 where casid = :casid")
    @RegisterRowMapper(Zip33.Zip33Mapper.class)
    List<Zip33> findZipByCas(@BindBean Cas cas);

    /**
     * 關閉 {@link java.sql.Connection Connection}連線
     */
    @Override
    default void close() {
        getHandle().close();
    }

}
