package per.amon.translator.translate;

import org.apache.commons.collections4.map.ListOrderedMap;
import per.amon.translator.MyException;
import per.amon.translator.entity.StringArrayInfo;
import per.amon.translator.entity.TranslateInfo;
import per.amon.translator.Config;
import per.amon.translator.entity.StringInfo;
import per.amon.translator.utils.CommonUtils;
import per.amon.translator.utils.LanguageUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static per.amon.translator.utils.CommonUtils.DEFAULT_LANGUAGE;

public abstract class Translator {
    private static final int MAX_BATCH_SIZE = 100;

    public abstract List<String> translate(List<String> words, String sourceLang, String targetLang);

    public abstract Set<String> getSupportedLanguages();

    Set<String> supportedLanguages = null;

    public void translate(Map<String, TranslateInfo> stringTranslateInfoMap) throws MyException {
        TranslateInfo defaultTranslateInfo = stringTranslateInfoMap.get(DEFAULT_LANGUAGE);
        if (defaultTranslateInfo == null) {
            throw new MyException("默认语言配置不存在");
        }
        if (Config.apiKey == null || Config.apiKey.isEmpty()) {
            throw new MyException("请配置翻译API Key");
        }

        AtomicInteger i = new AtomicInteger();
        int size = stringTranslateInfoMap.size();
        stringTranslateInfoMap.forEach((language, translateInfo) -> {
            if (!Config.targetLanguages.containsKey(language) && !language.equals(DEFAULT_LANGUAGE)) {
                return;
            }
            i.getAndIncrement();
            System.out.println("当前正在翻译：" + language + "  语言翻译进度：" + i + "/" + size);
            try {
                if (!language.equals(DEFAULT_LANGUAGE) && !getSupportedLanguages().contains(language)) {
                    throw new MyException("不支持当前语言：" + language);
                }
                // 第一遍先将默认语言中的中文都翻译成英文
                AtomicReference<String> sourceLanguage = new AtomicReference<>("en");
                AtomicReference<String> targetLanguage = new AtomicReference<>(LanguageUtils.getLanguageNameByDirName(language));
                ListOrderedMap<String, String> translateWords = new ListOrderedMap<>();
                ListOrderedMap<String, StringInfo> defaultStringInfoMap = defaultTranslateInfo.getStringInfoMap();
                ListOrderedMap<String, StringArrayInfo> defaultStringArrayInfoMap = defaultTranslateInfo.getStringArrayInfoMap();

                // 翻译字符串
                AtomicReference<Map<String,List<String>>> xliffSubstringMap = new AtomicReference<>(new HashMap<>());
                AtomicReference<Map<String,List<String>>> specialSubstringMap = new AtomicReference<>(new HashMap<>());
                System.out.println("翻译字符串...");
                if (language.equals(DEFAULT_LANGUAGE)) {
                    sourceLanguage.set("zh-cn");
                    targetLanguage.set("en");
                    defaultStringInfoMap.forEach((name, v) -> {
                        if (LanguageUtils.containsChinese(v.getValue())) {
                            String value = v.getValue();
                            // 把不需要翻译的字符串替换成特殊字符
                            value = replaceSubstring(xliffSubstringMap, specialSubstringMap, name, value);

                            // 把中文翻译成英文
                            translateWords.put(name, value);
                            // 把values的中文移至zh-cn下
                            StringInfo stringInfo = stringTranslateInfoMap.get("zh-cn").getStringInfo(name);
                            if (stringInfo.getFinalValue() == null) {
                                stringInfo.setValue(v.getValue());
                            }
                        }
                    });
                } else {
                    defaultStringInfoMap.forEach((name, defaultStringInfo) -> {
                        if (!defaultStringInfo.isTranslatable()) {
                            return;
                        }
                        StringInfo stringInfo = translateInfo.getStringInfo(name);
                        if (isSupportTranslate(stringInfo) && isTranslateString(name)) {
                            String value = defaultStringInfo.getValue();
                            // 把不需要翻译的字符串替换成特殊字符
                            value = replaceSubstring(xliffSubstringMap, specialSubstringMap, name, value);
                            translateWords.put(name, value);
                        }
                    });
                }

                if (this instanceof GptTranslator || isSupportedLanguage(targetLanguage.get())) {
                    translateMapValuesInPlace(translateWords, sourceLanguage.get(), targetLanguage.get());
                    translateWords.forEach((name, translateWord) -> {
                        // 翻译后进行处理
                        // 替换xliff字符串
                        Map<String, List<String>> xliffMap = xliffSubstringMap.get();
                        if (xliffMap != null && xliffMap.containsKey(name)) {
                            List<String> xliffSubstring = xliffMap.get(name);
                            for (int j = 0; j < xliffSubstring.size(); j++) {
                                translateWord = translateWord.replace("#*"+j+"*#", xliffSubstring.get(j));
                            }
                        }
                        // 替换特殊字符串
                        Map<String, List<String>> specialMap = specialSubstringMap.get();
                        if (specialMap != null && specialMap.containsKey(name)) {
                            List<String> specialSubstring = specialMap.get(name);
                            for (int j = 0; j < specialSubstring.size(); j++) {
                                translateWord = translateWord.replace("*"+j+"*", specialSubstring.get(j));
                            }
                        }
                        translateInfo.getStringInfo(name).setTranslatedValue(translateWord);
                    });
                } else {
                    try {
                        throw new MyException("不支持当前语言：" + language);
                    } catch (MyException e) {
                        e.printStackTrace();
                    }
                }

                // 翻译字符串数组

                if (Config.isSupportTranslateArrays) {
                    translateWords.clear();
                    System.out.println("翻译字符串数组...");

                    specialSubstringMap.set(new HashMap<>());
                    if (language.equals(DEFAULT_LANGUAGE)) {
                        sourceLanguage.set("zh-cn");
                        targetLanguage.set("en");
                        defaultStringArrayInfoMap.forEach((name, defaultStringArrayInfo) -> {
                            StringInfo[] items = defaultStringArrayInfo.getItems();
                            StringArrayInfo stringArrayInfo = stringTranslateInfoMap.get("zh-cn").getStringArrayInfo(name);

                            for (int j = 0; j < items.length; j++) {
                                if (LanguageUtils.containsChinese(items[j].getValue())) {
                                    String namei = name+"["+j+"]";
                                    String value = items[j].getValue();
                                    // 把不需要翻译的字符串替换成特殊字符
                                    value = replaceSubstring(xliffSubstringMap, specialSubstringMap, namei, value);

                                    // 把中文翻译成英文
                                    translateWords.put(namei, value);
                                    // 把values的中文移至zh-cn下
                                    if (stringArrayInfo != null) {
                                        StringInfo[] arrayInfoItems = stringArrayInfo.getItems();
                                        if (arrayInfoItems[j].getFinalValue() == null) {
                                            arrayInfoItems[j].setValue(value);
                                        }
                                    }
                                }

                            }

                        });
                    } else {
                        defaultStringArrayInfoMap.forEach((name, defaultStringArrayInfo) -> {
                            if (!defaultStringArrayInfo.isTranslatable()) {
                                return;
                            }
                            StringArrayInfo stringArrayInfo = translateInfo.getStringArrayInfo(name);
                            if (stringArrayInfo != null) {
                                StringInfo[] defaultItems = defaultStringArrayInfo.getItems();
                                StringInfo[] items = stringArrayInfo.getItems();
                                for (int j = 0; j < items.length; j++) {
                                    if (isSupportTranslate(items[j])) {
                                        // 把不需要翻译的字符串替换成特殊字符
                                        String namei = name+"["+j+"]";
                                        String value = defaultItems[j].getValue();
                                        // 把不需要翻译的字符串替换成特殊字符
                                        value = replaceSubstring(xliffSubstringMap, specialSubstringMap, namei, value);
                                        translateWords.put(namei, value);
                                    }
                                }
                            }

                        });
                    }

                    if (this instanceof GptTranslator || isSupportedLanguage(targetLanguage.get())) {
                        translateMapValuesInPlace(translateWords, sourceLanguage.get(), targetLanguage.get());
                        translateWords.forEach((name, translateWord) -> {
                            // 翻译后进行处理
                            // 替换xliff字符串
                            Map<String, List<String>> xliffMap = xliffSubstringMap.get();
                            if (xliffMap != null && xliffMap.containsKey(name)) {
                                List<String> xliffSubstring = xliffMap.get(name);
                                for (int j = 0; j < xliffSubstring.size(); j++) {
                                    translateWord = translateWord.replace("#*"+j+"*#", xliffSubstring.get(j));
                                }
                            }
                            // 替换特殊字符串
                            Map<String, List<String>> specialMap = specialSubstringMap.get();
                            if (specialMap != null && specialMap.containsKey(name)) {
                                List<String> specialSubstring = specialMap.get(name);
                                for (int j = 0; j < specialSubstring.size(); j++) {
                                    translateWord = translateWord.replace("*"+j+"*", specialSubstring.get(j));
                                }
                            }
                            StringArrayInfo stringArrayInfo = translateInfo.getStringArrayInfo(name.split("\\[")[0]);
                            stringArrayInfo.getItems()[Integer.parseInt(name.split("\\[")[1].split("]")[0])].setTranslatedValue(translateWord);
                        });
                    } else {
                        try {
                            throw new MyException("不支持当前语言：" + language);
                        } catch (MyException e) {
                            e.printStackTrace();
//                        System.out.println(e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
//                System.out.println(e.getMessage());
            }

        });
    }

    public boolean isSupportedLanguage(String language) {
        String lang = language.split("-")[0];
        return getSupportedLanguages().contains(language) || getSupportedLanguages().contains(lang);
    }

    public void translateMapValuesInPlace(Map<String, String> map, String sourceLang, String targetLang) {
        // 保存原始键值对的顺序
        List<String> keys = new ArrayList<>(map.keySet());

        // 创建新的 Map 保存翻译后的值
        Map<String, String> translatedValues = new HashMap<>();

        // 分批翻译值并替换原有的值
        int start = 0;
        int batchSize = MAX_BATCH_SIZE;

        System.out.println("需要翻译的数量 = " + keys.size());
        while (start < keys.size()) {
            int end = Math.min(start + batchSize, keys.size());  // 计算批次的结束索引，确保不超出列表大小
            List<String> batchKeys = keys.subList(start, end);
            List<String> batchValues = new ArrayList<>();
            for (String key : batchKeys) {
                batchValues.add(map.get(key));
            }
            int batchCount = (start + batchSize - 1) / batchSize;
            System.out.println("翻译中。。。" + "batchCount：" + batchCount + "，目标语言：" + targetLang + "，翻译数量：" + batchValues.size());
            List<String> translatedBatch = translate(batchValues, sourceLang, targetLang);

            // 逐一更新翻译结果至新的 Map 中，仅更新返回的数量
            for (int i = 0; i < translatedBatch.size() && i < batchKeys.size(); i++) {
                translatedValues.put(batchKeys.get(i), translatedBatch.get(i));
            }
            start += batchSize;  // 更新起始索引以进入下一个批次
        }
        System.out.println("目标语言：" + targetLang + "，已翻译完成");

        // 替换原有的值
        for (Map.Entry<String, String> entry : translatedValues.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
    }

    private boolean isSupportTranslate(StringInfo stringInfo) {
        if (!CommonUtils.notNullOrEmpty(stringInfo.getReplacedValue()) &&
                (!CommonUtils.notNullOrEmpty(stringInfo.getTranslatedValue()) || Config.isOverwrite)) {
            // 有特殊字符串不翻译
            if (stringInfo.isSpecial()) {
                return false;
            }

            // 已有字符串不翻译
            if (CommonUtils.notNullOrEmpty(stringInfo.getValue()) && !Config.isOverwrite) {
                return false;
            }

            return true;
        } else if (CommonUtils.notNullOrEmpty(stringInfo.getTranslatedValue()) && !Config.translateStrings.isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean isTranslateString(String name) {
        // 判断是不是配置需要翻译的字符串
        if (!Config.translateStrings.isEmpty()) {
            if (Config.translateStrings.contains(name)) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private List<String> getSpecialSubstring(String str) {
        List<String> result = new ArrayList<>();
        // 包含unicode则不翻译
        result.addAll(LanguageUtils.mathcUnicode(str));
        // 包含占位符则不翻译
        result.addAll(LanguageUtils.matchPlaceholder(str));
        return result;
    }

    private List<String> getXliffSubstring(String str) {
        List<String> result = new ArrayList<>();
        // 包含占位符则不翻译
        result.addAll(LanguageUtils.matchXliff(str));
        return result;
    }

    public String replaceSubstring(AtomicReference<Map<String,List<String>>> xliffSubstringMap, AtomicReference<Map<String,List<String>>> specialSubstringMap,String name, String value) {
        // 替换有 特殊符号 的字符串
        List<String> specialSubstring = getSpecialSubstring(value);
        for (int j = 0; j < specialSubstring.size(); j++) {
            value = value.replaceFirst(specialSubstring.get(j), "*"+j+"*");
        }
        specialSubstringMap.get().put(name, specialSubstring);

        // 替换被 xliff 包裹的字符串
        List<String> xliffSubstring = getXliffSubstring(value);
        for (int j = 0; j < xliffSubstring.size(); j++) {
            value = value.replaceFirst(xliffSubstring.get(j), "#*"+j+"*#");
        }
        xliffSubstringMap.get().put(name, xliffSubstring);
        return value;
    }
}
