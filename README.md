## SSO Test

### 两个工程
1. ssotest  模拟sso服务，提供登录、验证（登录验证，token验证）功能
2. ssotestsub1 模拟业务服务，展示购物车商品，展示前需要已登录

两个工程都使用Spring boot，页面渲染使用thymeleaf，用户信息存储在redis中。

两个工程启动均用mvn启动： ```mvn spring-boot:run```

### 在本地模拟一个sso服务，两个业务服务
修改hosts文件，添加如下：

```
127.0.0.1 www.sso-login.com
127.0.0.1 www.sub1.com
127.0.0.1 www.sub2.com
```

端口： sso-login使用```8080```端口，sub1和sub2使用```8081```端口

模拟流程：

1. 用户查看sub1系统中的购物车商品，地址栏输入```http://www.sub1.com:8081/index```;
2. 发现未登录，跳转到```www.sso-login.com:8080/index```登录页，输入正确的用户名密码后跳转到sub1系统的展示页；
3. 用户查看sub2系统中的购物车商品，地址栏输入```http://www.sub2.com:8081/index```;
4. 因为已经登录过，直接打开展示页。




