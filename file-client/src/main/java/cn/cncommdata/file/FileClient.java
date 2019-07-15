package cn.cncommdata.file;

import cn.cncommdata.file.feign.FileFeignClient;
import cn.cncommdata.file.vo.BaseVO;
import cn.cncommdata.file.vo.FileTransferVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author: create by leimin
 * @version: v1.0
 * @description:
 * @date:2019-05-24 11:06
 **/
@Component
public class FileClient {
    /**
     * 引入调用的Feign 接口
     */
    @Autowired
    private FileFeignClient fileFeignClient;

    /**
     * 图片或文件上传
     *
     * @param formId      formId
     * @param tenantId    企业id
     * @param fieldName   字段名称
     * @param compress    图片压缩最大边长
     * @param imageWidth  图片宽度
     * @param imageHeight 图片高度
     * @param imageString 图片base64字符串
     * @param file        非图片文件
     * @return 文件传输对象
     * @throws Exception 异常
     */
    public FileTransferVo uploadFile(Long formId, Long tenantId, String fieldName, Integer compress, Integer imageWidth,
                                     Integer imageHeight, String imageString, MultipartFile file) {

        return fileFeignClient.uploadFile(formId, tenantId, fieldName, compress, imageWidth, imageHeight, imageString, file).getData();

    }

    /**
     * 将各种类型的文件，上传到fastdfs
     *
     * @param file        非图片文件
     * @return 文件传输对象
     * @throws Exception 异常
     */
    public FileTransferVo uploadAllKindsOfFile(MultipartFile file) {
        return fileFeignClient.uploadAllKindsOfFile(null, null, null, null, null, file).getData();
    }

    /**
     * 上传模板
     *
     * @param fileString 模板base64String
     * @param fileName   文件名称
     * @return 上传结果对象
     * @throws Exception 抛出异常
     */
    public FileTransferVo upTemplate(String fileString, String fileName) {

        return fileFeignClient.upTemplate(fileString, fileName).getData();
    }

    /**
     * 删除单个文件
     *
     * @param id 主键id
     * @return BaseVO
     */
    public BaseVO delFile(Long id) {

        return fileFeignClient.delFile(id);
    }
}
