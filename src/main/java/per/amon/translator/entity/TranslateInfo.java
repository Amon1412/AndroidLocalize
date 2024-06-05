package per.amon.translator.entity;

import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.Map;

public class TranslateInfo {

    private String language;
    private String locale;
    private String languageName;
    private String path;

    private ListOrderedMap<String, StringInfo> stringInfoMap = new ListOrderedMap<>();
    private ListOrderedMap<String, StringArrayInfo> stringArrayInfoMap = new ListOrderedMap<>();

    public TranslateInfo(String language, String locale) {
        this.language = language;
        this.locale = locale;
        this.languageName = language + locale;
    }

    public TranslateInfo(String language, String locale, String languageName) {
        this.language = language;
        this.locale = locale;
        this.languageName = languageName;
    }

    public StringInfo getStringInfo(String id) {
        return stringInfoMap.get(id);
    }

    public StringArrayInfo getStringArrayInfo(String id) {
        return stringArrayInfoMap.get(id);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public ListOrderedMap<String, StringInfo> getStringInfoMap() {
        return stringInfoMap;
    }

    public void addStringInfo(String name, StringInfo stringInfo) {
        stringInfoMap.put(name, stringInfo);
    }

    public ListOrderedMap<String, StringArrayInfo> getStringArrayInfoMap() {
        return stringArrayInfoMap;
    }

    public void addStringArrayInfo(String name, StringArrayInfo stringArrayInfo) {
        stringArrayInfoMap.put(name, stringArrayInfo);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
