package per.amon.translator;

import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.*;

public class Config {
    public static final boolean DEBUG = true;
    // 加载模式，0，以配置的 targetLanguages 为基准;1，以 res 目录下已有的资源为基准； 2, 以 excel 为基准
    public static int loadMode = 0;
    // 是否覆盖已有翻译，需要对原字符串特殊处理的情况下使用，例如去除双引号
    public static int quoteMode = 0;
    public static int quoteTransformMode = 0;
    public static boolean isOverwrite = false;
    public static boolean isNoDefault = false;
    public static boolean isWriteXml = true;
    public static boolean isTranslate = true;
    public static boolean isLanguageSupportLocale = false;
    public static boolean isSupportTranslateArrays = true;
    public static String sourcePath;
    public static List<String> resPaths = new ArrayList<>();
    public static String targetPath;
    // 如果是 loadMode 为 2，需要配置 excelPath
    public static String excelPath;
    public static Map<String, String> excelPaths = new HashMap<>();
    // 已有翻译表的 excel 路径，加载作为默认对照表
    public static String customExcelPath;
    public static Map<String, String> customExcelPaths = new HashMap<>();

    // 翻译软件模式， 0,Google; 1,DeepL; 2,GPT
    public static int translatorModel = 0;
    public static String apiKey = "";
    public static int poxyPort = 33210;

    public static Set<String> translateStrings = new HashSet<>();

    public static ListOrderedMap<String, String> targetLanguages = new ListOrderedMap<>();

    //
    static {
        if (DEBUG) {


//        resPaths = new ArrayList<>();
//        resPaths.add("E:\\work\\androidFiles\\TestTranslation\\app\\src\\main\\res");
//        isGenerateExcelMode = true;
            sourcePath = "E:\\work\\translate_test";
//            sourcePath = "Z:\\A6-320-my\\packages\\apps\\DeskClock";
//            sourcePath = "Z:\\A6-240-my\\vendor\\sgtc\\apps\\SgCustomAppRes\\SgtcCommon\\WeChat";
//        sourcePath = "\\\\192.168.1.205\\shb\\asr\\ASR_320x390_test\\vendor\\sgtc\\apps\\SgCustomAppRes\\SgtcCommon\\HeartRate";
//        sourcePath = "Z:\\A6-320-my\\vendor\\sgtc\\apps\\commons_source\\SgtcSettings";
//        sourcePath = "X:\\AsrAndroid8.1_2023112121\\vendor\\sgtc\\apps\\commons_source\\SgtcSettings";

//        excelPath = "E:\\work\\translate_test";
//        targetPath = "E:\\work\\translate_test";

            translateStrings.add("prod_name");
            translateStrings.add("test1");

            targetLanguages.put("af", "af_ZA");
            targetLanguages.put("am", "am_ET");
            targetLanguages.put("ar", "ar_EG");
            /**
            targetLanguages.put("az", "az_AZ");
            targetLanguages.put("be", "be_BY");
            targetLanguages.put("bg", "bg_BG");
            targetLanguages.put("bn", "bn_BD");
            targetLanguages.put("bs", "bs_BA");
            targetLanguages.put("ca", "ca_ES");
            targetLanguages.put("cs", "cs_CZ");
            targetLanguages.put("da", "da_DK");
            targetLanguages.put("de", "de_DE");
            targetLanguages.put("el", "el_GR");
            targetLanguages.put("en", "en_US");  // 此处需注意，en有多个对应，这里只选了一个作为示例
            targetLanguages.put("es", "es_ES");
            targetLanguages.put("et", "et_EE");
            targetLanguages.put("eu", "eu_ES");
            targetLanguages.put("fa", "fa_IR");
            targetLanguages.put("fi", "fi_FI");
            targetLanguages.put("fr", "fr_FR");
            targetLanguages.put("gl", "gl_ES");
            targetLanguages.put("gu", "gu_IN");
            targetLanguages.put("hi", "hi_IN");
            targetLanguages.put("hr", "hr_HR");
            targetLanguages.put("hu", "hu_HU");
            targetLanguages.put("hy", "hy_AM");
            targetLanguages.put("in", "in_ID");
            targetLanguages.put("is", "is_IS");
            targetLanguages.put("it", "it_IT");
            targetLanguages.put("iw", "iw_IL");
            targetLanguages.put("ja", "ja_JP");
            targetLanguages.put("ka", "ka_GE");
            targetLanguages.put("kk", "kk_KZ");
            targetLanguages.put("km", "km_KH");
            targetLanguages.put("kn", "kn_IN");
            targetLanguages.put("ko", "ko_KR");
            targetLanguages.put("ky", "ky_KG");
            targetLanguages.put("lo", "lo_LA");
            targetLanguages.put("lt", "lt_LT");
            targetLanguages.put("lv", "lv_LV");
            targetLanguages.put("mk", "mk_MK");
            targetLanguages.put("ml", "ml_IN");
            targetLanguages.put("mn", "mn_MN");
            targetLanguages.put("mr", "mr_IN");
            targetLanguages.put("ms", "ms_MY");
            targetLanguages.put("my", "my_MM");
            targetLanguages.put("nb", "nb_NO");
            targetLanguages.put("ne", "ne_NP");
            targetLanguages.put("nl", "nl_NL");
            targetLanguages.put("pa", "pa_IN");
            targetLanguages.put("pl", "pl_PL");
            targetLanguages.put("pt", "pt_BR");
            targetLanguages.put("ro", "ro_RO");
            targetLanguages.put("ru", "ru_RU");
            targetLanguages.put("si", "si_LK");
            targetLanguages.put("sk", "sk_SK");
            targetLanguages.put("sl", "sl_SI");
            targetLanguages.put("sq", "sq_AL");
            targetLanguages.put("sr", "sr_Latn_RS");
            targetLanguages.put("sv", "sv_SE");
            targetLanguages.put("sw", "sw_TZ");
            targetLanguages.put("ta", "ta_IN");
            targetLanguages.put("te", "te_IN");
            targetLanguages.put("th", "th_TH");
            targetLanguages.put("tl", "tl_PH");
            targetLanguages.put("tr", "tr_TR");
            targetLanguages.put("uk", "uk_UA");
             **/
            targetLanguages.put("ur", "ur_PK");
            targetLanguages.put("uz", "uz_UZ");
            targetLanguages.put("vi", "vi_VN");
            targetLanguages.put("zh-cn", "zh_CN");
            targetLanguages.put("zh-tw", "zh_TW");
        }
    }
}
