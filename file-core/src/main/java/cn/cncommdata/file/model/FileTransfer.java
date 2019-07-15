package cn.cncommdata.file.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * @author： leimin
 * @DESCRIPTION：
 * @create： on 2019/03/26
 **/
@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class FileTransfer {


    /**
     * 主键id
     */
    private Long id;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件唯一名称
     */
    private String uniqueName;

    /**
     * 缩略图
     */
    private String thumbnail;

    /**
     * 文件保存路径
     */
    private String path;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 用于接收文件的bytes[]数组/图片base64string
     */
    private String imageString;

    /**
     * 生成缩略图的最大边长，等比缩放
     */
    private int compress;

    /**
     * 传输图片的限制大小，超出提示
     */
    private int imageMaxMemory;

    /**
     * 前端传图片宽度
     */
    private int imageWidth;

    /**
     * 前端传图片高度
     */
    private int imageHeight;

    /**
     * 文件类型,例如：jpg,mp4等。
     */
    private String fileType;

    /**
     * 是否上传成功，0：否，1：是
     */
    private int isUpload;


    public FileTransfer(Long id, String fileName, String thumbnail, String path, String fileType) {
        this.id = id;
        this.fileName = fileName;
        this.thumbnail = StringUtils.isEmpty(thumbnail) ? "" : thumbnail;
        this.path = path;
        this.fileType = fileType;
    }



}
