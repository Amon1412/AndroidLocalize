package per.amon.translator;

import org.apache.commons.collections4.map.ListOrderedMap;
import per.amon.translator.translate.DeepLTranslator;
import per.amon.translator.translate.GoogleTranslator;
import per.amon.translator.translate.Translator;
import per.amon.translator.utils.*;
import per.amon.translator.entity.TranslateInfo;

import java.util.*;

import static per.amon.translator.utils.CommonUtils.DEFAULT_LANGUAGE;
import static per.amon.translator.utils.CommonUtils.ZH_CN_LANGUAGE;

public class TranslationGenerator {
    public ListOrderedMap<String, TranslateInfo> stringTranslateInfoMap;

    public void start() {
        // 加载 res 下的 string.xml 文件
        if (CommonUtils.notNullOrEmpty(Config.sourcePath)){
            System.out.println("加载 res 资源文件中...");
            FileUtils.loadResPaths();
        }
        // 从之前生成的excel中加载数据，如果有的话
        if (!CommonUtils.notNullOrEmpty(Config.excelPath)){
            Config.excelPath = Config.sourcePath;
        }
        FileUtils.loadExcelPaths();

        if (CommonUtils.notNullOrEmpty(Config.customExcelPath)){
            FileUtils.loadCustomExcelPaths();
        }

        if (!Config.translateStrings.isEmpty()) {
//            Config.isOverwrite = true;
        }

        if (Config.resPaths.isEmpty()){
            throw new RuntimeException("请配置 res 资源文件路径");
        }

        for (int i = 0; i <Config.resPaths.size() ; i++) {
            try {
                String resPath = Config.resPaths.get(i);
                System.out.println("加载res resPath = " + resPath);

                stringTranslateInfoMap = XmlUtils.loadXml(resPath);
                LanguageUtils.generateTargetLanguage(stringTranslateInfoMap);
                sortLanguage(stringTranslateInfoMap);
                if (!Config.excelPaths.isEmpty()){
                    String excelPath = Config.excelPaths.get(CommonUtils.getAppName(resPath));
                    System.out.println("加载 excel 资源文件中...  excelPath = " + excelPath);
                    ExcelUtils.readFromExcel(excelPath, stringTranslateInfoMap);
                }

                if (!Config.customExcelPaths.isEmpty()){
                    String excelPath = Config.customExcelPaths.get(CommonUtils.getAppName(resPath));
                    System.out.println("加载 customExcel 资源文件中...  excelPath = " + excelPath);
                    ExcelUtils.readFromCustomExcel(excelPath, stringTranslateInfoMap);
                }
                System.out.println("加载资源完成 -> 准备翻译中...");
                // 翻译
                Translator translator;
                switch (Config.translatorModel) {
                    case 0:
                        translator = new GoogleTranslator();
                        break;
                    case 1:
                        translator = new DeepLTranslator();
                        break;
                    default:
                        throw new RuntimeException("不支持的翻译模式");
                }
                if (Config.isTranslate) {
                    translator.translate(stringTranslateInfoMap);
                    System.out.println("翻译完成 -> 生成 Excel 文件中...");
                }

                ExcelUtils.writeToExcel(stringTranslateInfoMap, CommonUtils.getAppName(resPath));

                System.out.println("生成 Excel 文件完成");

                if (Config.targetPath != null && !Config.targetPath.isEmpty()) {
                    resPath = Config.targetPath + "\\"+ CommonUtils.getTargetResDir(resPath);
                }
                if (Config.isWriteXml) {
                    System.out.println("xml写入中。。。");
                    XmlUtils.writeToXml(stringTranslateInfoMap, resPath);
                    System.out.println("写入完成");
                }

            } catch (Exception e) {
                System.out.println("翻译失败 resPath = " + Config.resPaths.get(i));
                e.printStackTrace();
            }

        }
        System.out.println("所有任务已执行完成");

    }

    private void sortLanguage(ListOrderedMap<String, TranslateInfo> stringTranslateInfoMap) {
        // 先取出default的TranslateInfo
        TranslateInfo defaultTranslateInfo = stringTranslateInfoMap.remove(DEFAULT_LANGUAGE);
        TranslateInfo zhCNTranslateInfo = stringTranslateInfoMap.remove(ZH_CN_LANGUAGE);

        // 假设这是你从map的keySet得到的Set
        Set<String> strings = stringTranslateInfoMap.keySet();

        // 将Set转换为List
        List<String> sortedList = new ArrayList<>(strings);

        // 对List进行字母顺序排序
        Collections.sort(sortedList);

        // 创建一个临时的map
        ListOrderedMap<String, TranslateInfo> tempMap = new ListOrderedMap<>();

        // 把 default 的 TranslateInfo 放到开始
        if (defaultTranslateInfo != null) {
            tempMap.put(DEFAULT_LANGUAGE, defaultTranslateInfo);
        }
        // 把 zh-cn 的 TranslateInfo 放回
        if (zhCNTranslateInfo != null) {
            tempMap.put(ZH_CN_LANGUAGE, zhCNTranslateInfo);
        }
        // 遍历排序后的List，从原map中获取并添加到临时map中
        for (String key : sortedList) {
            tempMap.put(key, stringTranslateInfoMap.get(key));
        }
        stringTranslateInfoMap.clear();
        stringTranslateInfoMap.putAll(tempMap);

    }

}
