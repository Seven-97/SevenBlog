---
title: 支付技术
category: 系统设计
tag:
  - 支付技术
---





## 微信支付

### 流程

1. 获取商户号

微信商户平台：https://pay.weixin.qq.com/ 步骤：申请成为商户 => 提交资料 => 签署协议 => 获取商户号

2. 获取AppID

微信公众平台：https://mp.weixin.qq.com/ 步骤：注册服务号 => 服务号认证 => 获取APPID => 绑定商户号

3. 申请商户证书

步骤：登录商户平台 => 选择 账户中心 => 安全中心 => API安全 => 申请API证书 包括商户证书和商户私钥

4. 获取微信的证书

可以预先下载，也可以通过编程的方式获取。

5. 获取APIv3秘钥（在微信支付回调通知和商户获取平台证书使用APIv3密钥）

步骤：登录商户平台 => 选择 账户中心 => 安全中心 => API安全 => 设置APIv3密钥

 

 

 

### 开发案例

1. 引依赖

```xml
<!--wechatpay-sdk-->
<dependency>
    <groupId>com.github.wechatpay-apiv3</groupId>
    <artifactId>wechatpay-apache-httpclient</artifactId>
    <version>0.4.5</version>
</dependency>
```



2. 写配置类

```java
@Configuration
@PropertySourse("classpath:wxpay.properties")//读取配置文件
@ConfigurationProperties(prefix = "wxpay")//读取wxpay节点
public class WechatpayConfig{
    // 商户号
    @Value("${wechat-pay.merchant-id}")
    private String merchantId;
    
    // API证书序列号
    @Value("${wechat-pay.merchant-serial-number}")
    private String merchantSerialNumber;
    
    // 私钥文件
    @Value("${wechat-pay.private-key}")
    private String privateKey;
    
    // APIV3密钥
    @Value("${wechat-pay.api-v3-key}")
    private String apiV3Key;
    /**
     * 给容器中加入WechatPay的HttpClient,虽然它是WechatPay的,
     * 但可以用它给任何外部发请求,因为它只对发给WechatPay的请求做处理而不对发给别的的请求做处理.
     */
    @Bean
    public HttpClient httpClient(){
        //私钥
        PrivateKey merchantPrivateKey=PemUtil.loadPrivateKey(privateKey);
        //微信证书校验器
        Verifier verifier=null;
        try{
            //获取证书管理器实例
            CertificatesManager certificatesManager=CertificatesManager.getInstance();
            //向证书管理器增加需要自动更新平台证书的商户信息(默认时间间隔:24小时)
            certificatesManager.putMerchant(merchantId,new WechatPay2Credentials(merchantId,new PrivateKeySigner(merchantSerialNumber,merchantPrivateKey)),apiV3Key.getBytes(StandardCharsets.UTF_8));
            //从证书管理器中获取verifier
            verifier=certificatesManager.getVerifier(merchantId);
        }
        catch(Exception e){
            new RuntimeException("微信证书校验器配置失败");
        }
        WechatPayHttpClientBuilder builder=WechatPayHttpClientBuilder.create()
                                                                     .withMerchant(merchantId,merchantSerialNumber,merchantPrivateKey)
                                                                     .withValidator(new WechatPay2Validator(verifier));
        CloseableHttpClient httpClient=builder.build();
        return httpClient;
    }
    /**
     * 和上面相比只是不需要验证签名了
     * @return
     */
    @Bean
    public HttpClient httpClientWithNoSign(){
        //私钥
        PrivateKey merchantPrivateKey=PemUtil.loadPrivateKey(privateKey);
        //微信证书校验器
        Verifier verifier=null;
        try{
            //获取证书管理器实例
            CertificatesManager certificatesManager=CertificatesManager.getInstance();
            //向证书管理器增加需要自动更新平台证书的商户信息(默认时间间隔:24小时)
            certificatesManager.putMerchant(merchantId,new WechatPay2Credentials(merchantId,new PrivateKeySigner(merchantSerialNumber,merchantPrivateKey)),apiV3Key.getBytes(StandardCharsets.UTF_8));
            //从证书管理器中获取verifier
            verifier=certificatesManager.getVerifier(merchantId);
        }
        catch(Exception e){
            new RuntimeException("微信证书校验器配置失败");
        }
        WechatPayHttpClientBuilder builder=WechatPayHttpClientBuilder.create()
                                                                     .withMerchant(merchantId,merchantSerialNumber,merchantPrivateKey)
                                                                     .withValidator(response->true);
        CloseableHttpClient httpClient=builder.build();
        return httpClient;
    }
}
```



3. 写配置

```xml
wechat-pay:
  #接下来两个用来标识用户
  #商户id
  merchant-id: xxxxxxxxxxx
  #公众号appid(和商户id绑定过)
  appid: xxxxxxxxxxx
  #接下来两个用来确保SSL(内容未作任何加密,只做了签名.)
  #商户证书序列号
  merchant-serial-number: xxxxxxxxxxx
  #商户私钥
  private-key: xxxxxxxxxxx
  #APIv3密钥(在微信支付回调通知和商户获取平台证书使用APIv3密钥)
  api-v3-key: xxxxxxxxxxx
  #接下来两个是相关地址
  #微信服务器地址
  domain: https://api.mch.weixin.qq.com
  #接收结果通知地址
  notify-domain: xxxxxxxxxxx
```



4. 使用

**以下涉及的共有的内容**

```java
public class WechatPayConstant{
    public static final String CANCEL_PAY_URL="/v3/pay/transactions/out-trade-no/%s/close";
    public static final String CREATE_PAY_URL="/v3/pay/transactions/native";
    public static final String QUERY_PAY_URL="/v3/pay/transactions/out-trade-no/%s?mchid=%s";
    public static final String CREATE_REFUND_URL="/v3/refund/domestic/refunds";
    public static final String QUERY_REFUND_URL="/v3/refund/domestic/refunds/%s";
    public static final String TRADE_BILL_URL="/v3/bill/tradebill?bill_date=%s&bill_type=%s";
    public static final String FLOW_BILL_URL="/v3/bill/fundflowbill?bill_date=%s";
    public static final String TRADE_STATE_SUCCESS="SUCCESS";
    public static final String REFUND_STATE_SUCCESS="SUCCESS";
}
```

```java
@Value("${wechat-pay.merchant-id}")
private String merchantId;
@Value("${wechat-pay.merchant-serial-number}")
private String merchantSerialNumber;
@Value("${wechat-pay.api-v3-key}")
private String apiV3Key;
@Value("${wechat-pay.domain}")
private String domain;
@Value("${wechat-pay.appid}")
private String appId;
@Value("${wechat-pay.notify-url}")
private String notifyUrl;
@Autowired
private HttpClient httpClient;
@Autowired
private HttpClient httpClientWithNoSign;
```



### 支付

流程图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272222065.png)

 

创建支付

**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_1.shtml**

```java
private String createPay(OrderInfo orderInfo) throws Exception{
    //请求构造
    HttpPost httpPost=new HttpPost(domain+WechatPayConstant.CREATE_PAY_URL);
    //请求体
    //构造数据
    HashMap<String,Object> reqData=new HashMap<>();
    reqData.put("appid",appId);
    reqData.put("mchid",merchantId);
    reqData.put("description",orderInfo.getTitle());
    reqData.put("out_trade_no",orderInfo.getOrderNo());
    reqData.put("notify_url",notifyUrl+"/pay/order/order-signal");
    HashMap<String,Integer> amount=new HashMap<>();
    //单位是分
    amount.put("total",orderInfo.getTotalFee());
    reqData.put("amount",amount);
    String jsonReqData=new Gson().toJson(reqData);
    StringEntity entity=new StringEntity(jsonReqData,"utf-8");
    entity.setContentType("application/json");
    httpPost.setEntity(entity);
    //请求头
    httpPost.setHeader("Accept","application/json");
    //完成签名并执行请求
    CloseableHttpResponse response=(CloseableHttpResponse)httpClient.execute(httpPost);
    Map<String,String> dataMap=null;
    try{
        int statusCode=response.getStatusLine()
                               .getStatusCode();
        //成功
        if(statusCode==200){
            String body=EntityUtils.toString(response.getEntity());
            dataMap=new Gson().fromJson(body,HashMap.class);
        }
        //失败
        else{
            if(statusCode!=204){
                String body=EntityUtils.toString(response.getEntity());
                log.error(body);
                return null;
            }
        }
    }
    finally{
        response.close();
    }
    //返回二维码的地址
    return dataMap.get("code_url");
}
```



支付通知

**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_5.shtml**

```java
@PostMapping("/pay-signal")
public HashMap<String,String> paySignal(@RequestBody Map<String,Object> signalRes,HttpServletResponse response){
    log.debug("收到微信回调");
    try{
        //TODO:验签
        //用密文解密出明文
        Map<String,String> resource=(Map<String,String>)signalRes.get("resource");
        String ciphertext=resource.get("ciphertext");
        String associatedData=resource.get("associated_data");
        String nonce=resource.get("nonce");
        String plainText=new AesUtil(apiV3Key.getBytes(StandardCharsets.UTF_8)).decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),nonce.getBytes(StandardCharsets.UTF_8),ciphertext);
        //转换
        HashMap<String,Object> data=new Gson().fromJson(plainText,HashMap.class);
        //从数据库中查出对应的订单
        QueryWrapper<OrderInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("order_no",data.get("out_trade_no"));
        OrderInfo orderInfo=orderInfoService.getOne(queryWrapper);
        synchronized(this){
            if(orderInfo.getOrderStatus()
               .equals(OrderInfo.NOT_PAIED)){
                //将订单设置为已支付状态
                orderInfo.setOrderStatus(OrderInfo.PAIED);
                //更新订单状态
                orderInfoService.updateById(orderInfo);
                //添加支付记录
                PaymentInfo paymentInfo=new PaymentInfo();
                paymentInfo.setOrderNo(orderInfo.getOrderNo());
                paymentInfo.setCreateTime(new Date());
                paymentInfo.setUpdateTime(new Date());
                paymentInfo.setPayerTotal(orderInfo.getTotalFee());
                paymentInfo.setPaymentType(PaymentInfo.WECHAT_PAY);
                //微信支付中的支付编号
                paymentInfo.setTransactionId((String)data.get("transaction_id"));
                //交易类型(扫码 刷脸等等)
                paymentInfo.setTradeType((String)data.get("trade_type"));
                paymentInfo.setTradeState((String)data.get("trade_state"));
                //存放全部数据(json)以备不时之需
                paymentInfo.setContent(plainText);
                paymentInfoService.save(paymentInfo);
                log.info("订单{}的支付记录添加成功,支付记录id为{}.",orderInfo.getOrderNo(),paymentInfo.getId());
            }
            else{
                log.debug("订单{}状态为{},回调处理退出.",orderInfo.getOrderNo(),OrderInfo.PAIED);
            }
        }
        return null;
    }
    catch(Exception e){
        log.error(e.getMessage());
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        HashMap<String,String> map=new HashMap<>();
        map.put("code","FAIL");
        map.put("message","支付失败");
        return map;
    }
}
```



查询支付

**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_2.shtml**

```java
private Map<String,Object> queryPay(String orderNo) throws Exception{
    //请求构造
    HttpGet httpGet=new HttpGet(String.format(domain+WechatPayConstant.QUERY_PAY_URL,orderNo,merchantId));
    //请求头
    httpGet.setHeader("Accept","application/json");
    //完成签名并执行请求
    CloseableHttpResponse response=(CloseableHttpResponse)httpClient.execute(httpGet);
    Map<String,Object> dataMap=null;
    try{
        int statusCode=response.getStatusLine()
            .getStatusCode();
        String body=EntityUtils.toString(response.getEntity());
        //成功
        if(statusCode<400){
            dataMap=new Gson().fromJson(body,HashMap.class);
            log.info("查询订单支付{}成功",orderNo);
        }
        //失败
        else{
            log.error("查询订单支付{}失败,返回数据为{}.",orderNo,body);
            throw new IOException("订单查询失败");
        }
    }
    finally{
        response.close();
    }
    return dataMap;
}
```



取消支付

[**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_3.shtml**](https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_3.shtml)

```java
private boolean cancelPay(String orderNo){
    //请求构造
    HttpPost httpPost=new HttpPost(String.format(domain+WechatPayConstant.CANCEL_PAY_URL,orderNo));
    //请求体
    //构造数据
    HashMap<String,Object> reqData=new HashMap<>();
    reqData.put("mchid",merchantId);
    String jsonReqData=new Gson().toJson(reqData);
    StringEntity entity=new StringEntity(jsonReqData,"utf-8");
    entity.setContentType("application/json");
    httpPost.setEntity(entity);
    //请求头
    httpPost.setHeader("Accept","application/json");
    CloseableHttpResponse response=null;
    try{
        //完成签名并执行请求
        response=(CloseableHttpResponse)httpClient.execute(httpPost);
        Map<String,String> dataMap=null;
        int statusCode=response.getStatusLine()
            .getStatusCode();
        //成功
        if(statusCode==204){
            log.info("取消订单{}成功",orderNo);
            return true;
        }
        //失败
        else{
            log.error("取消订单{}失败",orderNo);
            return false;
        }
    }
    catch(Exception e){
        log.error("取消订单{}失败",orderNo);
        return false;
    }
    finally{
        try{
            response.close();
        }
        catch(IOException e){
            log.error("关闭response失败");
        }
    }
}
```



### 退款

创建退款

[**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_10.shtml**](https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_10.shtml)

```java
private String createRefund(RefundInfo refundInfo) throws Exception{
    //请求构造
    HttpPost httpPost=new HttpPost(domain+WechatPayConstant.CREATE_REFUND_URL);
    //请求体
    //构造数据
    HashMap<String,Object> reqData=new HashMap<>();
    reqData.put("out_trade_no",refundInfo.getOrderNo());//订单编号
    reqData.put("out_refund_no",refundInfo.getRefundNo());//退款单编号
    reqData.put("reason",refundInfo.getReason());//退款原因
    reqData.put("notify_url",notifyUrl+"/pay/order/refund-signal");//退款通知地址
    HashMap<String,Object> amount=new HashMap<>();
    amount.put("refund",refundInfo.getRefund());//退款金额
    amount.put("total",refundInfo.getTotalFee());//原订单金额
    amount.put("currency","CNY");//币种
    reqData.put("amount",amount);
    //将参数转换成json字符串
    String jsonData=new Gson().toJson(reqData);
    log.info("请求参数 ===> {}"+jsonData);
    StringEntity entity=new StringEntity(jsonData,"utf-8");
    httpPost.setEntity(entity);//将请求报文放入请求对象
    //请求头
    httpPost.setHeader("content-type","application/json");
    httpPost.setHeader("Accept","application/json");//设置响应报文格式
    //完成签名并执行请求
    CloseableHttpResponse response=(CloseableHttpResponse)httpClient.execute(httpPost);
    try{
        //解析响应结果
        String bodyAsString=EntityUtils.toString(response.getEntity());
        int statusCode=response.getStatusLine()
                               .getStatusCode();
        if(statusCode==200){
            log.info("成功, 退款返回结果 = "+bodyAsString);
            return bodyAsString;
        }
        else{
            if(statusCode!=204){
                log.warn("退款异常:"+bodyAsString);
            }
            return null;
        }
    }
    finally{
        response.close();
    }
}
```



退款通知

[**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_11.shtml**](https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_11.shtml)

```java
@PostMapping("/refund-signal")
public HashMap<String,String> refundSignal(@RequestBody Map<String,Object> signalRes,HttpServletResponse response){
    log.debug("收到微信回调");
    try{
        Map<String,String> resource=(Map<String,String>)signalRes.get("resource");
        String ciphertext=resource.get("ciphertext");
        String associatedData=resource.get("associated_data");
        String nonce=resource.get("nonce");
        //解密出明文
        String plainText=new AesUtil(apiV3Key.getBytes(StandardCharsets.UTF_8)).decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),nonce.getBytes(StandardCharsets.UTF_8),ciphertext);
        //转换
        HashMap<String,Object> data=new Gson().fromJson(plainText,HashMap.class);
        //从数据库中查出对应的退款信息
        QueryWrapper<RefundInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("refund_no",data.get("out_refund_no"));
        RefundInfo refundInfo=refundInfoService.getOne(queryWrapper);
        synchronized(this){
            if(RefundInfo.REFUND_PROCESSING.equals(refundInfo.getRefundStatus())){
                //将退款状态设置为退款成功
                refundInfo.setRefundStatus(RefundInfo.REFUND_SUCCESS);
                //存放全部数据(json)以备不时之需
                refundInfo.setContentNotify(plainText);
                //更新退款
                refundInfoService.updateById(refundInfo);
                //更新订单的状态
                String orderNo=refundInfo.getOrderNo();
                QueryWrapper<OrderInfo> orderInfoQueryWrapper=new QueryWrapper<>();
                orderInfoQueryWrapper.eq("order_no",orderNo);
                OrderInfo orderInfoToUpdate=new OrderInfo();
                orderInfoToUpdate.setOrderStatus(OrderInfo.REFUNDED);
                orderInfoService.update(orderInfoToUpdate,orderInfoQueryWrapper);
                log.debug("退款成功,退款单为{},对应的订单为{}.",refundInfo.getRefundNo(),refundInfo.getOrderNo());
            }
            else{
                log.debug("退款单{}状态为{},回调处理退出.",refundInfo.getRefundNo(),refundInfo.getRefundStatus());
            }
        }
        HashMap<String,String> map=new HashMap<>();
        map.put("code","SUCCESS");
        map.put("message","成功");
        return map;
    }
    catch(Exception e){
        log.error(e.getMessage());
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        HashMap<String,String> map=new HashMap<>();
        map.put("code","FAIL");
        map.put("message","失败");
        return map;
    }
}
```



查询退款

[**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_10.shtml**](https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_10.shtml)

```java
private Map<String,Object> queryRefund(String refundNo) throws Exception{
    //请求构造
    HttpGet httpGet=new HttpGet(String.format(domain+WechatPayConstant.QUERY_REFUND_URL,refundNo));
    //请求头
    httpGet.setHeader("Accept","application/json");
    //完成签名并执行请求
    CloseableHttpResponse response=(CloseableHttpResponse)httpClient.execute(httpGet);
    Map<String,Object> dataMap=null;
    try{
        int statusCode=response.getStatusLine()
            .getStatusCode();
        String body=EntityUtils.toString(response.getEntity());
        //成功
        if(statusCode<400){
            dataMap=new Gson().fromJson(body,HashMap.class);
            log.info("查询退款{}成功",refundNo);
        }
        //失败
        else{
            log.warn("查询退款{}失败,返回数据为{}.",refundNo,body);
        }
    }
    finally{
        response.close();
    }
    return dataMap;
}
```



### 账单

查询交易账单（注重交易双方）下载URL

[**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_6.shtml**](https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_6.shtml)

```java
/**
 * @param billDate 格式:yyyy-MM-dd 5.6日的账单记录的时间为05-06 9:00到05-07 9:00,并且在05-07 9:00后才能查到.
 * @param billType ALL(支付成功且未退款+支付成功且退款) SUCCESS(支付成功且未退款) REFUND(支付成功且退款)
 * @return 账单下载url(30s后则失效)
 */
private String queryTradeBillDownloadUrl(String billDate,String billType)throws Exception{
    //请求构造
    HttpGet httpGet=new HttpGet(String.format(domain+WechatPayConstant.TRADE_BILL_URL,billDate,billType));
    //请求头
    httpGet.setHeader("Accept","application/json");
    //完成签名并执行请求
    CloseableHttpResponse response=(CloseableHttpResponse)httpClient.execute(httpGet);
    String downloadUrl=null;
    try{
        int statusCode=response.getStatusLine()
                               .getStatusCode();
        String body=EntityUtils.toString(response.getEntity());
        //成功
        if(statusCode<400){
            downloadUrl=(String)new Gson().fromJson(body,HashMap.class)
                                          .get("download_url");
            return downloadUrl;
        }
        //失败
        else{
            log.warn("查询downloadUrl失败,返回数据为{}.",body);
            return null;
        }
    }
    catch(Exception e){
        log.warn("查询downloadUrl失败");
        return null;
    }
    finally{
        response.close();
    }
}
```



查询流水账单（只注重商户）下载URL

[**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_7.shtml**](https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_7.shtml)

```java
/**
 * @param billDate 格式:yyyy-MM-dd 5.6日的账单记录的时间为05-06 9:00到05-07 9:00,并且在05-07 9:00后才能查到.
 * @return 账单下载url(30s后则失效)
 */
private String queryFlowBillDownloadUrl(String billDate) throws Exception{
    //请求构造
    HttpGet httpGet=new HttpGet(String.format(domain+WechatPayConstant.FLOW_BILL_URL,billDate));
    //请求头
    httpGet.setHeader("Accept","application/json");
    //完成签名并执行请求
    CloseableHttpResponse response=(CloseableHttpResponse)httpClient.execute(httpGet);
    String downloadUrl=null;
    try{
        int statusCode=response.getStatusLine()
                               .getStatusCode();
        String body=EntityUtils.toString(response.getEntity());
        //成功
        if(statusCode<400){
            downloadUrl=(String)new Gson().fromJson(body,HashMap.class)
                                          .get("download_url");
            return downloadUrl;
        }
        //失败
        else{
            log.warn("查询downloadUrl失败,返回数据为{}.",body);
            return null;
        }
    }
    catch(Exception e){
        log.warn("查询downloadUrl失败");
        return null;
    }
    finally{
        response.close();
    }
}
```



获取账单（包括交易/流水）数据

[**https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_8.shtml**](https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_8.shtml)

```java
/**
 * 调用微信支付的接口只返回数据,然后我们把数据返回给前端,前端将其转化为excel.
 * @param downloadUrl 交易/流水账单的下载URL(即上面两个方法的返回值)
 */
private String downloadBill(String downloadUrl)throws Exception{
    //请求构造
    HttpGet httpGet=new HttpGet(downloadUrl);
    //请求头
    httpGet.addHeader("Accept","application/json");
    //完成签名并执行请求(注意:这里必须用httpClientWithNoSign,因为这个请求返回的数据是没有签名的.)
    CloseableHttpResponse response=(CloseableHttpResponse)httpClientWithNoSign.execute(httpGet);
    try{
        String bodyAsString=EntityUtils.toString(response.getEntity());
        int statusCode=response.getStatusLine()
                               .getStatusCode();
        if(statusCode<400){
            log.debug("下载账单成功");
            //存放数据
            return bodyAsString;
        }
        else{
            log.warn("下载账单失败,返回结果为:{}",bodyAsString);
            return null;
        }
    }
    catch(Exception e){
        log.warn("下载账单失败");
        return null;
    }
    finally{
        response.close();
    }
}
```



完整下载账单的操作

```java
@GetMapping("/download-trade-bill")
public AjaxResult downloadTradeBill(String billDate,@RequestParam(defaultValue="ALL") String billType)throws Exception{
    //获取downloadUrl
    String downloadUrl=queryTradeBillDownloadUrl(billDate,billType);
    log.debug("downloadUrl:{}",downloadUrl);
    //访问downloadUrl获取数据
    String returnData=downloadBill(downloadUrl);
    HashMap<String,String> resultMap=new HashMap<>();
    resultMap.put("result",returnData);
    return AjaxResult.success(AjaxResult.QUERY_SUCCESS,resultMap);
}
@GetMapping("/download-flow-bill")
public AjaxResult downloadFlowBill(String billDate) throws Exception{
    //获取downloadUrl
    String downloadUrl=queryFlowBillDownloadUrl(billDate);
    log.debug("downloadUrl:{}",downloadUrl);
    //访问downloadUrl获取数据
    String returnData=downloadBill(downloadUrl);
    HashMap<String,String> resultMap=new HashMap<>();
    resultMap.put("result",returnData);
    return AjaxResult.success(AjaxResult.QUERY_SUCCESS,resultMap);
}
```



 

## 支付宝支付

支付宝相对于微信支付更人性化，且细节做得更好。

**标识商户身份的信息、商户的证书和私钥、支付宝的证书、支付宝API的URL**

- 正式环境

  - 1.创建应用

  - 2.绑定应⽤

  - 3.配置秘钥

  - 4.上线应⽤

  - 5.签约功能

- 沙箱环境（虚拟环境）

1. 引依赖：

```xml
<!--ali-pay-sdk-->
<dependency>
    <groupId>com.alipay.sdk</groupId>
    <artifactId>alipay-sdk-java</artifactId>
    <version>4.23.0.ALL</version>
</dependency>
```



2. 写配置类

```java
@Configuration
public class AlipayClientConfig{
    @Value("${ali-pay.app-id}")
    private String appId;
    @Value("${ali-pay.merchant-private-key}")
    private String merchantPrivateKey;
    @Value("${ali-pay.ali-pay-public-key}")
    private String aliPayPublicKey;
    @Value("${ali-pay.gateway-url}")
    private String gatewayUrl;
    @Bean
    public AlipayClient alipayClient() throws AlipayApiException{
        AlipayConfig alipayConfig=new AlipayConfig();
        //设置appId
        alipayConfig.setAppId(appId);
        //设置商户私钥
        alipayConfig.setPrivateKey(merchantPrivateKey);
        //设置支付宝公钥
        alipayConfig.setAlipayPublicKey(aliPayPublicKey);
        //设置支付宝网关
        alipayConfig.setServerUrl(gatewayUrl);
        //设置请求格式,固定值json.
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        //设置字符集
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        //设置签名类型
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);
        //构造client
        AlipayClient alipayClient=new DefaultAlipayClient(alipayConfig);
        return alipayClient;
    }
}
```



3. 写配置

```xml
ali-pay:
  #接下来两个用来标识用户
  #商户id
  pid: xxxxxxxxxxx
  #应用appid(和商户id绑定过)
  app-id: xxxxxxxxxxx
  #接下来三个用来确保SSL
  #商户私钥
  merchant-private-key: xxxxxxxxxxx
  #支付宝公钥
  ali-pay-public-key: xxxxxxxxxxx
  #TODO:  #对称加密密钥
  #  content-key:
  #接下来三个是相关地址
  #支付宝网关
  gateway-url: https://openapi.alipaydev.com/gateway.do
  #接收结果通知地址
  notify-url: xxxxxxxxxxx
  #页面跳转同步通知页面路径
  return-url: xxxxxxxxxxx
```



### 开发案例

以下涉及的共有的内容

```java
public class AliPayConstant{
    public static final String TRADE_STATE_SUCCESS="TRADE_SUCCESS";
    public static final String REFUND_STATE_SUCCESS="REFUND_SUCCESS";
}
```

```java
@Value("${ali-pay.ali-pay-public-key}")
private String aliPayPublicKey;
@Value("${ali-pay.notify-url}")
private String notifyUrl;
@Value("${ali-pay.return-url}")
private String returnUrl;
@Autowired
private AlipayClient alipayClient;
```



### 支付

流程图

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272223358.png)

创建支付

[**https://opendocs.alipay.com/apis/028r8t?scene=22**](https://opendocs.alipay.com/apis/028r8t?scene=22)

```java
private String createPay(OrderInfo orderInfo){
    //请求
    AlipayTradePagePayRequest request=new AlipayTradePagePayRequest();
    //数据
    AlipayTradePagePayModel bizModel=new AlipayTradePagePayModel();
    bizModel.setOutTradeNo(orderInfo.getOrderNo());
    //单位是元
    bizModel.setTotalAmount(orderInfo.getTotalFee()
                            .toString());
    bizModel.setSubject(orderInfo.getTitle());
    //默认的
    bizModel.setProductCode("FAST_INSTANT_TRADE_PAY");
    request.setBizModel(bizModel);
    request.setNotifyUrl(notifyUrl+"/pay/order/pay-signal");
    //用户支付后支付宝会以GET方法请求returnUrl,并且携带out_trade_no,trade_no,total_amount等参数.

    request.setReturnUrl(returnUrl);
    AlipayTradePagePayResponse response=null;
    try{
        //完成签名并执行请求
        response=alipayClient.pageExecute(request);
        if(response.isSuccess()){
            log.debug("调用成功");
            return response.getBody();
        }
        else{
            log.error("调用失败");
            log.error(response.getMsg());
            return null;
        }
    }
    catch(AlipayApiException e){
        log.error("调用异常");
        return null;
    }
}
```



支付通知

[**https://opendocs.alipay.com/open/270/105902**](https://opendocs.alipay.com/open/270/105902)

```java
@PostMapping("/pay-signal")
public String paySignal(@RequestBody Map<String,String> data){
    log.debug("收到支付宝回调");
    //验签
    boolean signVerified=false;
    try{
        signVerified=AlipaySignature.rsaCheckV1(data,aliPayPublicKey,AlipayConstants.CHARSET_UTF8,AlipayConstants.SIGN_TYPE_RSA2);
        //验签成功
        if(signVerified){
            log.debug("验签成功");
            //从数据库中查出对应的订单
            QueryWrapper<OrderInfo> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("order_no",data.get("out_trade_no"));
            OrderInfo orderInfo=orderInfoService.getOne(queryWrapper);
            synchronized(this){
                if(orderInfo.getOrderStatus()
                   .equals(OrderInfo.NOT_PAIED)){
                    //将订单设置为已支付状态
                    orderInfo.setOrderStatus(OrderInfo.PAIED);
                    //更新订单状态
                    orderInfoService.updateById(orderInfo);
                    //添加支付记录
                    PaymentInfo paymentInfo=new PaymentInfo();
                    paymentInfo.setOrderNo(orderInfo.getOrderNo());
                    paymentInfo.setCreateTime(new Date());
                    paymentInfo.setUpdateTime(new Date());
                    paymentInfo.setPayerTotal(orderInfo.getTotalFee());
                    paymentInfo.setPaymentType(PaymentInfo.ALI_PAY);
                    //支付宝支付中的支付编号
                    paymentInfo.setTransactionId(data.get("trade_no"));
                    //交易类型(扫码 登录等等)
                    paymentInfo.setTradeType("扫码/登录支付");
                    paymentInfo.setTradeState(data.get("trade_status"));
                    //存放全部数据(json)以备不时之需
                    paymentInfo.setContent(gson.toJson(data));
                    paymentInfoService.save(paymentInfo);
                    log.info("订单{}的支付记录添加成功,支付记录id为{}.",orderInfo.getOrderNo(),paymentInfo.getId());
                }
                else{
                    log.debug("订单{}状态为{},回调处理退出.",orderInfo.getOrderNo(),OrderInfo.PAIED);
                }
            }
            //除了success外其他返回均认为是失败
            return "success";
        }
        //验签失败
        else{
            log.error("验签失败");
            return "failure";
        }
    }
    catch(AlipayApiException e){
        log.error("验签异常");
        return "failure";
    }
}
```



查询支付

[**https://opendocs.alipay.com/open/028woa**](https://opendocs.alipay.com/open/028woa)

```java
private Map<String,Object> queryPay(String orderNo){
    //请求
    AlipayTradeQueryRequest request=new AlipayTradeQueryRequest();
    //数据
    AlipayTradeQueryModel bizModel=new AlipayTradeQueryModel();
    bizModel.setOutTradeNo(orderNo);
    request.setBizModel(bizModel);
    try{
        //完成签名并执行请求
        AlipayTradeQueryResponse response=alipayClient.execute(request);
        if(response.isSuccess()){
            log.debug("查询订单{}成功",orderNo);
            HashMap<String,Object> resultMap=gson.fromJson(response.getBody(),HashMap.class);
            return resultMap;
        }
        else{
            log.error("查询订单{}失败,响应数据是{}.",orderNo,response.getBody());
            return null;
        }
    }
    catch(AlipayApiException e){
        log.error("查询订单{}异常",orderNo);
        return null;
    }
}
```



取消支付

[**https://opendocs.alipay.com/open/028wob**](https://opendocs.alipay.com/open/028wob)

```java
private boolean cancelPay(String orderNo){
    //请求
    AlipayTradeCloseRequest request=new AlipayTradeCloseRequest();
    //数据
    AlipayTradeCloseModel bizModel=new AlipayTradeCloseModel();
    bizModel.setOutTradeNo(orderNo);
    request.setBizModel(bizModel);
    try{
        //完成签名并执行请求
        AlipayTradeCloseResponse response=alipayClient.execute(request);
        if(response.isSuccess()){
            log.debug("订单{}取消成功",orderNo);
        }
        else{
            log.debug("订单{}未创建,因此也可认为本次取消成功.",orderNo);
        }
        return true;
    }
    catch(AlipayApiException e){
        log.error("订单{}取消异常",orderNo);
        return false;
    }
}
```



### 退款

流程图

![stickPicture.png](file:///C:/Users/HUAWEI%20MateBook%20Xpro/AppData/Local/Packages/oice_16_974fa576_32c1d314_2a8/AC/Temp/msohtmlclip1/01/clip_image006.gif)

创建退款

[**https://opendocs.alipay.com/open/028sm9**](https://opendocs.alipay.com/open/028sm9)

```java
private HashMap<String,Object> createRefund(RefundInfo refundInfo){
    //请求
    AlipayTradeRefundRequest request=new AlipayTradeRefundRequest();
    //数据
    AlipayTradeRefundModel bizModel=new AlipayTradeRefundModel();
    //订单号
    bizModel.setOutTradeNo(refundInfo.getOrderNo());
    //退款单号
    bizModel.setOutRequestNo(refundInfo.getRefundId());
    bizModel.setRefundAmount(refundInfo.getTotalFee().toString());
    bizModel.setRefundReason(refundInfo.getReason());
    request.setBizModel(bizModel);
    HashMap<String,Object> resultMap=new HashMap<>();
    try{
        //完成签名并执行请求
        AlipayTradeRefundResponse response=alipayClient.execute(request);
        //成功则说明退款成功了
        resultMap.put("data",response.getBody());
        if(response.isSuccess()){
            resultMap.put("isRefundSuccess",true);
            log.debug("订单{}退款成功",refundInfo.getOrderNo());
        }
        else{
            resultMap.put("isRefundSuccess",false);
            log.error("订单{}退款失败",refundInfo.getOrderNo());
        }
        return resultMap;
    }
    catch(AlipayApiException e){
        resultMap.put("isRefundSuccess",false);
        log.error("订单{}退款异常",refundInfo.getOrderNo());
        return resultMap;
    }
}
```



查询退款

[**https://opendocs.alipay.com/open/028sma**](https://opendocs.alipay.com/open/028sma)

```java
private Map<String,Object> queryRefund(RefundInfo refundInfo){
    AlipayTradeFastpayRefundQueryRequest request=new AlipayTradeFastpayRefundQueryRequest();
    AlipayTradeFastpayRefundQueryModel bizModel=new AlipayTradeFastpayRefundQueryModel();
    //订单号
    bizModel.setOutTradeNo(refundInfo.getOrderNo());
    //退款单号
    bizModel.setOutRequestNo(refundInfo.getRefundNo());
    //想要额外返回的数据(也就是文档中响应可选的数据)
    ArrayList<String> extraResponseDatas=new ArrayList<>();
    extraResponseDatas.add("refund_status");
    bizModel.setQueryOptions(extraResponseDatas);
    request.setBizModel(bizModel);
    try{
           //完成签名并执行请求
        AlipayTradeFastpayRefundQueryResponse response=alipayClient.execute(request);
        if(response.isSuccess()){
            log.debug("退款{}查询成功",refundInfo.getRefundNo());
            return gson.fromJson(response.getBody(),HashMap.class);
        }
        else{
            log.debug("退款{}查询失败",refundInfo.getRefundNo());
            return null;
        }
    }
    catch(AlipayApiException e){
        log.debug("退款{}查询异常",refundInfo.getRefundNo());
        return null;
    }
}
```



### 账单

[**https://opendocs.alipay.com/open/028woc**](https://opendocs.alipay.com/open/028woc)

查询账单下载URL

```java
/**
 * @param billType 账单类型,枚举值为1.trade(交易账单)2.signcustomer(流水账).
 * @param billDate 账单日期:
 *                 日账单:格式:yyyy-MM-dd 5.6日的账单记录的时间为05-06 9:00到05-07 9:00,并且在05-07 9:00后才能查到.
 *                 月账单:格式:yyyy-MM 8月的账单记录的时间为08-03到09-03,并且在09-03后才能查到.
 * @return 账单下载url(30s后则失效)
 */
@GetMapping("/query-bill-download-url")
public String queryBillDownloadUrl(String billType,String billDate){
    //请求
    AlipayDataDataserviceBillDownloadurlQueryRequest request=new AlipayDataDataserviceBillDownloadurlQueryRequest();
    //数据
    AlipayDataDataserviceBillDownloadurlQueryModel bizModel=new AlipayDataDataserviceBillDownloadurlQueryModel();
    bizModel.setBillType(billType);
    bizModel.setBillDate(billDate);
    request.setBizModel(bizModel);
    try{
        //完成签名并执行请求
        AlipayDataDataserviceBillDownloadurlQueryResponse response=alipayClient.execute(request);
        if(response.isSuccess()){
            log.debug("获取账单下载url成功");
            return response.getBillDownloadUrl();
        }
        else{
            log.error("获取账单下载url失败");
            return null;
        }
    }
    catch(AlipayApiException e){
        log.error("获取账单下载url异常");
        return null;
    }
}
```



 

 

 

 

 

 <!-- @include: @article-footer.snippet.md -->     

 

 

 

 

 

 

 

 

 