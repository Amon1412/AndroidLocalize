package per.amon.translator.utils;

import org.apache.commons.collections4.map.ListOrderedMap;
import per.amon.translator.Config;
import per.amon.translator.MyException;
import per.amon.translator.entity.StringArrayInfo;
import per.amon.translator.entity.StringInfo;
import per.amon.translator.entity.TranslateInfo;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static per.amon.translator.utils.CommonUtils.*;

public class LanguageUtils {
    private static Map<String, String> languageMap = new HashMap<>();

    public static Map<String, String> getLanguageMap() {
        return languageMap;
    }

    private static Pattern UNICODE_PATTERN = Pattern.compile("\\\\u[0-9A-Fa-f]{4}");
    private static Pattern RES_QUOTE_PATTERN = Pattern.compile("@[^/]+/");
    private static Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\d+\\$s|[\\p{L}\\p{N}]*)");
    private static Pattern XLIFF_PATTERN = Pattern.compile("<xliff:g(.*?)</xliff:g>");
    private static Pattern ESCAPE_PATTERN = Pattern.compile("&(#\\d+|[a-zA-Z0-9]+)");


    public static String getLanguageName(String language) {
        language = language.toLowerCase();
        String languageNoLocale = language.split("-")[0];
        if (languageMap.containsKey(language)) {
            return languageMap.get(language);
        } else if (languageMap.containsKey(languageNoLocale)) {
            return languageMap.get(languageNoLocale);
        }
        return "未知语言";
    }

    public static boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fa5') {
                return true;
            }
        }
        return false;
    }

    public static boolean containSpecial(String s) {
        Matcher matcher = RES_QUOTE_PATTERN.matcher(s);
        return matcher.find();
    }

    public static List<String> mathcUnicode(String s) {
        return match(UNICODE_PATTERN, s);
    }

    public static List<String> matchPlaceholder(String s) {
        return match(PLACEHOLDER_PATTERN, s);
    }
    public static List<String> matchXliff(String s) {
        return match(XLIFF_PATTERN, s);
    }



    public static List<String> matchEscape(String s) {
        return match(ESCAPE_PATTERN, s);
    }

    private static List<String> match(Pattern pattern, String s) {
        List<String> list = new ArrayList<>();
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }

    public static void generateTargetLanguage(Map<String, TranslateInfo> stringTranslateInfoMap) {
        TranslateInfo defaultTranslateInfo = stringTranslateInfoMap.get(DEFAULT_LANGUAGE);
        if (Config.loadMode == CommonUtils.LOAD_BY_CONFIG && !Config.isNoDefault) {
            Config.targetLanguages.forEach((language, dirName) -> {
                if (!stringTranslateInfoMap.containsKey(language)) {
                    TranslateInfo newTranslateInfo = new TranslateInfo(language, null);
                    if (language.contains("-")) {
                        String[] split = language.split("-");
                        newTranslateInfo.setLanguage(split[0]);
                        newTranslateInfo.setLocale("r"+split[1].toUpperCase());
                    }
                    newTranslateInfo.setLanguageName(LanguageUtils.getLanguageName(language));
                    // 以default为模板，创建对应的stringinfo
                    defaultTranslateInfo.getStringInfoMap().forEach((name, defaultStringInfo) -> {
                        StringInfo stringInfo = new StringInfo(defaultStringInfo);
                        newTranslateInfo.addStringInfo(name,stringInfo);
                    });
                    defaultTranslateInfo.getStringArrayInfoMap().forEach((name, defaultStringArrayInfo) -> {
                        StringArrayInfo stringArrayInfo = new StringArrayInfo(defaultStringArrayInfo);
                        newTranslateInfo.addStringArrayInfo(name,stringArrayInfo);
                    });
                    stringTranslateInfoMap.put(language, newTranslateInfo);
                }
            });
        }
    }

    public static ListOrderedMap<String, TranslateInfo> getLanguageDirectories(String resPath) throws MyException {
        ListOrderedMap<String, TranslateInfo> languageInfos = new ListOrderedMap<>();
        File dir = new File(resPath);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                String dirName = file.getName();

                if (file.isDirectory() && dirName.startsWith(VALUES)) {

                    String[] data = getLanguageData(dirName);
                    String languageNoLocale = data[1];
                    String locale = data[2];
                    String fullLanguage = languageNoLocale;
                    if (!data[2].isEmpty()) {
                        fullLanguage = languageNoLocale + "-" + data[2];
                    }
                    fullLanguage = fullLanguage.toLowerCase();

                    if (VALUES.equals(data[0])) {
                        TranslateInfo translateInfo = new TranslateInfo(languageNoLocale, locale);
                        translateInfo.setPath(file.getAbsolutePath());

                        // 对于默认语言，逻辑单独处理
                        if (dirName.equals(VALUES)) {
                            languageInfos.put(DEFAULT_LANGUAGE, translateInfo);
                            translateInfo.setLanguageName(LanguageUtils.getLanguageName(DEFAULT_LANGUAGE));
                            continue;
                        }
                        translateInfo.setLanguageName(LanguageUtils.getLanguageName(fullLanguage));

                        // 如果是在已有目录中翻译，则所有目录都添加
                        if (Config.loadMode == LOAD_BY_DIR) {
                            languageInfos.put(fullLanguage, translateInfo);
                        } else if (Config.loadMode == LOAD_BY_CONFIG && Config.targetLanguages != null) {
                            if (Config.isLanguageSupportLocale && Config.targetLanguages.containsKey(languageNoLocale)) {
                                languageInfos.put(languageNoLocale, translateInfo);
                            }
                            // 带 locale 的语言默认不处理
                            else if (Config.targetLanguages.containsKey(fullLanguage)) {
                                languageInfos.put(fullLanguage, translateInfo);
                            }
                            // 读取目录时 locale 前有 r
                            else if (fullLanguage.equals("zh-rcn") || fullLanguage.equals("zh-rtw") || fullLanguage.equals("mni-rmtei")) {
                                fullLanguage = fullLanguage.replace("-r", "-");
                                translateInfo.setLanguageName(LanguageUtils.getLanguageName(fullLanguage));
                                languageInfos.put(fullLanguage, translateInfo);
                            }
                        } else {
                            throw new MyException("loadMode配置不正确 或者 Config.targetLanguages为空. resPath = " + resPath);
                        }
                    }
                }
            }
        }

        return languageInfos;
    }

    private static String[] getLanguageData(String language) {
        String[] split = language.split("-");
        String[] data = new String[]{"", "", ""};
        if (split.length > 3) {
            return data;
        }
        for (int i = 0; i < split.length; i++) {
            data[i] = split[i];
        }
        return data;
    }

    public static String getLanguageNameByDirName(String dirName) {
        return dirName.replace("values-", "").toLowerCase();
    }

    static {
        languageMap.put(DEFAULT_LANGUAGE, "默认语言");
        languageMap.put("af", "南非荷兰语");
        languageMap.put("sq", "阿尔巴尼亚语");
        languageMap.put("am", "阿姆哈拉语");
        languageMap.put("ar", "阿拉伯语");
        languageMap.put("hy", "亚美尼亚文");
        languageMap.put("as", "阿萨姆语");
        languageMap.put("ay", "艾马拉语");
        languageMap.put("az", "阿塞拜疆语");
        languageMap.put("bm", "班巴拉语");
        languageMap.put("eu", "巴斯克语");
        languageMap.put("be", "白俄罗斯语");
        languageMap.put("bn", "孟加拉文");
        languageMap.put("bho", "博杰普尔语");
        languageMap.put("bs", "波斯尼亚语");
        languageMap.put("bg", "保加利亚语");
        languageMap.put("ca", "加泰罗尼亚语");
        languageMap.put("ceb", "宿务语");
        languageMap.put("zh-cn", "中文（简体）");
        languageMap.put("zh-tw", "中文（繁体）");
        languageMap.put("co", "科西嘉语");
        languageMap.put("hr", "克罗地亚语");
        languageMap.put("cs", "捷克语");
        languageMap.put("da", "丹麦语");
        languageMap.put("dv", "迪维希语");
        languageMap.put("doi", "多格来语");
        languageMap.put("nl", "荷兰语");
        languageMap.put("en", "英语");
        languageMap.put("eo", "世界语");
        languageMap.put("et", "爱沙尼亚语");
        languageMap.put("ee", "埃维语");
        languageMap.put("fil", "菲律宾语（塔加拉语）");
        languageMap.put("fi", "芬兰语");
        languageMap.put("fr", "法语");
        languageMap.put("fy", "弗里斯兰语");
        languageMap.put("gl", "加利西亚语");
        languageMap.put("ka", "格鲁吉亚语");
        languageMap.put("de", "德语");
        languageMap.put("el", "希腊文");
        languageMap.put("gn", "瓜拉尼人");
        languageMap.put("gu", "古吉拉特文");
        languageMap.put("ht", "海地克里奥尔语");
        languageMap.put("ha", "豪萨语");
        languageMap.put("haw", "夏威夷语");
        languageMap.put("he", "希伯来语");
        languageMap.put("hi", "印地语");
        languageMap.put("hmn", "苗语");
        languageMap.put("hu", "匈牙利语");
        languageMap.put("in", "印度尼西亚语");
        languageMap.put("is", "冰岛语");
        languageMap.put("ig", "伊博语");
        languageMap.put("ilo", "伊洛卡诺语");
        languageMap.put("id", "印度尼西亚语");
        languageMap.put("ga", "爱尔兰语");
        languageMap.put("it", "意大利语");
        languageMap.put("ja", "日语");
        languageMap.put("jv", "爪哇语");
        languageMap.put("kn", "卡纳达文");
        languageMap.put("kk", "哈萨克语");
        languageMap.put("km", "高棉语");
        languageMap.put("rw", "卢旺达语");
        languageMap.put("gom", "贡根语");
        languageMap.put("ko", "韩语");
        languageMap.put("kri", "克里奥尔语");
        languageMap.put("ku", "库尔德语");
        languageMap.put("ckb", "库尔德语（索拉尼）");
        languageMap.put("ky", "吉尔吉斯语");
        languageMap.put("lo", "老挝语");
        languageMap.put("la", "拉丁文");
        languageMap.put("lv", "拉脱维亚语");
        languageMap.put("ln", "林格拉语");
        languageMap.put("lt", "立陶宛语");
        languageMap.put("lg", "卢干达语");
        languageMap.put("lb", "卢森堡语");
        languageMap.put("mk", "马其顿语");
        languageMap.put("mai", "迈蒂利语");
        languageMap.put("mg", "马尔加什语");
        languageMap.put("ms", "马来语");
        languageMap.put("ml", "马拉雅拉姆文");
        languageMap.put("mt", "马耳他语");
        languageMap.put("mi", "毛利语");
        languageMap.put("mr", "马拉地语");
        languageMap.put("mni-mtei", "梅泰语（曼尼普尔语）");
        languageMap.put("lus", "米佐语");
        languageMap.put("mn", "蒙古文");
        languageMap.put("my", "缅甸语");
        languageMap.put("nb", "挪威语");
        languageMap.put("ne", "尼泊尔语");
        languageMap.put("no", "挪威语");
        languageMap.put("ny", "尼杨扎语（齐切瓦语）");
        languageMap.put("or", "奥里亚语（奥里亚）");
        languageMap.put("om", "奥罗莫语");
        languageMap.put("ps", "普什图语");
        languageMap.put("fa", "波斯语");
        languageMap.put("pl", "波兰语");
        languageMap.put("pt", "葡萄牙语（葡萄牙、巴西）");
        languageMap.put("pa", "旁遮普语");
        languageMap.put("qu", "克丘亚语");
        languageMap.put("ro", "罗马尼亚语");
        languageMap.put("ru", "俄语");
        languageMap.put("sm", "萨摩亚语");
        languageMap.put("sa", "梵语");
        languageMap.put("gd", "苏格兰盖尔语");
        languageMap.put("nso", "塞佩蒂语");
        languageMap.put("sr", "塞尔维亚语");
        languageMap.put("st", "塞索托语");
        languageMap.put("sn", "修纳语");
        languageMap.put("sd", "信德语");
        languageMap.put("si", "僧伽罗语");
        languageMap.put("sk", "斯洛伐克语");
        languageMap.put("sl", "斯洛文尼亚语");
        languageMap.put("so", "索马里语");
        languageMap.put("es", "西班牙语");
        languageMap.put("su", "巽他语");
        languageMap.put("sw", "斯瓦希里语");
        languageMap.put("sv", "瑞典语");
        languageMap.put("tl", "塔加路语（菲律宾语）");
        languageMap.put("tg", "塔吉克语");
        languageMap.put("ta", "泰米尔语");
        languageMap.put("tt", "鞑靼语");
        languageMap.put("te", "泰卢固语");
        languageMap.put("th", "泰语");
        languageMap.put("ti", "蒂格尼亚语");
        languageMap.put("ts", "宗加语");
        languageMap.put("tr", "土耳其语");
        languageMap.put("tk", "土库曼语");
        languageMap.put("ak", "契维语（阿坎语）");
        languageMap.put("uk", "乌克兰语");
        languageMap.put("ur", "乌尔都语");
        languageMap.put("ug", "维吾尔语");
        languageMap.put("uz", "乌兹别克语");
        languageMap.put("vi", "越南语");
        languageMap.put("cy", "威尔士语");
        languageMap.put("xh", "班图语");
        languageMap.put("yi", "意第绪语");
        languageMap.put("yo", "约鲁巴语");
        languageMap.put("zu", "祖鲁语");
    }

}
