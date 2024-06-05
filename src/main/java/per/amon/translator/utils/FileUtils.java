package per.amon.translator.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import per.amon.translator.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
    public static void findResPaths() throws IOException {
        // 查找 res 路径
        Path start = Paths.get(Config.sourcePath);
        // 检查是否是目录
        if (!Files.isDirectory(start)) {
            throw new IllegalArgumentException("路径必须是一个目录：" + Config.sourcePath);
        }

        // 使用 Files.walk() 来遍历目录，使用 try-with-resources 自动关闭流
        try (Stream<Path> paths = Files.walk(start)) {
            Config.resPaths = paths
                    .filter(Files::isDirectory) // 确保是目录
                    .filter(path -> path.getFileName().toString().equals("res")) // 筛选目录名为 "res"
                    .map(Path::toAbsolutePath) // 获取绝对路径
                    .map(Path::toString) // 转换为字符串
                    .collect(Collectors.toList());
        }
    }

    public static Map<String, String> findExcelPaths(String sourcePath) throws IOException {
        // 查找 excel 路径
        Path start = Paths.get(sourcePath);
        // 检查是否是目录
        if (!Files.isDirectory(start)) {
            throw new IllegalArgumentException("路径必须是一个目录：" + sourcePath);
        }

        // 使用 Files.walk() 来遍历目录，使用 try-with-resources 自动关闭流
        try (Stream<Path> paths = Files.walk(start)) {
            return paths.filter(Files::isRegularFile) // 确保是文件
                    .filter(path -> path.getFileName().toString().endsWith(".xlsx")) // 筛选文件名以 ".xlsx" 结尾
                    .collect(Collectors.toMap(
                            path -> path.getFileName().toString().split("_")[0], // key: 应用名
                            Path::toString, // value: 文件路径
                            (existing, replacement) -> {
                                // 比较现有文件和新文件的日期，保留日期较晚的
                                String existingDate = existing.split("_")[2]; // 从路径字符串中获取日期
                                String replacementDate = replacement.split("_")[2];
                                return existingDate.compareTo(replacementDate) > 0 ? existing : replacement;
                            }
                    ));
        }
    }

    public static void unzipApk(String sourceApk, String destinationFolder) {
        try {
            ZipFile zipFile = new ZipFile(sourceApk);
            zipFile.extractAll(destinationFolder);
            System.out.println("APK has been extracted successfully.");
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    public static void repackageApk(String folderPath, String destinationApk) {
        try {
            File folderToZip = new File(folderPath);
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setCompressionMethod(CompressionMethod.DEFLATE); // 设置压缩方法
            zipParameters.setCompressionLevel(CompressionLevel.NORMAL); // 设置压缩级别

            ZipFile zipFile = new ZipFile(destinationApk);
            zipFile.addFolder(folderToZip, zipParameters);  // 使用 File 对象而非 String
            System.out.println("Folder has been repackaged into APK successfully.");
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    public static void loadResPaths() {
        // 遍历 Config.sourcePath 下的所有 res 文件夹
        Config.resPaths = new ArrayList<>();
        try {
            findResPaths();
        } catch (IOException e) {
            System.out.println("加载res目录失败");
            throw new RuntimeException(e);
        }
    }

    public static void loadExcelPaths() {
        // 遍历 Config.excelPath 下的所有 excel 文件
        Config.excelPaths = new HashMap<>();
        try {
            Config.excelPaths = FileUtils.findExcelPaths(Config.excelPath);
        } catch (Exception e) {
            System.out.println("加载excel目录失败");
        }
    }
    public static void loadCustomExcelPaths() {
        // 遍历 Config.custonExcelPath 下的所有 excel 文件
        Config.customExcelPaths = new HashMap<>();
        try {
            Config.customExcelPaths = FileUtils.findExcelPaths(Config.customExcelPath);
        } catch (Exception e) {
            System.out.println("加载customExcel目录失败");
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        unzipApk("app-debug.apk", "app-debug");
    }
}
