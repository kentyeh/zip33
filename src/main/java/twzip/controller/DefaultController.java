package twzip.controller;

import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import twzip.context.Zip;

/**
 *
 * @author Kent Yeh
 */
@Controller
public class DefaultController {

    @Autowired
    Zip zip;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Callable<String> root() {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "index";
            }
        };
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public Callable<String> root(final HttpServletRequest request) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                String addr = request.getParameter("addr");
                if (addr == null || addr.isEmpty()) {
                    request.setAttribute("message", "地址不可為空");
                } else {
                    request.setAttribute("zips", zip.getZip(addr));
                }
                return "index";
            }
        };
    }

}
