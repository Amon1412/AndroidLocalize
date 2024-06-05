package per.amon.translator.translate;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import per.amon.translator.Config;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class GoogleTranslatorHelper {
    public static final String BASE_URL = "https://translation.googleapis.com/language/translate/v2/";
    private static final int RETRY_TIMES = 20;

    public static void main(String[] args) {
        String s = getSupportLaunguage();
        System.out.println("s = " + s);
//        List<String> words = new ArrayList<>();
//        words.add("Hello");
//        words.add("World");
//        String translate = translate(words, "en", "zh");
//        System.out.println("translate = " + translate);
    }

    public static String getSupportLaunguage() {
        String url = BASE_URL + "languages?key=" + Config.apiKey;
        String result = "";
        int retryTimes = RETRY_TIMES;
        while (retryTimes > 0) {
            try {
                HttpHost proxy = new HttpHost("127.0.0.1", Config.poxyPort, "http");
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .build();
                HttpClient client = HttpClientBuilder.create()
                        .setDefaultRequestConfig(config)
                        .build();
                // todo:javax.net.ssl.SSLHandshakeException: Remote host terminated the handshake
                HttpResponse response = client.execute(new HttpGet(url));

                // 获取响应实体并转换为字符串
                HttpEntity entity = response.getEntity();
                result = EntityUtils.toString(entity, "UTF-8");

                break;
            } catch (Exception e) {
                retryTimes--;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
                if (retryTimes == 0) {
                    System.out.println("获取支持的语言失败: " + e.getMessage());
                }
            }
        }

        return result;
    }

    public static List<String> translate(List<String> datas, String sourceLang, String targetLang) {
        int retryTimes = RETRY_TIMES;
        List<String> result = new ArrayList<>();
        while (retryTimes > 0) {
            try {
                // 配置代理（如果需要）
                HttpHost proxy = new HttpHost("127.0.0.1", Config.poxyPort, "http");
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .build();

                // 创建 HttpClient 实例
                HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
                HttpPost post = new HttpPost(BASE_URL);

                // 构建 multipart/form-data 请求体
                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
                        .setCharset(Charset.forName("UTF-8"))
                        .addTextBody("target", targetLang)
                        .addTextBody("format", "text")
                        .addTextBody("model", "base")
                        .addTextBody("key", Config.apiKey);
                datas.forEach(data -> multipartEntityBuilder.addTextBody("q", data, ContentType.create("text/plain", Consts.UTF_8)));
                HttpEntity data = multipartEntityBuilder.build();

                post.setEntity(data);

                // 执行 POST 请求
                // todo :java.net.SocketException: Connection reset
                HttpResponse response = client.execute(post);

                // 获取响应实体并转换为字符串
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity, "UTF-8");

                JSONObject jsonResult = JSON.parseObject(responseString);
                JSONArray translations = jsonResult.getJSONObject("data").getJSONArray("translations");

                translations.forEach(language -> {
                    JSONObject jsonObject = (JSONObject) language;
                    result.add(jsonObject.getString("translatedText"));
                });
                break;
            } catch (Exception e) {
                retryTimes--;
                if (retryTimes == 0) {
                    System.out.println("翻译成目标语言失败: "+ targetLang + "  " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
