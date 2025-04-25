package com.yufei.pictorabackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.yufei.pictorabackend.annotation.AuthCheck;
import com.yufei.pictorabackend.common.BaseResponse;
import com.yufei.pictorabackend.common.ResultUtils;
import com.yufei.pictorabackend.constant.UserConstant;
import com.yufei.pictorabackend.exception.BusinessException;
import com.yufei.pictorabackend.exception.ErrorCode;
import com.yufei.pictorabackend.manager.COSManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private COSManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        String filename = multipartFile.getOriginalFilename();
        // 构造文件存储路径，这里将文件存储在 /test 目录下，文件名保持不变
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            // 创建一个临时文件，用于存储上传的文件内容。
            // filepath 是文件路径，null 表示不指定文件后缀。
            file = File.createTempFile(filepath, null);
            // 将上传的文件内容写入临时文件。
            multipartFile.transferTo(file);
            // 将临时文件上传到云存储（如腾讯云 COS）
            cosManager.putObject(filepath, file);
            // 返回可访问的地址
            return ResultUtils.success(filepath);
        } catch (IOException e) {
            log.error("file upload error, filepath = {}, " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            // 通过 cosManager 从云存储（如腾讯云 COS）获取文件对象
            COSObject cosObject = cosManager.getObject(filepath);
            // 获取文件的输入流，用于读取文件内容
            cosObjectInput = cosObject.getObjectContent();
            // 使用 IOUtils.toByteArray 将输入流转换为字节数组。
            // IOUtils 是 Apache Commons IO 库中的工具类
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            // 设置响应内容的 MIME 类型为 application/octet-stream，表示二进制流
            response.setContentType("application/octet-stream;charset=UTF-8");
            // 设置响应头 Content-Disposition，指示浏览器将文件作为附件下载
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            // 将文件内容写入响应输出流。
            response.getOutputStream().write(bytes);
            // 刷新输出流，确保所有数据都已发送到客户端
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            // 释放流
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }
}
