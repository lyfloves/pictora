package com.yufei.pictorabackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yufei.pictorabackend.config.COSClientConfig;
import com.yufei.pictorabackend.exception.BusinessException;
import com.yufei.pictorabackend.exception.ErrorCode;
import com.yufei.pictorabackend.exception.ThrowUtils;
import com.yufei.pictorabackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.protocol.HTTP;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * 文件服务
 * @deprecated 已废弃，改为使用 upload 包的模板方法优化
 */
@Slf4j
//@Service
public class FileManager {

    @Resource
    private COSClientConfig cosClientConfig;

    @Resource
    private COSManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(multipartFile);
        // 2. 图片上传地址
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int pictureWidth = imageInfo.getWidth();
            int pictureHeight = imageInfo.getHeight();
            double pictureScale = NumberUtil.round(pictureWidth * 1.0 / pictureHeight, 2).doubleValue();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(pictureWidth);
            uploadPictureResult.setPicHeight(pictureHeight);
            uploadPictureResult.setPicScale(pictureScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            // 返回可访问的地址
            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // 清理临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小
        long size = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(size > (2 * ONE_M), ErrorCode.PARAMS_ERROR, "文件不能超过 2MB");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表（或者集合）
        final List<String> ALLOW_FORMAT_LIST = List.of("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "暂不支持该文件类型");
    }

    /**
     * 清理临时文件
     *
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

    /**
     * 上传图片
     *
     * @param fileUrl          文件 url
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        // 1. 校验图片
        // validPicture(multipartFile);
        validPicture(fileUrl);
        // 2. 图片上传地址
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uuid = RandomUtil.randomString(16);
        // String originalFilename = multipartFile.getOriginalFilename();
        String originalFilename = FileUtil.mainName(fileUrl);
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            // multipartFile.transferTo(file);
            HttpUtil.downloadFile(fileUrl, file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int pictureWidth = imageInfo.getWidth();
            int pictureHeight = imageInfo.getHeight();
            double pictureScale = NumberUtil.round(pictureWidth * 1.0 / pictureHeight, 2).doubleValue();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(pictureWidth);
            uploadPictureResult.setPicHeight(pictureHeight);
            uploadPictureResult.setPicScale(pictureScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            // 返回可访问的地址
            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // 清理临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     *
     * @param fileUrl
     */
    private void validPicture(String fileUrl) {
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
}


