package com.example.demo;

import io.netty.util.internal.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * @author zhaoteng
 * @date 2020/3/15
 */
@Controller
@RequestMapping("/")
public class SsoController {

    private static final String USERTOKEN = "userToken";

    @Autowired
    private JedisPool jedisPool;

    /**
     * 登录首页
     * @return
     */
    @RequestMapping(value="index")
    public String index(ModelMap map, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String refer = request.getParameter("refer");
        System.out.println(new Date() + "Index refer:" + refer);

        // 先判断cookie是否存在，如果存在且有效，则不再跳转到登录页
        String userToken = getTokenFromCookie(request);
        String username = userToken.split("\\.")[0];
        Jedis redis = jedisPool.getResource();
        redis.select(2);
        String token = redis.get("token:" + username);
        if (!StringUtils.isEmpty(token) && token.equals(userToken)) {
            response.sendRedirect(refer + "?" + USERTOKEN + "=" + token);
        }

        User user = new User();
        user.setUsername("tengyuanjack");
        user.setPassword("123456");
        map.put("user", user);
        map.put("refer", refer == null ? "" : refer);
        return "index";
    }

    /**
     * 登录
     * @param user
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public String login(@ModelAttribute("user") User user, @RequestParam("refer") String refer, HttpServletRequest req, HttpServletResponse resp)
            throws IOException{

        // 记录从哪儿来的请求，登录完成需重定向回去

        System.out.println("Login refer: " + refer);

        String username = user.getUsername();
        String password = user.getPassword();
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return "Login info error!";
        }
        // 和redis中进行校验，如果校验通过，返回一个带有用户名信息的token
        Jedis redis = jedisPool.getResource();
        redis.select(2);

        String pwd = redis.hget("user:db", username);
        if (!password.equals(pwd)) {
            return "Password error!";
        }
        // 生成token，设置失效时间10分钟，放到redis
        // TODO 先模拟一个token
        String token = username + "." + System.currentTimeMillis();
        redis.set("token:" + username, token);
        redis.expire("token:" + username, 10 * 60);

        // 写cookie
        resp.addCookie(new Cookie(USERTOKEN, token));

        // 重定向回来源url
        // sendRedirect方法等价于 1. setStatus(302) 2. setHeader("Location", originUrl)
        // 写到header里，供originUrl用
        if (!StringUtils.isEmpty(refer)) {
            resp.sendRedirect(refer + "?" + USERTOKEN + "=" + token);
        }

        System.out.println("Login return token:" + token);

        return token;
    }

    /**
     * 业务系统第一次登录时需要重定向到sso获取，获取后设置到自己的域名下
     * 根据cookie获取token
     * @param req
     * @return
     */
    @ResponseBody
    @RequestMapping("checkCookie")
    public String checkCookie(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String originUrl = req.getParameter("refer");

        Cookie[] cookies = req.getCookies();
        if (cookies == null || cookies.length == 0) {
            resp.setStatus(403);
            ErrorMsg msg = new ErrorMsg("登录信息失效，需要重新登录");
            return msg.toString();
        }
        String token = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(USERTOKEN)) {
                token = cookie.getValue();
                break;
            }
        }

        resp.sendRedirect(originUrl);

        return token;
    }

    private String getTokenFromCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        String token = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(USERTOKEN)) {
                token = cookie.getValue();
                break;
            }
        }
        return token;
    }

    /**
     * 检查业务系统传过来的token的有效性 <br/>
     * 业务系统后台调用
     * 约定userToken不能传空
     *
     * TODO 没有验证token
     *
     * @param userToken
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "checkLogin", method = RequestMethod.GET)
    public String checkLogin(@RequestHeader("userToken") String userToken) {
        String username = userToken.split("\\.")[0];
        Jedis redis = jedisPool.getResource();
        redis.select(2);
        String token = redis.get("token:" + username);
        if (StringUtils.isEmpty(token) || !token.equals(userToken)) {
            ErrorMsg msg = new ErrorMsg("未登录或登录信息已过期，需要重新登录");
            return msg.toString();
        }

        // 验证成功一次，就把token的过期时间重置
        redis.expire("token:" + username, 10 * 60);

        return token;
    }


}
