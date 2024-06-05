package per.amon.translator.utils;

import java.util.*;
import java.util.regex.Pattern;

public class CommonUtils {
    // 语言映射
    // 加载方式
    public static final int LOAD_BY_CONFIG = 0;
    public static final int LOAD_BY_DIR = 1;
    public static final String DEFAULT_LANGUAGE = "default";
    public static final String ZH_CN_LANGUAGE = "zh-cn";
    public static final String VALUES = "values";

    public static boolean notNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }



    public static List<String> splitPath(String path) {
        String[] pathArr = path.split("\\\\");
        // 将数组转换为列表
        List<String> list = Arrays.asList(pathArr);

        // 使用 Collections.reverse() 反转列表
        Collections.reverse(list);
        if (list.size() >= 2) {
            if (list.get(1).equals("main")) {
                list = list.subList(0, 5);
            } else {
                list = list.subList(0, 2);
            }
        }

        Collections.reverse(list);
        return list;
    }

    public static String getAppName(String dirName) {
        List<String> strings = splitPath(dirName);
        // MyApplication2\app\src\main\res
        if (strings.get(1).equals("app") || strings.size() == 2) {
            return strings.get(0);
        } else {
            System.out.println("dirName = " + dirName);
            return strings.get(0)+"_"+strings.get(1);
        }

    }

    public static String getTargetResDir(String dirName) {
        return getAppName(dirName) +"\\res";
    }

    public static String replaceQuotes(String input) {
        // 这个正则表达式匹配那些不是由反斜杠转义的双引号
        return input.replaceAll("(?<!\\\\)\"", "\\\\\"");
    }

    public static String replaceSingleQuotes(String input) {
        // 这个正则表达式匹配那些不是由反斜杠转义的单引号
        return input.replaceAll("(?<!\\\\)'", "\\\\'");
    }

}
