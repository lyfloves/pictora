package com.yufei.pictorabackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yufei.pictorabackend.model.dto.picture.PictureQueryRequest;
import com.yufei.pictorabackend.model.dto.picture.PictureUploadRequest;
import com.yufei.pictorabackend.model.entity.Picture;
import com.yufei.pictorabackend.model.entity.User;
import com.yufei.pictorabackend.model.vo.LoginUserVO;
import com.yufei.pictorabackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author LYF
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-04-23 14:38:43
*/
public interface PictureService extends IService<Picture> {

    /**
     *  校验图片
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
}
