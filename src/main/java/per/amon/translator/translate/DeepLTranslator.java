package per.amon.translator.translate;

import com.deepl.api.*;
import per.amon.translator.Config;

import java.util.*;

public class DeepLTranslator extends per.amon.translator.translate.Translator {
    public static void main(String[] args) {
        DeepLTranslator deepLTranslator = new DeepLTranslator();
        Set<String> supportedLanguages = deepLTranslator.getSupportedLanguages();
        System.out.println(supportedLanguages);
    }

    private static final com.deepl.api.Translator translator = new com.deepl.api.Translator(Config.apiKey);;

    @Override
    public List<String> translate(List<String> words, String sourceLang, String targetLang) {
        List<String> translatedWords = new ArrayList<>();

        try {
            List<TextResult> textResults = translator.translateText(words, sourceLang, targetLang);
            for (TextResult textResult : textResults) {
                translatedWords.add(textResult.getText());
            }
            System.out.println("翻译成功： targetLang = " + targetLang);
        } catch (DeepLException | InterruptedException | IllegalArgumentException e) {
            System.out.println("翻译异常: " + targetLang + " " + e.getMessage());
        }
        return translatedWords;
    }

    @Override
    public Set<String> getSupportedLanguages() {
        if (supportedLanguages != null) {
            return supportedLanguages;
        } else {
            supportedLanguages = new HashSet<>();
        }
        try {
            List<Language> targetLanguages = translator.getTargetLanguages();
            for (Language language : targetLanguages) {
                supportedLanguages.add(language.getCode());
            }
            // 下方为官方文档上支持的语言
            supportedLanguages.add("AR");
            supportedLanguages.add("CS");
            supportedLanguages.add("DA");
            supportedLanguages.add("JA");
            supportedLanguages.add("NB");
            supportedLanguages.add("PT");
            supportedLanguages.add("RU");
        } catch (DeepLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return supportedLanguages;
    }
}

