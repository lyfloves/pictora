package com.yufei.pictorabackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.yufei.pictorabackend.exception.BusinessException;
import com.yufei.pictorabackend.exception.ErrorCode;
import com.yufei.pictorabackend.exception.ThrowUtils;
import org.apache.http.protocol.HTTP;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Url 图片上传
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate{


    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 1. 校验文件非空
        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR, "文件地址为空");
        // 2. 校验文件格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
        // 3. 校验 URL 的协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "暂不支持该协议下的文件地址");

        // 4. 发送 HEAD 请求，判断文件地址的图片是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();

            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 5. 文件存在，文件类型校验
            String contentType = httpResponse.header(HTTP.CONTENT_TYPE);
            // 允许上传的文件类型
            final List<String> ALLOW_CONTENT_LIST = List.of("image/jpeg", "image/jpg", "image/png", "image/webp");
            ThrowUtils.throwIf(!ALLOW_CONTENT_LIST.contains(contentType.toLowerCase()),
                    ErrorCode.PARAMS_ERROR, "暂不支持该文件类型");

            // 6. 文件存在，文件大小校验
            String contentLengthStr = httpResponse.header(HTTP.CONTENT_LEN);
            if (CharSequenceUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > (2 * ONE_M), ErrorCode.PARAMS_ERROR, "文件不能超过 2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
                }
            }
        } finally {
            // 释放资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
