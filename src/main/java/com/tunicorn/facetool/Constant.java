package com.tunicorn.facetool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 
 * @author vitas
 *
 */
@Component
public class Constant {

	@Value("${face.app-id}")
	public String appId;

	@Value("${face.authorization}")
	public String authorization;

	@Value("${face.detect-img-url}")
	public String detectImgURL;

	@Value("${face.detect-img-root-path}")
	public String detectImgRootPath;

	@Value("${face.compare-img-url}")
	public String compareImgURL;

	@Value("${face.compare-img-root-path}")
	public String compareImgRootPath;

	@Value("${face.second-interval}")
	public float secondInterval;

	@Value("${face.finished-path-name}")
	public String finishedPathName;
}
