package per.amon.translator.utils;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import per.amon.translator.Config;
import per.amon.translator.entity.StringArrayInfo;
import per.amon.translator.entity.StringInfo;
import per.amon.translator.entity.TranslateInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static per.amon.translator.utils.CommonUtils.DEFAULT_LANGUAGE;

public class ExcelUtils {
    private final static int ROW_LANGUAGE = 0;
    private final static int ROW_LANGUAGE_EN_ROW = 1;

    private final static int COL_APP_NAME_CN = 0;
    private final static int COL_APP_NAME = 1;
    private final static int COL_STRING_NAME = 2;
    private final static int COL_STRING_VALUE_DEFAULT = 3;
    private final static int COL_STRING_VALUE_CN = 4;

    public static Map<String, TranslateInfo> readFromExcel(String excelFilePath, Map<String, TranslateInfo> languageInfos) {
        try {
            ZipSecureFile.setMinInflateRatio(0.001);
            Workbook workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(excelFilePath)));
            Sheet sheet = workbook.getSheetAt(0);

            // 读取表头，确定语言
            Row rowLanguage = sheet.getRow(ROW_LANGUAGE);
            Row rowLanguageEn = sheet.getRow(ROW_LANGUAGE_EN_ROW);
            List<String> languages = new ArrayList<>();
            for (int cellNum = COL_STRING_NAME+1; cellNum < rowLanguage.getLastCellNum(); cellNum++) {
                Cell cell = rowLanguageEn.getCell(cellNum);
                if (cell == null) {
                    continue;
                }
                String language = cell.getStringCellValue();
                languages.add(language);
                TranslateInfo translateInfo;
                // 没有则添加对应语言的翻译信息
                if (!languageInfos.containsKey(language) && !languageInfos.containsKey(language.split("-")[0])) {
                    if (language.split("-").length == 2) {
                        translateInfo = new TranslateInfo(language.split("-")[0], language.split("-")[1]);
                    } else {
                        translateInfo = new TranslateInfo(language, "");
                    }
                    languageInfos.put(language, translateInfo);
                }
            }

            // 读取每行数据
            for (int rowNum = ROW_LANGUAGE_EN_ROW+1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                String name = row.getCell(COL_STRING_NAME).getStringCellValue();
                for (int cellNum = COL_STRING_VALUE_DEFAULT; cellNum < row.getLastCellNum(); cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    if (cell == null) {
                        continue;
                    }
                    String value = cell.getStringCellValue();
                    CellStyle cellStyle = cell.getCellStyle();
                    Font font = workbook.getFontAt(cellStyle.getFontIndex());

                    // 获取字体颜色
                    short fontColor = font.getColor();
                    // 获取背景颜色
                    short backgroundColor = cellStyle.getFillForegroundColor();

                    TranslateInfo info = languageInfos.get(languages.get(cellNum - 1));
                    if (info == null) {
                        info = languageInfos.get(languages.get(cellNum - 1).split("-")[0]);
                    }
                    StringInfo stringInfo = info.getStringInfo(name);
                    ListOrderedMap<String, StringArrayInfo> stringArrayInfoMap = info.getStringArrayInfoMap();

                    // 如果是数组
                    boolean isArray = false;
                    StringArrayInfo stringArrayInfo = null;
                    if (value.contains("[")) {
                        isArray = true;
                        // name 形如 "app_name[0]"，提取出 app_name 与 0
                        String key = name.split("\\[")[0];
                        int index;
                        try {
                            index = Integer.parseInt(name.split("\\[")[1].replace("]", ""));
                        } catch (Exception e) {
                            index = 0;
                        }
                        stringArrayInfo = stringArrayInfoMap.get(key);
                        if (stringArrayInfo != null) {
                            stringInfo = stringArrayInfo.getItems()[index];
                        }
                    }
                    if (stringInfo == null) {
                        System.out.println("未找到对应的字符串或数组--语言=" + languages.get(cellNum - 1) + "--字符串=" + name);
                        continue;
                    }
                    if (fontColor == IndexedColors.BLUE.getIndex()) {
                        stringInfo.setReplacedValue(value);
                    } else if (fontColor == IndexedColors.GREEN.getIndex()) {
                        stringInfo.setTranslatedValue(value);
                    } else if (backgroundColor == IndexedColors.ORANGE.getIndex()) {
                        stringInfo.setSpecial(true);
                        if (isArray && stringArrayInfo != null) {
                            stringArrayInfo.setSpecial(true);
                        }
                    } else if (backgroundColor == IndexedColors.GREY_25_PERCENT.getIndex()) {
                        stringInfo.setTranslatable(false);
                        if (isArray && stringArrayInfo != null) {
                            stringArrayInfo.setTranslatable(false);
                        }
                    } else {
                        if (!CommonUtils.notNullOrEmpty(stringInfo.getValue())) {
                            stringInfo.setValue(value);
                        }
                    }
                }
            }

            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return languageInfos;
    }


    public static Map<String, TranslateInfo> readFromCustomExcel(String excelFilePath, Map<String, TranslateInfo> languageInfos) {
        try {
            ZipSecureFile.setMinInflateRatio(0.001);
            Workbook workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(excelFilePath)));
            Sheet sheet = workbook.getSheetAt(0);

            // 读取表头，确定语言
            Row rowLanguage = sheet.getRow(ROW_LANGUAGE);
            Row rowLanguageEn = sheet.getRow(ROW_LANGUAGE_EN_ROW);
            List<String> languages = new ArrayList<>();
            for (int cellNum = COL_STRING_VALUE_DEFAULT; cellNum < rowLanguage.getLastCellNum(); cellNum++) {
                Cell cell = rowLanguageEn.getCell(cellNum);
                if (cell == null) {
                    continue;
                }
                String language = cell.getStringCellValue();
                languages.add(language);
                TranslateInfo translateInfo;
                // 没有则添加对应语言的翻译信息
                if (!languageInfos.containsKey(language) && !languageInfos.containsKey(language.split("-")[0])) {
                    if (language.split("-").length == 2) {
                        translateInfo = new TranslateInfo(language.split("-")[0], language.split("-")[1]);
                    } else {
                        translateInfo = new TranslateInfo(language, "");
                    }
                    languageInfos.put(language, translateInfo);
                }
            }

            // 读取每行数据
            for (int rowNum = ROW_LANGUAGE_EN_ROW+1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                String name = row.getCell(COL_STRING_NAME).getStringCellValue();
                for (int cellNum = COL_STRING_VALUE_DEFAULT; cellNum < row.getLastCellNum(); cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    if (cell == null) {
                        continue;
                    }
                    String value = cell.getStringCellValue();
                    TranslateInfo info = languageInfos.get(languages.get(cellNum - 1));
                    if (info == null) {
                        info = languageInfos.get(languages.get(cellNum - 1).split("-")[0]);
                    }
                    info.getStringInfo(name).setReplacedValue(value);
                }
            }

            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return languageInfos;
    }

    public static void writeToExcel(ListOrderedMap<String, TranslateInfo> languageInfos, String appName) {
        try {
            // 在jar包同级目录下生成一个excel文件
            String jarDir = new File(ConfigUtils.externalPath).getParentFile().getPath();
            if (Config.targetPath != null && !Config.targetPath.isEmpty()) {
                jarDir = Config.targetPath;
            } else if (Config.sourcePath != null && !Config.sourcePath.isEmpty()) {
                jarDir = Config.sourcePath;
            }
            LocalDateTime now = LocalDateTime.now();
            // 定义时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMdd_HHmm_ss");
            // 格式化当前时间
            String time = now.format(formatter);
            String excelFilePath = jarDir + "/" + appName + "_translate_result_"+time+".xlsx";

            // 读取应用名 中英文
            String appNameCN = "";
            try {
                appNameCN = languageInfos.get("zh-cn").getStringInfo("app_name").getValue();
            } catch (Exception e) {
               try {
                   ListOrderedMap<String, StringInfo> stringInfoMap = languageInfos.get("zh-cn").getStringInfoMap();
                   appNameCN = stringInfoMap.getValue(0).getValue();
               } catch (Exception e1) {
                   appNameCN = appName;
               }
            }

            Workbook workbook = new XSSFWorkbook();

            Sheet sheet = workbook.createSheet("字符串翻译结果");
            AtomicInteger rowNum = new AtomicInteger();
            Row l0 = sheet.createRow(rowNum.getAndIncrement());
            Row l1 = sheet.createRow(rowNum.getAndIncrement());

            TranslateInfo defaultTranslateInfo = languageInfos.get(DEFAULT_LANGUAGE);
            ListOrderedMap<String, StringInfo> defaultTranslations;
            if (defaultTranslateInfo == null) {
                // 没有默认值就不写了
                defaultTranslateInfo = languageInfos.get(languageInfos.get(0));
                defaultTranslations = defaultTranslateInfo.getStringInfoMap();
                defaultTranslateInfo.getStringInfoMap().forEach((k, v) -> {
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    Cell cell = row.createCell(COL_STRING_NAME);
                    setCell(workbook, cell, k);
                });
            } else {
                // 写入左侧的表头和value的值
                defaultTranslations = defaultTranslateInfo.getStringInfoMap();

                Cell cell01 = l0.createCell(COL_STRING_VALUE_DEFAULT);
                setCell(workbook, cell01, LanguageUtils.getLanguageName(DEFAULT_LANGUAGE));

                Cell cell11 = l1.createCell(COL_STRING_VALUE_DEFAULT);
                setCell(workbook, cell11, DEFAULT_LANGUAGE);

                defaultTranslations.forEach((k, v) -> {
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    Cell cellx0 = row.createCell(COL_STRING_NAME);
                    setCell(workbook, cellx0, k);

                    Cell cellx1 = row.createCell(COL_STRING_VALUE_DEFAULT);
                    setCell(workbook, cellx1, v.getFinalValue());
                });
            }

            // 遍历所有语言数据，写入
            AtomicInteger colNum = new AtomicInteger(COL_STRING_VALUE_DEFAULT+1);

            String finalAppNameCN = appNameCN;
            languageInfos.forEach((language, translateInfo) -> {
                if (language.equals(DEFAULT_LANGUAGE)) {
                    return;
                }
                if (translateInfo.getStringInfoMap().isEmpty()) {
                    return;
                }

                Cell cell0x = l0.createCell(colNum.get());
                setCell(workbook, cell0x, LanguageUtils.getLanguageName(language));

                Cell cell1x = l1.createCell(colNum.get());
                setCell(workbook, cell1x, language);


                for (int i = 0; i < defaultTranslations.keyList().size(); i++) {
                    String name = defaultTranslations.get(i);
                    if (translateInfo.getStringInfoMap().containsKey(name)) {
                        Row row = sheet.getRow(i + 2);
                        Cell cell = row.createCell(colNum.get());
                        setCell(workbook, cell, translateInfo.getStringInfo(name));

                        // 写入应用名
                        Cell cell0 = row.createCell(COL_APP_NAME_CN);
                        setCell(workbook, cell0, finalAppNameCN);

                        Cell cell1 = row.createCell(COL_APP_NAME);
                        setCell(workbook, cell1, appName);
                    }
                }
                colNum.getAndIncrement();
            });


            // 写入文件
            workbook.write(Files.newOutputStream(Paths.get(excelFilePath)));
            System.out.println("翻译结果已写入：" + excelFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setCell(Workbook workbook, Cell cell, StringInfo stringInfo) {
        CellStyle style = getBaseStyle(workbook);
        cell.setCellStyle(style);
        setCell(cell, style, stringInfo);
    }

    private static void setCell(Workbook workbook, Cell cell, String string) {
        CellStyle style = getBaseStyle(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cell.setCellStyle(style);
        cell.setCellValue(string);
    }

    private static CellStyle getBaseStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // 设置字体颜色
        Font font = workbook.createFont();
        // 设置背景颜色
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // 设置边框样式
        style.setBorderTop(BorderStyle.THIN); // 上边框
        style.setBorderBottom(BorderStyle.THIN); // 下边框
        style.setBorderLeft(BorderStyle.THIN); // 左边框
        style.setBorderRight(BorderStyle.THIN); // 右边框

        // 设置边框颜色
        style.setTopBorderColor(IndexedColors.GREY_80_PERCENT.getIndex()); // 上边框颜色
        style.setBottomBorderColor(IndexedColors.GREY_80_PERCENT.getIndex()); // 下边框颜色
        style.setLeftBorderColor(IndexedColors.GREY_80_PERCENT.getIndex()); // 左边框颜色
        style.setRightBorderColor(IndexedColors.GREY_80_PERCENT.getIndex()); // 右边框颜色

        style.setFont(font);
        return style;
    }

    private static void setCell(Cell cell, CellStyle style, StringInfo stringInfo) {
        // 从style中获取font
        Font font = cell.getSheet().getWorkbook().getFontAt(style.getFontIndex());
        // 被替换的用蓝色
        if (CommonUtils.notNullOrEmpty(stringInfo.getReplacedValue())) {
            font.setColor(IndexedColors.BLUE.getIndex());
            cell.setCellValue(stringInfo.getReplacedValue());
        }
        // 翻译的用绿色
        else if (CommonUtils.notNullOrEmpty(stringInfo.getTranslatedValue())) {
            font.setColor(IndexedColors.GREEN.getIndex());
            cell.setCellValue(stringInfo.getTranslatedValue());
        }
        // 如果不翻译则为白色
        else if (CommonUtils.notNullOrEmpty(stringInfo.getValue())) {
            font.setColor(IndexedColors.BLACK.getIndex());
            cell.setCellValue(stringInfo.getValue());
        }
        // 如果有特殊字符则为橙色
        else if (stringInfo.isSpecial()) {
            style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        }
        // 如果不翻译的，则为灰色
        else if (!stringInfo.isTranslatable()) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
        // 如果都没有值则为黄色
        else if (!CommonUtils.notNullOrEmpty(stringInfo.getReplacedValue())
                && !CommonUtils.notNullOrEmpty(stringInfo.getTranslatedValue())
                && !CommonUtils.notNullOrEmpty(stringInfo.getValue())) {
            style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        }
    }

}
