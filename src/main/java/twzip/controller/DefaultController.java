package twzip.controller;

import java.util.List;
import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import twzip.context.Zip;
import twzip.model.Dao;
import twzip.model.Zip33;

/**
 *
 * @author Kent Yeh
 */
@Controller
public class DefaultController {

    private Zip zip;
    private Dao dao;

    @Autowired
    public void setZip(Zip zip) {
        this.zip = zip;
    }

    @Autowired
    public void setDao(Dao dao) {
        this.dao = dao;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Callable<String> root() {
        return () -> "index";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public Callable<String> root(final HttpServletRequest request) {
        return () -> {
            String addr = request.getParameter("addr");
            if (addr == null || addr.isEmpty()) {
                request.setAttribute("message", "地址不可為空");
            } else {
                List<Zip33> zips = zip.getZip33(addr);
                if (!zips.isEmpty()) {
                    request.setAttribute("zips", zips);
                }
            }
            return "index";
        };
    }
}
