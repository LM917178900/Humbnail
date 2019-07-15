package cn.cncommdata.file.feign;

import cn.cncommdata.file.vo.BaseVO;
import cn.cncommdata.file.vo.FileTransferVo;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author: create by leimin
 * @version: v1.0
 * @description:
 * @date:2019-05-24 11:06
 **/
@FeignClient(name = "file", url = "${file.service.url}", configuration = FileFeignClient.MultipartSupportConfig.class)
public interface FileFeignClient {
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
    @PostMapping(value = "/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CrossOrigin
    BaseVO<FileTransferVo> uploadFile(@RequestParam("form_id") Long formId, @RequestHeader("tenant_id") Long tenantId,
                                      @RequestParam("field_name") String fieldName, @RequestParam("compress") Integer compress,
                                      @RequestParam("image_width") Integer imageWidth, @RequestParam("image_height") Integer imageHeight,
                                      @RequestParam(value = "image_string", required = false) String imageString,
                                      @RequestBody MultipartFile file);
    /**
     * 将各种类型的文件，上传到fastdfs
     *
     * @param formId      formId
     * @param tenantId    企业id
     * @param fieldName   字段名称
     * @param imageWidth  图片宽度
     * @param imageHeight 图片高度
     * @param file        非图片文件
     * @return 文件传输对象
     * @throws Exception 异常
     */
    @PostMapping(value = "/all/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CrossOrigin // 解决前端跨域问题
    BaseVO<FileTransferVo> uploadAllKindsOfFile(
            @RequestParam(value = "form_id", required = false) Long formId,
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "field_name", required = false) String fieldName,
            @RequestParam(value = "image_width", required = false) Integer imageWidth,
            @RequestParam(value = "image_height", required = false) Integer imageHeight,
            @RequestBody MultipartFile file
    );

    /**
     * 上传模板
     *
     * @param fileString 模板base64String
     * @param fileName   文件名称
     * @return 上传结果对象
     * @throws Exception 抛出异常
     */
    @PostMapping("/template/up")
    BaseVO<FileTransferVo> upTemplate(@RequestParam("file_string") String fileString, @RequestParam("file_name") String fileName);

    /**
     * 删除单个文件
     *
     * @param id 主键id
     * @return BaseVO
     */
    @DeleteMapping("/file/id")
    BaseVO delFile(@RequestParam(value = "id") Long id);


    /**
     * 此注释用于排除Checkstyle检查
     */
    @Configuration
    class MultipartSupportConfig {
        /**
         * 此注释用于排除Checkstyle检查
         * @return Encoder
         */
        @Bean
        public Encoder feignFormEncoder() {
            return new SpringFormEncoder();
        }
    }
}
