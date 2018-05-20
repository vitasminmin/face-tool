package com.tunicorn.facetool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class FaceToolApplication implements CommandLineRunner {
    @Autowired
    private Constant constant;
    @Autowired
    private FileOperateUtils fileOperateUtils;


    private static Logger logger = LoggerFactory.getLogger(FaceToolApplication.class);

	public static void main(String[] args) {
	    SpringApplication.run(FaceToolApplication.class, args);
	}


    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0 || args[0] == null) {
            logger.error("no args found!");
            return;
        }

        if ("detect".equals(args[0].trim().toLowerCase())) {
            try {
                detectFace();
                logger.info("detect finished");
            } catch (Exception e) {
                logger.error("detect error:" + e.getMessage());
                e.printStackTrace();
            }
        } else if ("compare".equals(args[0].trim().toLowerCase())) {
            try {
                compareFace();
                logger.info("compare finished");
            } catch (Exception e) {
                logger.error("compare error:" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.error("invalid args! args must be 'detect' or 'compare'!");
        }

        logger.info("******* THE END *******");
    }

    private void compareFace() throws Exception {
        int successCount = 0;
        String compareUrl = constant.compareImgURL;
        String imgRootPathStr = constant.compareImgRootPath;
        String imgFinishedPathStr = imgRootPathStr + File.separator + constant.finishedPathName + File.separator;
        File imgRootPath = new File(imgRootPathStr);

        Map<String, String> headers = fileOperateUtils.constructHeader();
        Map<String, Object> paras = new HashMap<String, Object>();
        paras.put("app_id", constant.appId);
        ObjectMapper mapper = new ObjectMapper();

        if (!imgRootPath.isDirectory()) {
            throw new Exception(imgRootPath + " is not exist or is not a directory");
        }
        File[] imgFolders = imgRootPath.listFiles();
        if (imgFolders.length == 0) {
            logger.warn("no directory found in : " + imgRootPath);
            return;
        }
        List<File> folderList = Arrays.asList(imgFolders);
        fileOperateUtils.checkPath(imgFinishedPathStr);
        for (File folder : folderList) {
            if (!folder.isDirectory() || constant.finishedPathName.equals(folder.getName())) {
                continue;// 非目录或者是生成的指定目录不处理
            }
            File[] imgs = listImages(folder);
            if (imgs.length < 2) {
                logger.error("compare error:" + folder.getAbsolutePath() + ":directory less than two files");
                continue;// 目录内少于两个文件不处理
            }
            File imgA = imgs[0];
            File imgB = imgs[1];

            String imgABase64Str = fileOperateUtils.imgToBase64(imgA);
            String imgBBase64Str = fileOperateUtils.imgToBase64(imgB);
            paras.put("imageA", imgABase64Str);
            paras.put("imageB", imgBBase64Str);

            String json = mapper.writeValueAsString(paras);
            String result = post(compareUrl, headers, json);
            logger.debug("compareUrl response：" + result);
            if (!StringUtils.hasText(result)) {
                logger.error(folder.getAbsolutePath() + "compare error,url:" + compareUrl + " not response");
                continue;
            }

            JsonNode myJson = mapper.readTree(result);
            if (myJson.path("errorcode") != null && myJson.path("errorcode").asInt(-1) == 0) {
                String destinationImgStr = imgFinishedPathStr + folder.getName();
                String similarity = myJson.path("similarity").asText("0");
                String resultJsonName = removeSuffix(imgA.getName()) + "-" + removeSuffix(imgB.getName()) + "_" + similarity + ".json";
                String destinationJsonStr = destinationImgStr + File.separator + resultJsonName;
                fileOperateUtils.moveImg(folder, destinationImgStr);
                fileOperateUtils.writeImgResult(destinationJsonStr, result);
                logger.debug(folder.getAbsolutePath() + " compare success");
                successCount++;
                if (constant.secondInterval != 0) {
                    logger.info("sleep " + constant.secondInterval + " seconds");
                    Thread.sleep(((Float) (constant.secondInterval * 1000)).intValue());
                }
            } else {
                logger.error(folder.getAbsolutePath() + " compare error,detect failed,message:"
                    + myJson.path("errormsg").asText(""));
            }
        }
        logger.info("total face compare success number:" + successCount);
    }

    private void detectFace() throws Exception {
        int successCount = 0;
        String detectUrl = constant.detectImgURL;
        String imgRootPathStr = constant.detectImgRootPath;
        String imgFinishedPathStr = imgRootPathStr + File.separator + constant.finishedPathName + File.separator;
        File imgRootPath = new File(imgRootPathStr);

        Map<String, String> headers = fileOperateUtils.constructHeader();
        Map<String, Object> paras = new HashMap<String, Object>();
        paras.put("mode", 0);
        paras.put("app_id", constant.appId);
        ObjectMapper mapper = new ObjectMapper();

        if (!imgRootPath.isDirectory()) {
            throw new Exception(imgRootPath + " is not exist or is not a directory");
        }
        File[] images = listImages(imgRootPath);
        if (images.length == 0) {
            logger.warn("no file found in : " + imgRootPath);
            return;
        }
        List<File> imgs = Arrays.asList(images);
        fileOperateUtils.checkPath(imgFinishedPathStr);
        for (File img : imgs) {
            if (img.isDirectory()) {// directory will be passed
                continue;
            }
            String imgBase64Str = fileOperateUtils.imgToBase64(img);
            paras.put("image", imgBase64Str);
            String json = mapper.writeValueAsString(paras);
            String result = post(detectUrl, headers, json);
            logger.debug("detectUrl response：" + result);
            if (!StringUtils.hasText(result)) {
                logger.error(img.getAbsolutePath() + "detect error,url:" + detectUrl + " not response");
                continue;
            }
            JsonNode myJson = mapper.readTree(result);
            if (myJson.path("errorcode") != null && myJson.path("errorcode").asInt(-1) == 0) {
                String destinationImgStr = imgFinishedPathStr + img.getName();
                String destinationJsonStr = destinationImgStr + ".json";
                fileOperateUtils.writeImgResult(destinationJsonStr, result);
                fileOperateUtils.moveImg(img, destinationImgStr);
                logger.info(img.getAbsolutePath() + " detect success");
                successCount++;
                if (constant.secondInterval != 0) {
                    logger.info("sleep " + constant.secondInterval + " seconds");
                    Thread.sleep(((Float) (constant.secondInterval * 1000)).intValue());
                }
            } else {
                logger.error(img.getAbsolutePath() + "detect error,message:" + myJson.path("errormsg").asText(""));
            }
        }
        logger.info("total face detect success number:" + successCount);
    }

    public String post(String url, Map<String, String> headers, String json) throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        if (headers != null && headers.size() > 0) {
            for (String key : headers.keySet()) {
                httpPost.setHeader(key, headers.get(key));
            }
        }

        String responseBody = null;
        CloseableHttpResponse response = client.execute(httpPost);
        if (response.getStatusLine().getStatusCode() == 200) {
            // Getting the response body.
            responseBody  = EntityUtils.toString(response.getEntity());
            client.close();
            return responseBody;
        } else {
            logger.error("The HTTP status code is {}", response.getStatusLine().getStatusCode());
            client.close();
            return null;
        }
    }

    private String removeSuffix(String fileName) {
        String result = fileName;
        if (fileName.contains(".")) {
            result = fileName.substring(0, fileName.lastIndexOf("."));
        }
        return result;
    }

    private File[] listImages(File directory) {
	    String[] ext = new String[] {
	        ".png", ".ico", ".jpg", ".jpeg", ".bmp", ".gif", ".tiff"
        };
	    if (directory != null) {
            File[] files = directory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    for(String e : ext) {
                        if (name != null && name.toLowerCase().endsWith(e)) {
                            return true;
                        }
                    }

                    return false;
                }
            });

            return files;
        } else {
	        return new File[] {};
        }
    }
}

