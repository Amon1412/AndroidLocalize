package per.amon.translator.utils;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.w3c.dom.*;
import per.amon.translator.MyException;
import per.amon.translator.Config;
import per.amon.translator.entity.StringArrayInfo;
import per.amon.translator.entity.StringInfo;
import per.amon.translator.entity.TranslateInfo;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static per.amon.translator.utils.CommonUtils.DEFAULT_LANGUAGE;
import static per.amon.translator.utils.CommonUtils.LOAD_BY_DIR;

public class XmlUtils {
    public static Document parseXML(String filePath) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(new java.io.File(filePath));
    }

    public static ListOrderedMap<String, TranslateInfo> loadXml(String resPath) throws MyException {
        // 获取所有 values 开头的目录
        ListOrderedMap<String, TranslateInfo> languageInfos = LanguageUtils.getLanguageDirectories(resPath);
        TranslateInfo defaultTranslationInfo;
        if (languageInfos.containsKey(DEFAULT_LANGUAGE)) {
            defaultTranslationInfo = languageInfos.get(DEFAULT_LANGUAGE);
        }
        languageInfos.forEach((language, translateInfo) -> {
            loadStrings(resPath, language, translateInfo, "strings.xml");
            loadStrings(resPath, language, translateInfo, "sgtc_strings.xml");
            loadStrings(resPath, language, translateInfo, "strings_3gtc.xml");
            if (Config.isSupportTranslateArrays) {
                loadStringArrays(resPath, language, translateInfo, "strings.xml");
                loadStringArrays(resPath, language, translateInfo, "arrays.xml");
            }
        });
        if (!Config.isNoDefault) {
            defaultTranslationInfo = languageInfos.get(DEFAULT_LANGUAGE);
            if (defaultTranslationInfo == null) {
                if (resPath.contains("build")) {
                    System.out.println("该目录不支持 resPath = " + resPath);
                } else {
                    throw new MyException("初始化 string 错误。 resPath = " + resPath);
                }
            }
            // 初始化所有其他的字符串信息
            defaultTranslationInfo.getStringInfoMap().forEach((name, defaultStringInfo) -> {
                languageInfos.forEach((language, translateInfo) -> {
                    if (!language.equals(DEFAULT_LANGUAGE)) {
                        if (translateInfo.getStringInfo(name) == null) {
                            translateInfo.addStringInfo(name, new StringInfo(defaultStringInfo));
                        } else {
                            StringInfo stringInfo = translateInfo.getStringInfo(name);
                            stringInfo.setTranslatable(defaultStringInfo.isTranslatable());
                            stringInfo.setSpecial(stringInfo.isSpecial());
                        }
                    }
                });
            });
            // 初始化所有其他的字符串数组信息
            defaultTranslationInfo.getStringArrayInfoMap().forEach((name, defaultStringArrayInfo) -> {
                languageInfos.forEach((language, translateInfo) -> {
                    if (!language.equals(DEFAULT_LANGUAGE)) {
                        if (translateInfo.getStringArrayInfo(name) == null) {
                            translateInfo.addStringArrayInfo(name, new StringArrayInfo(defaultStringArrayInfo));
                        } else {
                            StringArrayInfo stringArrayInfo = translateInfo.getStringArrayInfo(name);
                            stringArrayInfo.loadFromDefault(defaultStringArrayInfo);
                        }
                    }
                });
            });
        }
        return languageInfos;
    }

    public static void writeToXml(Map<String, TranslateInfo> languageInfos, String resPath) {
        languageInfos.forEach((language, translateInfo) -> {
            String dirName;
            if (Config.isLanguageSupportLocale) {
                dirName = "values-" + translateInfo.getLanguage();
                if (translateInfo.getLocale() != null && !translateInfo.getLocale().isEmpty()) {
                    dirName += "-" + translateInfo.getLocale();
                }
            } else {
                if (language.contains("-") && Config.loadMode != LOAD_BY_DIR) {
                    String substring = language.substring(language.indexOf("-") + 1);
                    String newSubstring = "r" + substring.toUpperCase();
                    language = language.replace(substring, newSubstring);
                }
                dirName = "values-" + language;

            }
            if (language.equals(DEFAULT_LANGUAGE)) {
                dirName = "values";
                // 默认语言可能需要把中文翻译成英文，故不跳过
                // return;
            }
            Path languagePath = Paths.get(resPath, dirName);

            try {
                Path stringsPath = languagePath.resolve("strings.xml");
                if (!Files.exists(stringsPath)) {
                    createAndInit(languagePath, stringsPath);
                }
                Document stringsDoc = XmlUtils.parseXML(stringsPath.toString());
                writeStrings(stringsDoc, translateInfo, language);

                // 如果在 strings.xml 中有配置 string-array,则删除
                if (Config.isSupportTranslateArrays) {
                    Path arraysPath = languagePath.resolve("arrays.xml");
                    if (!Files.exists(arraysPath)) {
                        createAndInit(languagePath, arraysPath);
                    }
                    Document arraysDoc = XmlUtils.parseXML(arraysPath.toString());
                    writeArrays(arraysDoc, translateInfo, language);
                    deleteStringArrayFromStrings(stringsDoc);
                    // 写回 arrays.xml 文件
                    writeBackXml(arraysDoc, arraysPath);
                }

                // 写回 strings.xml 文件
                writeBackXml(stringsDoc, stringsPath);


            } catch (FileNotFoundException e) {
                // 如果 strings.xml 文件不存在,就跳过这个目录
                e.printStackTrace();
            } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
                System.out.println("language = " + language);
                e.printStackTrace();
            }
        });
    }

    public static void createAndInit(Path dirPath, Path filePath) {

        // 检查目录是否存在，如果不存在，则创建目录和文件
        if (!Files.exists(dirPath)) {
            try {
                // 创建目录
                Files.createDirectories(dirPath);
                // 创建文件
                Files.createFile(filePath);

                // 写入 <resources></resources> 到文件中
                try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                    writer.write("<resources>\n\n</resources>");
                }
            } catch (IOException e) {
                // IOException 处理
                throw new RuntimeException("Error creating language resource directory and file", e);
            }
        } else if (!Files.exists(filePath)) {
            // 如果目录存在但文件不存在，仅创建文件并写入
            try {
                Files.createFile(filePath);
                try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                    writer.write("<resources>\n\n</resources>");
                }
            } catch (IOException e) {
                throw new RuntimeException("Error creating strings.xml file", e);
            }
        }
    }

    private static void writeBackXml(Document doc, Path xmlPath) throws TransformerException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(Files.newOutputStream(xmlPath.toFile().toPath()));
        transformer.transform(source, result);
    }

    private static void removeWhitespaceNodes(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; children != null && i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                child.setTextContent(child.getTextContent().trim());
                if (child.getTextContent().isEmpty()) {
                    node.removeChild(child);
                    i--;  // Adjust the index to continue correctly after remove
                }
            } else if (child.hasChildNodes()) {
                removeWhitespaceNodes(child);
            }
        }
    }

    private static void loadStrings(String resPath, String language, TranslateInfo translateInfo, String fileName) {
        String dir;
        if (language.equals(DEFAULT_LANGUAGE)) {
            dir = "values";
        } else {
            dir = "values-" + translateInfo.getLanguage();
            if (Config.isLanguageSupportLocale && translateInfo.getLocale() != null && !translateInfo.getLocale().isEmpty()) {
                dir += "-" + translateInfo.getLocale();
            } else {
                if (language.contains("-")) {
                    String substring = language.substring(language.indexOf("-") + 1);
                    String newSubstring = "r" + substring.toUpperCase();
                    language = language.replace(substring, newSubstring);
                }
                dir = "values-" + language;
            }
        }
        Path languagePath = Paths.get(resPath, dir);
        if (Files.exists(languagePath)) {
            // 解析 strings.xml 文件
            try {
                Document doc = XmlUtils.parseXML(languagePath.resolve(fileName).toString());
                NodeList stringNodes = doc.getElementsByTagName("string");

                // 遍历 strings.xml 中的 string 节点
                for (int i = 0; i < stringNodes.getLength(); i++) {
                    Node stringNode = stringNodes.item(i);
                    if (stringNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element stringElement = (Element) stringNode;

                        NamedNodeMap attributes = stringElement.getAttributes();
                        Map<String, String> stringAttribute = new HashMap<>();
                        boolean translatable = true;
                        String name = "";
                        StringInfo stringInfo = null;
                        for (int j = 0; j < attributes.getLength(); j++) {
                            String nodeName = attributes.item(j).getNodeName();
                            if (nodeName.equals("name")) {
                                name = stringElement.getAttribute("name");
                                String value = stringElement.getTextContent();
                                // 更新翻译映射
                                stringInfo = new StringInfo(name);
                                stringInfo.setValue(value);
                            }
                            // 如果是 translatable=false 属性，则设置不翻译
                            else if (nodeName.equals("translatable")) {
                                translatable = stringElement.getAttribute("translatable").equals("true");
                            } else if (nodeName.equals("translate")) {
                                translatable = stringElement.getAttribute("translate").equals("true");
                            } else {
                                stringAttribute.put(nodeName, stringElement.getAttribute(nodeName));
                            }
                        }
                        stringInfo.setTranslatable(translatable);
                        stringInfo.setAttribute(stringAttribute);
                        translateInfo.addStringInfo(name, stringInfo);
                    }
                }
            } catch (FileNotFoundException e) {
                // 如果 strings.xml 文件不存在,就跳过这个目录
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private static void loadStringArrays(String resPath, String language, TranslateInfo translateInfo, String dirName) {
        String dir;
        if (language.equals(DEFAULT_LANGUAGE)) {
            dir = "values";
        } else {
            dir = "values-" + translateInfo.getLanguage();
            if (Config.isLanguageSupportLocale && translateInfo.getLocale() != null && !translateInfo.getLocale().isEmpty()) {
                dir += "-" + translateInfo.getLocale();
            } else {
                if (language.contains("-")) {
                    String substring = language.substring(language.indexOf("-") + 1);
                    String newSubstring = "r" + substring.toUpperCase();
                    language = language.replace(substring, newSubstring);
                }
                dir = "values-" + language;
            }
        }
        Path languagePath = Paths.get(resPath, dir);
        if (Files.exists(languagePath)) {
            // 解析 strings.xml 文件
            try {
                Document stringDoc = XmlUtils.parseXML(languagePath.resolve(dirName).toString());
                NodeList stringArrayNodes = stringDoc.getElementsByTagName("string-array");

                // 遍历 strings.xml 中的 string 节点
                for (int i = 0; i < stringArrayNodes.getLength(); i++) {
                    Node stringArrayNode = stringArrayNodes.item(i);
                    if (stringArrayNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element stringArrayElement = (Element) stringArrayNode;

                        NamedNodeMap attributes = stringArrayElement.getAttributes();
                        Map<String, String> stringAttribute = new HashMap<>();
                        boolean translatable = true;
                        String name = "";
                        StringArrayInfo stringArrayInfo = null;
                        for (int j = 0; j < attributes.getLength(); j++) {
                            String nodeName = attributes.item(j).getNodeName();
                            if (nodeName.equals("name")) {
                                name = stringArrayElement.getAttribute("name");
                                NodeList items = stringArrayElement.getElementsByTagName("item");
                                // 更新翻译映射
                                stringArrayInfo = new StringArrayInfo(name);
                                StringInfo[] stringInfos = new StringInfo[items.getLength()];
                                for (int k = 0; k < items.getLength(); k++) {
                                    StringInfo stringInfo = new StringInfo(name);
                                    stringInfo.setValue(items.item(k).getTextContent());
                                    stringInfos[k] = stringInfo;
                                }
                                stringArrayInfo.setItems(stringInfos);
                            }
                            // 如果是 translatable=false 属性，则设置不翻译
                            else if (nodeName.equals("translatable")) {
                                translatable = stringArrayElement.getAttribute("translatable").equals("true");
                            } else if (nodeName.equals("translate")) {
                                translatable = stringArrayElement.getAttribute("translate").equals("true");
                            } else {
                                stringAttribute.put(nodeName, stringArrayElement.getAttribute(nodeName));
                            }
                        }
                        stringArrayInfo.setTranslatable(translatable);
                        stringArrayInfo.setStringAttribute(stringAttribute);
                        translateInfo.addStringArrayInfo(name, stringArrayInfo);
                    }
                }
            } catch (FileNotFoundException e) {
                // 如果 strings.xml 文件不存在,就跳过这个目录
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private static void writeStrings(Document doc, TranslateInfo translateInfo, String language) {
        removeWhitespaceNodes(doc.getDocumentElement());

        NodeList stringNodes = doc.getElementsByTagName("string");

        ListOrderedMap<String, StringInfo> stringInfoMap = translateInfo.getStringInfoMap();

        Set<String> stringNames = new LinkedHashSet<>(stringInfoMap.keySet());

        // 把 stringNodes 转成 map
        Map<String, Element> stringNodeMap = new HashMap<>();
        for (int i = 0; i < stringNodes.getLength(); i++) {
            Node stringNode = stringNodes.item(i);
            if (stringNode.getNodeType() == Node.ELEMENT_NODE) {
                Element stringElement = (Element) stringNode;
                stringNodeMap.put(stringElement.getAttribute("name"), stringElement);
            }
        }

        // 遍历翻译映射
        for (String name : stringNames) {
            StringInfo stringInfo = stringInfoMap.get(name);
            Element stringElement = stringNodeMap.get(name);

            String finalValue = stringInfo.getFinalValue();

            if (stringElement == null) {
                if (!CommonUtils.notNullOrEmpty(finalValue)) {
                    continue;
                }
                // 如果 stringElement 为空,则创建一个新的 string 节点
                stringElement = doc.createElement("string");
                // 添加属性
                Map<String, String> stringAttribute = translateInfo.getStringInfo(name).getAttribute();
                Set<String> attributeSet = stringAttribute.keySet();
                for (String attribute : attributeSet) {
                    stringElement.setAttribute(attribute, stringAttribute.get(attribute));
                }
                // 添加翻译值
                stringElement.setAttribute("name", name);
                doc.getDocumentElement().appendChild(stringElement);

                stringElement.setTextContent(finalValue);
//                        System.out.println("添加节点 name= " +name + " value= " + stringInfo.getFinalValue());
            } else {
                if (!stringInfo.isTranslatable() && !language.equals(DEFAULT_LANGUAGE)) {
                    deleteNode(stringElement);
                } else {
                    if (CommonUtils.notNullOrEmpty(finalValue)) {
                        stringElement.setTextContent(finalValue);
                    }
                }

            }
            Map<String, String> stringAttribute = stringInfo.getAttribute();
            for (String attribute : stringAttribute.keySet()) {
                stringElement.setAttribute(attribute, stringAttribute.get(attribute));
            }

        }
    }

    private static void writeArrays(Document doc, TranslateInfo translateInfo, String language) {
        removeWhitespaceNodes(doc.getDocumentElement());

        NodeList stringNodes = doc.getElementsByTagName("string-array");

        ListOrderedMap<String, StringArrayInfo> stringArrayInfoMap = translateInfo.getStringArrayInfoMap();

        Set<String> stringArrayNames = new LinkedHashSet<>(stringArrayInfoMap.keySet());

        // 把 stringArrayNodes 转成 map
        Map<String, Element> stringArrayNodeMap = new HashMap<>();
        for (int i = 0; i < stringNodes.getLength(); i++) {
            Node stringArrayNode = stringNodes.item(i);
            if (stringArrayNode.getNodeType() == Node.ELEMENT_NODE) {
                Element stringArrayElement = (Element) stringArrayNode;
                stringArrayNodeMap.put(stringArrayElement.getAttribute("name"), stringArrayElement);
            }
        }

        // 遍历翻译映射
        for (String name : stringArrayNames) {
            StringArrayInfo stringArrayInfo = stringArrayInfoMap.get(name);

            Element stringArrayElement = stringArrayNodeMap.get(name);

            StringInfo[] items = stringArrayInfo.getItems();
            if (stringArrayElement == null) {
                if ((!stringArrayInfo.isTranslatable() || stringArrayInfo.isSpecial())&& !language.equals(DEFAULT_LANGUAGE)) {
                    continue;
                }
                // 如果 stringElement 为空,则创建一个新的 string 节点
                stringArrayElement = doc.createElement("string-array");
                // 添加属性
                Map<String, String> stringAttribute = stringArrayInfo.getAttribute();
                Set<String> attributeSet = stringAttribute.keySet();
                for (String attribute : attributeSet) {
                    stringArrayElement.setAttribute(attribute, stringAttribute.get(attribute));
                }
                // 添加翻译值
                stringArrayElement.setAttribute("name", name);
                doc.getDocumentElement().appendChild(stringArrayElement);

                for (StringInfo stringInfo : items) {
                    Element itemElement = doc.createElement("item");
                    itemElement.setTextContent(stringInfo.getFinalValue());
                    stringArrayElement.appendChild(itemElement);
                }
//                        System.out.println("添加节点 name= " +name + " value= " + stringInfo.getFinalValue());
            } else {
                if ((stringArrayInfo.isSpecial() || !stringArrayInfo.isTranslatable()) && !language.equals(DEFAULT_LANGUAGE)) {
                    deleteNode(stringArrayElement);
                    continue;
                }
                for (int i = 0; i < items.length; i++) {
                    StringInfo stringInfo = items[i];
                    Element itemElement = (Element) stringArrayElement.getElementsByTagName("item").item(i);
                    if (itemElement == null) {
                        try {
                            throw new MyException("itemElement is null" + " name = " +name + " index =  " + i);
                        } catch (MyException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    if (!CommonUtils.notNullOrEmpty(itemElement.getTextContent()) ||stringInfo.getReplacedValue() != null || Config.isOverwrite || language.equals(DEFAULT_LANGUAGE)) {
                            itemElement.setTextContent(stringInfo.getFinalValue());
                    }

                }
            }
            Map<String, String> stringAttribute = stringArrayInfo.getAttribute();
            for (String attribute : stringAttribute.keySet()) {
                stringArrayElement.setAttribute(attribute, stringAttribute.get(attribute));
            }
        }
    }

    private static void deleteStringArrayFromStrings(Document doc) {
        // 获取所有的 string-array 节点
        NodeList elementsByTagName = doc.getElementsByTagName("string-array");

        // 创建一个临时列表以存储需要删除的节点
        List<Node> nodesToDelete = new ArrayList<>();
        for (int i = 0; i < elementsByTagName.getLength(); i++) {
            nodesToDelete.add(elementsByTagName.item(i));
        }

        // 删除每个节点
        for (Node node : nodesToDelete) {
            deleteNode(node);
        }
    }

    private static void deleteNode(Node node) {
        Node parent = node.getParentNode();
        if (parent != null) {
            parent.removeChild(node);
        }
    }

}
