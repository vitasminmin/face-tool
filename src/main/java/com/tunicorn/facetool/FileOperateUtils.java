package com.tunicorn.facetool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class FileOperateUtils {
    @Autowired
    private Constant constant;

	public Map<String, String> constructHeader() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", constant.authorization);
		return headers;
	}

	public boolean moveImg(File img, String destinationPathStr) {
		File finishedImg = new File(destinationPathStr);
		return img.renameTo(finishedImg);
	}

	public void writeImgResult(String fileName, String content) {
		FileOutputStream out = null;
		byte[] byteArr = content.getBytes();
		try {
			out = new FileOutputStream(fileName);
			out.write(byteArr);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public String imgToBase64(File file) throws IOException {
		return toBase64(readImg(file));
	}

	public byte[] readImg(File file) {
		FileInputStream in = null;
		byte[] byteArr = null;
		try {
			in = new FileInputStream(file);
			byteArr = new byte[in.available()];
			in.read(byteArr);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return byteArr;
	}

	public String toBase64(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	public void checkPath(String docPath) {
		File file = new File(docPath);
		if (!file.exists()) {
			file.mkdirs();
		}
	}
}
