package cn.cncommdata.file.util;

import cn.cncommdata.file.fastDFS.FastDFSUtil;
import cn.cncommdata.file.vo.FileTransferVo;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author: leimin
 * @DESCRIPTION
 * @create: on 2019/04/03
 **/
@Component
public class ImageUploadHelper {
    /**
     * fastDFS文件上传工具
     */
    @Resource
    private FastDFSUtil fastDFSUtil;
    /**
     * 上传服务器
     */
    @Value("${fdfs.file_server_url}")
    private String fileServerUrl;

    /**
     * 文件uri
     */
    private String thumbnail = "temp.";

    /**
     * 通过图片base64String ,上传图片到fastDFS，同时按比例生成缩略图，也上传fastDFS，返回他们的路径；
     *
     * @param fileTransferVo 文件传输对象
     * @return 返回值
     * @throws Exception 抛出异常
     */
    public FileTransferVo uploadImageAndThumbnail(FileTransferVo fileTransferVo) throws Exception {

        //把原图写入fastDFS;
        //inputStream 或String形式
        String originUrl = fastDFSUtil.uploadImage(fileTransferVo);
        fileTransferVo.setPath(fileServerUrl + "/" + originUrl);

        //处理缩略图
        String thumbnailUrl = handleThumbnail(fileTransferVo);
        fileTransferVo.setThumbnail(fileServerUrl + "/" + thumbnailUrl);
        return fileTransferVo;
    }

    /**
     * 处理缩略图，生成，上传，删除临时文件，返回url
     *
     * @param fileTransferVo 文件传输对象
     * @return 上传后返回路径
     * @throws Exception 抛出异常
     */
    private String handleThumbnail(FileTransferVo fileTransferVo) throws Exception {
        getThumbnail(fileTransferVo);
        fileTransferVo.setInputStream(getInputStream(fileTransferVo));
        String thumbnailUrl = fastDFSUtil.uploadImage(fileTransferVo);

        return thumbnailUrl;
    }

    /**
     * 从文件中获取输入流
     *
     * @param fileTransferVo 传输对象
     * @return 输入流
     */
    private InputStream getInputStream(FileTransferVo fileTransferVo) {
        InputStream is = null;
        try {
            File file = new File(thumbnail + fileTransferVo.getFileType());
            if (!file.exists()) {
                file.createNewFile();
            }
            is = new FileInputStream(file);
            file.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }

    /**
     * 生成缩略图，并保存到临时文件；
     *
     * @param fileTransferVo 文件对象
     * @throws Exception 抛出异常
     */
    private void getThumbnail(FileTransferVo fileTransferVo) throws Exception {

        //获取图片压缩后的尺寸
        int maxSize = fileTransferVo.getCompress();
        int imageWidth = fileTransferVo.getImageWidth();
        int imageHeight = fileTransferVo.getImageHeight();
        if (maxSize < 1 || imageWidth < 1 || imageHeight < 1) {
            throw new Exception("传输图片尺寸异常！");
        }

        //获取压缩的比率：
        double ratio = (double) maxSize / (imageHeight > imageWidth ? imageHeight : imageWidth);
        //获取压缩后的width/height;
        int width = (int) (ratio * imageWidth);
        int height = (int) (ratio * imageHeight);

        //将base64String 解析为 InputStream;
        InputStream inputStream = new ByteArrayInputStream(fastDFSUtil.generateImage(fileTransferVo.getImageString()));

        Thumbnails.of(inputStream)
                .sourceRegion(0, 0, imageWidth, imageHeight)
                .size(width, height)
                .keepAspectRatio(true)
                .toFile(thumbnail + fileTransferVo.getFileType());
        inputStream.close();
    }

}
