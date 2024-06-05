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


    public static Map<String, TranslateInfo> readFromExcel(String excelFilePath, Map<String, TranslateInfo> languageInfos) {
        try {
            ZipSecureFile.setMinInflateRatio(0.001);
            Workbook workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(excelFilePath)));
            Sheet sheet = workbook.getSheetAt(0);

            // 读取表头，确定语言
            Row languageRow = sheet.getRow(0);
            Row keyRow = sheet.getRow(1);
            List<String> languages = new ArrayList<>();
            for (int cellNum = 1; cellNum < languageRow.getLastCellNum(); cellNum++) {
                Cell cell = keyRow.getCell(cellNum);
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
            for (int rowNum = 2; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                String name = row.getCell(0).getStringCellValue();
                for (int cellNum = 1; cellNum < row.getLastCellNum(); cellNum++) {
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
            Row languageRow = sheet.getRow(0);
            Row keyRow = sheet.getRow(1);
            List<String> languages = new ArrayList<>();
            for (int cellNum = 1; cellNum < languageRow.getLastCellNum(); cellNum++) {
                Cell cell = keyRow.getCell(cellNum);
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
            for (int rowNum = 2; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                String name = row.getCell(0).getStringCellValue();
                for (int cellNum = 1; cellNum < row.getLastCellNum(); cellNum++) {
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

            Workbook workbook = new XSSFWorkbook();

            Sheet sheet = workbook.createSheet("字符串翻译结果");
            AtomicInteger rowNum = new AtomicInteger();
            Row l0 = sheet.createRow(rowNum.getAndIncrement());
            Row l1 = sheet.createRow(rowNum.getAndIncrement());

            CellStyle commonStyle = workbook.createCellStyle();
            // 设置边框样式
            commonStyle.setBorderTop(BorderStyle.THIN); // 上边框
            commonStyle.setBorderBottom(BorderStyle.THIN); // 下边框
            commonStyle.setBorderLeft(BorderStyle.THIN); // 左边框
            commonStyle.setBorderRight(BorderStyle.THIN); // 右边框

            // 设置边框颜色
            commonStyle.setTopBorderColor(IndexedColors.GREY_80_PERCENT.getIndex()); // 上边框颜色
            commonStyle.setBottomBorderColor(IndexedColors.GREY_80_PERCENT.getIndex()); // 下边框颜色
            commonStyle.setLeftBorderColor(IndexedColors.GREY_80_PERCENT.getIndex()); // 左边框颜色
            commonStyle.setRightBorderColor(IndexedColors.GREY_80_PERCENT.getIndex()); // 右边框颜色

            TranslateInfo defaultTranslateInfo = languageInfos.get(DEFAULT_LANGUAGE);
            ListOrderedMap<String, StringInfo> defaultTranslations;
            if (defaultTranslateInfo == null) {
                // 没有默认值就不写了
                defaultTranslateInfo = languageInfos.get(languageInfos.get(0));
                defaultTranslations = defaultTranslateInfo.getStringInfoMap();
                defaultTranslateInfo.getStringInfoMap().forEach((k, v) -> {
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    Cell cell = row.createCell(0);
                    cell.setCellValue(k);
                    cell.setCellStyle(commonStyle);
                });
            } else {
                // 写入左侧的表头和value的值
                defaultTranslations = defaultTranslateInfo.getStringInfoMap();

                Cell cell01 = l0.createCell(1);
                cell01.setCellValue(LanguageUtils.getLanguageName(DEFAULT_LANGUAGE));
                cell01.setCellStyle(commonStyle);

                Cell cell11 = l1.createCell(1);
                cell11.setCellValue(DEFAULT_LANGUAGE);
                cell11.setCellStyle(commonStyle);

                defaultTranslations.forEach((k, v) -> {
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    Cell cellx0 = row.createCell(0);
                    cellx0.setCellValue(k);
                    cellx0.setCellStyle(commonStyle);
                    Cell cellx1 = row.createCell(1);
                    cellx1.setCellValue(v.getFinalValue());
                    cellx1.setCellStyle(commonStyle);
                });
            }

            // 遍历所有语言数据，写入
            AtomicInteger colNum = new AtomicInteger(2);

            languageInfos.forEach((language, translateInfo) -> {
                if (language.equals(DEFAULT_LANGUAGE)) {
                    return;
                }
                Cell cell0x = l0.createCell(colNum.get());
                cell0x.setCellValue(translateInfo.getLanguageName());
                cell0x.setCellStyle(commonStyle);
                Cell cell1x = l1.createCell(colNum.get());
                cell1x.setCellValue(language);
                cell1x.setCellStyle(commonStyle);

                for (int i = 0; i < defaultTranslations.keyList().size(); i++) {
                    String name = defaultTranslations.get(i);
                    if (translateInfo.getStringInfoMap().containsKey(name)) {
                        Row row = sheet.getRow(i + 2);
                        Cell cell = row.createCell(colNum.get());
                        setCell(workbook, cell, translateInfo.getStringInfo(name));
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
        CellStyle style = workbook.createCellStyle();
        // 设置字体颜色
        Font font = workbook.createFont();
        // 设置背景颜色
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

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
        cell.setCellStyle(style);
    }

}
