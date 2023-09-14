package me.centralhardware.znatoki.telegram.statistic.web;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ResourceBundle;

import static me.centralhardware.znatoki.telegram.statistic.web.WebConstant.ERROR_MESSAGE;
import static me.centralhardware.znatoki.telegram.statistic.web.WebConstant.ERROR_TITLE;

@Controller
public class ZnatokiErrorController implements ErrorController {

    private final ResourceBundle resourceBundle;

    public ZnatokiErrorController(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @SuppressWarnings("SameReturnValue")
    @RequestMapping(value = "error", method = RequestMethod.GET)
    public String handleError(Model model) {
        model.addAttribute(ERROR_TITLE, resourceBundle.getString("ERROR"));
        model.addAttribute(ERROR_MESSAGE, resourceBundle.getString("HAPPENED_UNKNOWN_ERROR"));
        return Edit.ERROR_PAGE_NAME;
    }

}
