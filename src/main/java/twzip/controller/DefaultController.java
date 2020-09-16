package twzip.controller;

import java.util.List;
import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import twzip.context.Zip;
import twzip.model.PreDataUtil;
import twzip.model.Zip33;

/**
 *
 * @author Kent Yeh
 */
@Controller
public class DefaultController {

    private Zip zip;

    @Autowired
    public void setZip(Zip zip) {
        this.zip = zip;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Callable<String> root() {
        return () -> "index";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public Callable<String> root(final HttpServletRequest request) {
        return () -> {
            String addr = request.getParameter("addr");
            String mapsearch = request.getParameter("mapsearch");
            String forrcesearch = request.getParameter("forrcesearch");
            if (addr == null || addr.isEmpty()) {
                request.setAttribute("message", "地址不可為空");
            } else {
                List<Zip33> zips = zip.getZip33(addr);
                if (!zips.isEmpty()) {
                    request.setAttribute("zips", zips);
                    if (zips.size() > 1 || "Y".equals(forrcesearch)) {
                        request.setAttribute("alternatives", PreDataUtil.mapSearch(addr));
                    }
                } else if ("Y".equals(mapsearch) || "Y".equals(forrcesearch)) {
                    request.setAttribute("alternatives", PreDataUtil.mapSearch(addr));
                }
            }
            return "index";
        };
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public final String handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex, HttpServletRequest request) {
        request.setAttribute("message", "上網查詢逾時");
        return "index";
    }
}
