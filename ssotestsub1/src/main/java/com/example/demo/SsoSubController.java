package com.example.demo;

import okhttp3.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaoteng
 * @date 2020/3/15
 */
@RestController
@RequestMapping("/")
public class SsoSubController {

    private static final String USERTOKEN = "userToken";

    private List<String> products = new ArrayList<String>() {
        {add("product1"); add("product2");}
    };

    private static OkHttpClient httpClient = new OkHttpClient();


    /**
     * sub1系统的列表展示功能
     * 1. 先判断是否登录系统;
     * 2. 如未登录，需要去sso服务上登录，并将返回的token存到token中;
     * 3. 如已登录，直接展示
     *
     * @return
     */
    @RequestMapping("index")
    public List<String> display(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // 验证登录  TODO 需要使用AOP对所有要验证的接口增加校验
        String loginRes = checkLogin(request, response);
        if (StringUtils.isEmpty(loginRes)) {
            return null;
        }

        // 验证登录没有问题，返回产品集合
        return products;
    }

    /**
     * 登录，没有前端，这里写死用户名密码
     * @return
     */
    private String checkLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ssoUrl = "http://www.sso-login.com:8080";

        String subUrl = request.getRequestURL().toString();

        // 如果request的header中有userToken，说明改请求是从sso重定向过来的，需要将userToken写入子系统1域的cookie中；
        // 如果没有，需要重定向sso服务上获取
        String userToken = request.getParameter(USERTOKEN);
        if (!StringUtils.isEmpty(userToken)) {
            response.addCookie(new Cookie(USERTOKEN, userToken));
        } else {
            // 当前系统的cookie
            Cookie[] cookies = request.getCookies();
            if (cookies == null || cookies.length == 0) {
                // cookie不存在，重定向到sso去登录 (目前没有实现前端界面，重定向过去显示为空，需要在postman中手动请求一下)
                response.sendRedirect(ssoUrl + "/index" + "?refer=" + subUrl);
                return null;
            } else {
                boolean hasUserTokenCookie = false;
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(USERTOKEN)) {
                        hasUserTokenCookie = true;
                        userToken = cookie.getValue();
                        break;
                    }
                }
                if (!hasUserTokenCookie) {
                    // cookie不存在，重定向到sso去登录 (目前没有实现前端界面，重定向过去显示为空，需要在postman中手动请求一下)
                    response.sendRedirect(ssoUrl + "/index" + "?refer=" + subUrl);
                    return null;
                }
            }
        }
        System.out.println("Check login userToken:" + userToken);
        // 已经获取了userToken，需要再向sso服务器验证一下有效性，防止篡改
        Request okReq = new Request.Builder()
                .url(ssoUrl + "/checkLogin")
                .header(USERTOKEN, userToken)
                .build();

        final List<String> resList = new ArrayList<>();

//        httpClient.newCall(okReq).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                resList.add(response.body().string());
//            }
//        });

        Response okResp = httpClient.newCall(okReq).execute();

//        String res = resList.get(0);
        String res = okResp.body().string();
        if (StringUtils.isEmpty(res) || res.startsWith("Error:")) {
            response.sendRedirect(ssoUrl + "/index" + "?refer=" + subUrl);
            return null;
        }
        if (!okResp.isSuccessful()) {
            response.sendRedirect(ssoUrl + "/index" + "?refer=" + subUrl);
            return null;
        }
        return res;
    }
}
