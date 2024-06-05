package per.amon.translator;

import per.amon.translator.translate.GoogleTranslator;
import per.amon.translator.utils.ConfigUtils;
import per.amon.translator.utils.LanguageUtils;

import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-help")) {
                System.out.println("-init: 会在同级目录下生成一个配置文件，配置其中的信息后，执行-run执行即可");
                System.out.println("-lan: 列出支持的语言");
                System.out.println("-run: 开始运行进行翻译");
//                System.out.println(
//                        "    isOverwrite: 是否覆盖已有翻译\n" +
//                        "    isGenerateExcelMode: 是否只生成excel，不进行翻译与替换\n" +
//                        "    sourcePath: 源文件路径,如果配置了则会加载目录下所有res目录下的strings.xml文件并将路径设置到resPaths中，sourcePath与resPaths只用配置一个\n" +
//                        "    resPaths: 需要翻译的文件路径，可以单独配置目录进行翻译,如果配置了sourcePath则会忽略该配置\n" +
//                        "    targetPath: 翻译后的文件存放路径,如果有配置路径，则会在路径下生成有翻译后的 appName/res/values-xx 文件夹\n" +
//                        "    excelPath: 需要读取的excel文件路径,如果有配置，则会读取excel中的信息文件，并将文件并将路径设置到customExcelPaths中，excelPath与customExcelPaths只用配置一个\n" +
//                        "    customExcelPaths: 需要读取的excel文件路径，可以单独配置目录进行读取,如果配置了excelPath则会忽略该配置\n" +
//                        "    excel文件名：应以应用名_语言名.xlsx命名，如：SgtcSettings_translate_result.xlsx\n" +
//                        "    apiKey : google翻译api的key\n" +
//                        "    poxyPort: 代理端口，需要配置代理，不然谷歌翻译会被墙\n" +
//                        "\n" +
//                        "    *** 正常使用只需要配置 sourcePath/resPaths中的一个，和poxyPort即可");
            } else if (args[i].equals("-init")) {
                ConfigUtils.generateConfig();
            } else if (args[i].equals("-lan")) {
                GoogleTranslator googleTranslator = new GoogleTranslator();
                Set<String> supportedLanguages = googleTranslator.getSupportedLanguages();
                System.out.println("supportedLanguages = " + supportedLanguages);
            } else if (args[i].equals("-run")) {
                // main
                // 读取配置文件
                ConfigUtils.loadConfig();
                // 启动
                TranslationGenerator translationGenerator = new TranslationGenerator();
                translationGenerator.start();
            }
        }

    }
}
