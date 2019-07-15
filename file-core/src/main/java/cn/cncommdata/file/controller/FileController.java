package cn.cncommdata.file.controller;

import cc.iooc.snowflake.SnowflakeClient;
import cn.cncommdata.file.fastDFS.FastDFSUtil;
import cn.cncommdata.file.service.IFileService;
import cn.cncommdata.file.util.ChunkUtil;
import cn.cncommdata.file.vo.BaseVO;
import cn.cncommdata.file.vo.FileTransferVo;
import cn.cncommdata.metadata.MetadataClient;
import cn.cncommdata.metadata.vo.field.FieldBaseVO;
import cn.cncommdata.metadata.vo.field.FileVO;
import cn.cncommdata.metadata.vo.field.ImageVO;
import cn.hutool.core.io.FileUtil;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * @author: leimin
 * @DESCRIPTION
 * @create: on 2019/03/26
 **/
@RestController
@Slf4j
public class FileController {
    /**
     * service层接口
     */
    @Autowired
    private IFileService fileService;
    /**
     * 分片工具引用
     */
    @Resource
    private ChunkUtil chunkUtil;
    /**
     * id 生成器
     */
    @Resource
    private SnowflakeClient snowflakeClient;
    /**
     * fastDFS工具类
     */
    @Resource
    private FastDFSUtil fastDFSUtil;
    /**
     * 元数据客户端
     */
    @Autowired
    private MetadataClient metadataClient;
    /**
     * 1024进制
     */
    private static final int KB_NUMBER = 1024;
    /**
     * image Path number
     */
    private static final int TWO = 2;

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
    @ApiOperation(value = "获取前端出传来的文件对象，并上传到fastdfs")
    @CrossOrigin // 解决前端跨域问题
    public BaseVO<FileTransferVo> uploadFile(
            @ApiParam(value = "表单id", required = true) @RequestParam("form_id") Long formId,
            @ApiParam(value = "企业id", required = true) @RequestHeader("tenant_id") Long tenantId,
            @ApiParam(value = "filedName", required = true) @RequestParam("field_name") String fieldName,
            @ApiParam(value = "图片压缩后最大边长") @RequestParam(value = "compress", required = false) Integer compress,
            @ApiParam(value = "图片长度") @RequestParam(value = "image_width", required = false) Integer imageWidth,
            @ApiParam(value = "图片高度") @RequestParam(value = "image_height", required = false) Integer imageHeight,
            @ApiParam(value = "图片base64字符串") @RequestParam(value = "image_string", required = false) String imageString,
            @ApiParam(value = "非图片文件") @RequestBody MultipartFile file
    ) throws Exception {

        String substring = fieldName.indexOf(".") > 0 ? fieldName.substring(0, fieldName.lastIndexOf(".")) : fieldName;

        FieldBaseVO fieldBV = metadataClient.getField(tenantId, formId, substring);

        if (fieldBV == null) {
            return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
        }
        FileTransferVo fileTransferVo = setFileTransferVo(fieldBV, compress, imageWidth, imageHeight, file);

        FileVO fileVO = (FileVO) fieldBV;
        if (!CollectionUtils.isEmpty(fileVO.getExtensions()) && !fileVO.getExtensions().contains(fileTransferVo.getFileType())) {
            return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
        }

        if ("image".equals(fileVO.getType())) {
            if (fileVO.getSize() * KB_NUMBER * KB_NUMBER < imageString.length()
                    || StringUtils.isEmpty(imageString)) {
                return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法！");
            }
            return fileService.uploadImage(fileTransferVo);

        } else {
            if (file.isEmpty()) {
                return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
            }
            return fileService.uploadFile(fileTransferVo);
        }
    }

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
    @ApiOperation(value = "将各种类型的文件，上传到fastdfs")
    @CrossOrigin // 解决前端跨域问题
    public BaseVO<FileTransferVo> uploadAllKindsOfFile(
            @ApiParam(value = "表单id") @RequestParam(value = "form_id", required = false) Long formId,
            @ApiParam(value = "企业id") @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @ApiParam(value = "filedName") @RequestParam(value = "field_name", required = false) String fieldName,
            @ApiParam(value = "图片长度") @RequestParam(value = "image_width", required = false) Integer imageWidth,
            @ApiParam(value = "图片高度") @RequestParam(value = "image_height", required = false) Integer imageHeight,
            @ApiParam(value = "文件") @RequestBody MultipartFile file
    ) throws Exception {

        if (file == null || file.isEmpty()) {
            return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
        }
        //获取文件类型
        String originalFilename = file.getOriginalFilename();
        String fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        FieldBaseVO field = null;
        if (!StringUtils.isEmpty(fieldName) && !(originalFilename.endsWith("xls") || originalFilename.endsWith("xlsx")
                || originalFilename.endsWith("zip"))) {

            field = metadataClient.getField(tenantId, formId, fieldName);
            if (field == null) {
                return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
            }
            if ("image".equals(field.getType()) && (imageHeight == null || imageWidth == null)) {
                return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
            }

            FileVO fileVO = (FileVO) field;
            if (!CollectionUtils.isEmpty(fileVO.getExtensions()) && !fileVO.getExtensions().contains(fileType)) {
                return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
            }
            if (fileVO.getSize() != null && fileVO.getSize() * KB_NUMBER * KB_NUMBER < file.getSize()) {
                return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
            }
        }

        FileTransferVo fileTransferVo = setFileTransfer(field, imageWidth, imageHeight, fileType, file);

        if (field != null && fileTransferVo.getIsImage() == 1) {
            return fileService.uploadImage(fileTransferVo);
        } else {
            return fileService.uploadFile(fileTransferVo);
        }

    }


    /**
     * 上传模板
     *
     * @param fileString 模板base64String
     * @param fileName   文件名称
     * @return 上传结果对象
     * @throws Exception 抛出异常
     */
    @PostMapping("/template/up")
    @ApiOperation(value = "上传模板")
    public BaseVO<FileTransferVo> upTemplate(
            @ApiParam(value = "非图片文件base64String", required = true) @RequestParam("file_string") String fileString,
            @ApiParam(value = "非图片文件名", required = true) @RequestParam("file_name") String fileName) throws Exception {
        if (StringUtils.isEmpty(fileString)) {
            return new BaseVO(BaseVO.ILLEGAL_PARAM_CODE, "参数不合法");
        }
        FileTransferVo fileTransferVo = new FileTransferVo();
        List<Long> longs = snowflakeClient.uniqueIds(TWO);
        fileTransferVo.setId(longs.get(0));
        fileTransferVo.setUniqueName(longs.get(1).toString());
        fileTransferVo.setFileName(fileName);
        fileTransferVo.setFileType(FileUtil.extName(fileTransferVo.getFileName()));
        fileTransferVo.setBytes(chunkUtil.toByteArray(new ByteArrayInputStream(fastDFSUtil.generateImage(fileString))));

        return fileService.uploadFile(fileTransferVo);
//        fileTransferVo.setPath("xxxx");
//        return new BaseVO<FileTransferVo>(BaseVO.SUCCESS_CODE, "test", fileTransferVo);
    }


    /**
     * 重新处理传输数据
     *
     * @param fieldBaseVO 文件对象
     * @param imageWidth  图片宽
     * @param imageHeight 图片高
     * @param type        文件类型
     * @param file        非图片文件
     * @return 封装后的文件对象
     * @throws Exception 抛出异常
     */
    private FileTransferVo setFileTransfer(FieldBaseVO fieldBaseVO, Integer imageWidth, Integer imageHeight, String type,
                                           MultipartFile file) throws Exception {

        FileTransferVo fileTransferVo = new FileTransferVo();
        List<Long> longs = snowflakeClient.uniqueIds(TWO);
        fileTransferVo.setId(longs.get(0));
        fileTransferVo.setUniqueName(longs.get(1).toString());
        fileTransferVo.setFileName(file.getOriginalFilename());
        fileTransferVo.setFileType(type);
        fileTransferVo.setBytes(chunkUtil.toByteArray(file.getInputStream()));
        //图片
        if (!(fieldBaseVO == null || !"image".equals(fieldBaseVO.getType()))) {
            fileTransferVo.setIsImage(1);
            fileTransferVo.setImageHeight(imageHeight);
            fileTransferVo.setImageWidth(imageWidth);
            fileTransferVo.setCompress(((ImageVO) fieldBaseVO).getCompress());
            fileTransferVo.setImageString(fastDFSUtil.imageToBase64(file));
            fileTransferVo.setInputStream(file.getInputStream());
        }
        return fileTransferVo;
    }

    /**
     * 重新处理传输数据
     *
     * @param fieldBaseVO 文件对象
     * @param compress    图片压缩后最大尺寸
     * @param imageWidth  图片宽
     * @param imageHeight 图片高
     * @param file        非图片文件
     * @return 封装后的文件对象
     * @throws Exception 抛出异常
     */
    private FileTransferVo setFileTransferVo(FieldBaseVO fieldBaseVO, Integer compress, Integer imageWidth, Integer imageHeight,
                                             MultipartFile file) throws Exception {

        FileTransferVo fileTransferVo = new FileTransferVo();
        List<Long> longs = snowflakeClient.uniqueIds(TWO);
        fileTransferVo.setId(longs.get(0));
        fileTransferVo.setUniqueName(longs.get(1).toString());
        fileTransferVo.setFileName(file.getOriginalFilename());
        String originalFilename = file.getOriginalFilename();
        String fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        fileTransferVo.setFileType(fileType);
        fileTransferVo.setBytes(chunkUtil.toByteArray(file.getInputStream()));
        //图片
        if (!(fieldBaseVO == null || !"image".equals(fieldBaseVO.getType()))) {
            fileTransferVo.setIsImage(1);
            fileTransferVo.setImageHeight(imageHeight);
            fileTransferVo.setImageWidth(imageWidth);
            fileTransferVo.setCompress(compress);
            fileTransferVo.setImageString(fastDFSUtil.imageToBase64(file));
            fileTransferVo.setInputStream(file.getInputStream());
        }
        return fileTransferVo;
    }

    /**
     * 文件下载  to do by zhutianpeng
     *
     * @param path      下载的路径
     * @param filesName 下载文件的文件名
     * @param response  响应对象
     * @return BaseVO
     * @throws Exception 抛出异常
     */
    @GetMapping(value = "file/download")
    @ApiOperation(value = "根据文件的路径下载文件到本地")
    public BaseVO downloadFile(@ApiParam(value = "下载文件的文件名") @RequestParam(value = "file_name") String filesName,
                               @ApiParam(value = "下载的路径") @RequestParam(value = "path") String path,
                               @ApiParam(value = "响应对象") @RequestParam(value = "response") HttpServletResponse response) throws Exception {
        return fileService.downloadFile(filesName, path, response);
    }

    /**
     * 删除单个文件 to do by zhutianpeng
     *
     * @param id 主键id
     * @return BaseVO
     */
    @DeleteMapping("/file/id")
    @ApiOperation(value = "根据id删除文件数据库的记录和fastDFS的存储文件")
    public BaseVO delFile(@ApiParam(value = "数据库id") @RequestParam(value = "id") Long id) {
        return fileService.deleteFile(id);
    }

    /**
     * 根据id查询  to do by zhutianpeng
     *
     * @param id 该文件id
     * @return FileTransferVo
     */
    @GetMapping(value = "/file/id")
    @ApiOperation(value = "根据id查询")
    public BaseVO<FileTransferVo> getFile(@ApiParam(value = "数据库id") @RequestParam(value = "id") Long id) {
        return fileService.selectById(id);
    }

    /**
     * 查询所有  to do by zhutianpeng
     *
     * @return 所有传输对象
     */
    @GetMapping(value = "/file/list")
    @ApiOperation(value = "查询所有")
    public BaseVO<List<FileTransferVo>> findAll() {
        return fileService.findAll();
    }

    /**
     * 文件记录分页查询  to do by zhutianpeng
     *
     * @param pageNum  查询页码
     * @param pageSize 每页信息数量
     * @return PageInfo<FileTransferVo> 分页对象
     */
    @GetMapping("/file/page")
    @ApiOperation(value = "文件记录分页查询")
    public BaseVO<PageInfo<FileTransferVo>> getFileByPage(
            @ApiParam(value = "查询页码") @RequestParam(value = "page_num") int pageNum,
            @ApiParam(value = "每页显示条数") @RequestParam(value = "page_size") int pageSize) {
        return fileService.getFileByPage(pageNum, pageSize);
    }

}
