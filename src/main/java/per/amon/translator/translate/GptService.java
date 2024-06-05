package per.amon.translator.translate;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import per.amon.translator.Config;

import java.util.ArrayList;
import java.util.List;

public class GptService {

    private static final String GPT_API_URL = "https://api.openai-proxy.com/v1/completions";

    public static void main(String[] args) {
        List<String> words = new ArrayList<>();
        words.add("Hello");
        words.add("World");
        translate(words, "en", "zh");
    }

    public static void translate(List<String> words, String sourceLang, String targetLang) {
        // 构建翻译提示
        String jsonWords = JSON.toJSONString(words);
        System.out.println("jsonWords = " + jsonWords);

        String prompt = String.format("You are a proficient multilingual translator, I will pass in a stringlist in json format which you need to translate: ：\n" +
                "Source language: %s\n" +
                "Target language: %s\n" +
                "Translation content:%s\n" +
                "Only the translation result is provided and returned in json form", sourceLang, targetLang, jsonWords);
        System.out.println("prompt = " + prompt);

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        JSONObject json = new JSONObject();

        // 为 JSON 对象赋值
        json.put("model", "gpt-3.5-turbo-instruct-0914");
        json.put("prompt", prompt);
        json.put("max_tokens", 1000);

        // 将 JSONObject 转换成字符串
        String jsonString = json.toString();
        RequestBody body = RequestBody.create(mediaType, jsonString);

        // 构建请求
        Request request = new Request.Builder()
                .url(GPT_API_URL)
                .addHeader("Authorization", "Bearer " + Config.apiKey)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                System.out.println("responseBody = " + responseBody);
                // 对结果信息格式化提取
                JSONObject resultJson = JSON.parseObject(responseBody);
                Object choices = resultJson.get("choices");
                JSONArray array = JSONArray.of(JSON.parseObject(choices.toString()));
                Object text = ((JSONObject)array.get(0)).get("text");
                System.out.println("text = " + text);
            } else {
                System.out.println("Error: " + response.code() + " " + response.message());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
