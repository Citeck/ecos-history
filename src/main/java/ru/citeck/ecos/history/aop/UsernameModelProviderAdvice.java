package ru.citeck.ecos.history.aop;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import ru.citeck.ecos.history.config.ApplicationProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class UsernameModelProviderAdvice {

    public static final String REQUEST_USERNAME = "requestUsername";

    private final ApplicationProperties properties;

    @ModelAttribute
    public void populateModel(@CookieValue(value = "alfUsername3", required = false) String fromCookie,
                              HttpServletRequest request, Model model) {

        String username = request.getParameter("username");
        if (StringUtils.isBlank(username)) {
            username = fromCookie;
        }
        if (StringUtils.isBlank(username)) {
            username = tryHeaders(properties.getTryHeaderForUsername(), request);
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(REQUEST_USERNAME, username, RequestAttributes.SCOPE_REQUEST);
        }
    }

    private String tryHeaders(String checkHeader, HttpServletRequest request) {
        return Optional.ofNullable(checkHeader)
            .filter(x -> !x.equals(""))
            .map(request::getHeader)
            .orElse(null);
    }
}
