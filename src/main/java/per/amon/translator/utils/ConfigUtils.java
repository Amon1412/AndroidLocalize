package per.amon.translator.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.collections4.map.ListOrderedMap;
import per.amon.translator.Config;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ConfigUtils {
    public static String externalPath;
    static {
        try {
            externalPath = ConfigUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadConfig() {
        String configFilePath = new File(externalPath).getParent() + "/translate_config.txt";

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configFilePath))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String configContent = stringBuilder.toString();
            JSONObject jsonObject = JSON.parseObject(configContent);

            // 处理 JSON 对象
            parseConfig(jsonObject);

            if (Config.DEBUG) {
                System.out.println("Config loaded successfully: " + jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseConfig(JSONObject jsonObject) {
        Config.loadMode = jsonObject.getIntValue("loadMode");
        Config.quoteMode = jsonObject.getIntValue("quoteMode");
        Config.quoteTransformMode = jsonObject.getIntValue("quoteTransformMode");
        Config.isOverwrite = jsonObject.getBooleanValue("isOverwrite");
        Config.isNoDefault = jsonObject.getBooleanValue("isNoDefault");
        Config.isWriteXml = jsonObject.getBooleanValue("isWriteXml");
        Config.isTranslate = jsonObject.getBooleanValue("isTranslate");
        Config.isLanguageSupportLocale = jsonObject.getBooleanValue("isLanguageSupportLocale");
        Config.isSupportTranslateArrays = jsonObject.getBooleanValue("isSupportTranslateArrays");
        Config.sourcePath = jsonObject.getString("sourcePath");
        Config.excelPath = jsonObject.getString("excelPath");

        // 判断是否存在 "resPaths" 并且不为空
        if (jsonObject.containsKey("resPaths") && !jsonObject.getJSONArray("resPaths").isEmpty()) {
            Config.resPaths = jsonObject.getJSONArray("resPaths").toJavaList(String.class);
        }
        Config.targetPath = jsonObject.getString("targetPath");
        Config.customExcelPath = jsonObject.getString("customExcelPath");
        // 判断 "excelPaths" 是否存在并且有内容
        if (jsonObject.containsKey("excelPaths") && !jsonObject.getJSONObject("excelPaths").isEmpty()) {
            Config.excelPaths = jsonObject.getJSONObject("excelPaths").toJavaObject(ListOrderedMap.class);
        }
        // 判断 "customExcelPaths" 是否存在并且有内容
        if (jsonObject.containsKey("customExcelPaths") && !jsonObject.getJSONObject("customExcelPaths").isEmpty()) {
            Config.customExcelPaths = jsonObject.getJSONObject("customExcelPaths").toJavaObject(ListOrderedMap.class);
        }

        Config.translatorModel = jsonObject.getIntValue("translatorModel");
        Config.apiKey = jsonObject.getString("apiKey");
        Config.poxyPort = jsonObject.getIntValue("proxyPort");

        // 判断 "translateStrings" 是否存在并且有内容
        if (jsonObject.containsKey("translateStrings") && !jsonObject.getJSONArray("translateStrings").isEmpty()) {
            Config.translateStrings = jsonObject.getJSONArray("translateStrings").toJavaObject(HashSet.class);
        }

        // 判断 "targetLanguages" 是否存在并且有内容
        if (jsonObject.containsKey("targetLanguages") && !jsonObject.getJSONObject("targetLanguages").isEmpty()) {
            Config.targetLanguages = jsonObject.getJSONObject("targetLanguages").toJavaObject(ListOrderedMap.class);
        }
    }


    public static void generateConfig() {
        try {
            // 获取 JAR 文件所在的目录路径
            File jarFile = new File(ConfigUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String jarDir = jarFile.getParent();

            // 内部资源文件路径和外部文件路径
            String internalFilePath = "translate_config.txt"; // 资源文件名称
            String externalFilePath = jarDir + File.separator + internalFilePath;

            // 使用类加载器获取内部资源的输入流
            InputStream inputStream = ConfigUtils.class.getClassLoader().getResourceAsStream(internalFilePath);
            if (inputStream == null) {
                System.out.println("未找到内部资源文件：" + internalFilePath);
                return;
            }

            // 创建外部文件并设置输出流
            File externalFile = new File(externalFilePath);
            FileOutputStream outputStream = new FileOutputStream(externalFile);

            // 从输入流读取数据并写入到输出流
            byte[] buffer = new byte[1024];
            int length;

            StringBuilder sb = new StringBuilder();
            while ((length = inputStream.read(buffer)) > 0) {
                sb.append(new String(buffer, 0, length));
                outputStream.write(buffer, 0, length);
            }

            // 关闭流
            outputStream.close();
            inputStream.close();

            System.out.println("配置文件内容: " + sb.toString());
            System.out.println("配置文件已生成于: " + externalFilePath);
        } catch (IOException | URISyntaxException e) {
            System.out.println("生成配置文件失败");
            e.printStackTrace();
        }
    }
}
