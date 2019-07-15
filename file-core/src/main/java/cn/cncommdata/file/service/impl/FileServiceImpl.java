package cn.cncommdata.file.service.impl;

import cn.cncommdata.file.assembly.FileTransferAssembly;
import cn.cncommdata.file.model.FileTransfer;
import cn.cncommdata.file.dao.FileDao;
import cn.cncommdata.file.fastDFS.FastDFSUtil;
import cn.cncommdata.file.service.IFileService;
import cn.cncommdata.file.util.DateUtils;
import cn.cncommdata.file.util.ImageUploadHelper;

import cn.cncommdata.file.vo.BaseVO;
import cn.cncommdata.file.vo.FileTransferVo;
import cn.hutool.core.io.FileUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author: leimin
 * @DESCRIPTION:
 * @create: on 2019/03/26
 **/
@Service
@Transactional
public class FileServiceImpl implements IFileService {

    /**
     * 引入dao 层接口
     */
    @Autowired
    private FileDao fileDao;
    /**
     * fastDFS storageClient
     */
    @Autowired
    private FastFileStorageClient storageClient;
    /**
     * 转化对象
     */
    @Autowired
    private FileTransferAssembly fileTransferAssembly;
    /**
     * fastDFS 工具类
     */
    @Autowired
    private FastDFSUtil fastDFSUtil;
    /**
     * 文件上传工具类
     */
    @Autowired
    private ImageUploadHelper imageUploadHelper;
    /**
     * fastDFS 分片上传Client
     */
    @Autowired
    private AppendFileStorageClient appendFileStorageClient;
    /**
     * redisTemplate
     */
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 定义分片大小
     */
    @Value("${upload.chunkSize}")
    private int chunkSize;
    /**
     * 定义tracker路径
     */
    @Value("${fdfs.file_server_url}")
    private String fileServerUrl;

    @Override
    public BaseVO<PageInfo<FileTransferVo>> getFileByPage(int pageNum, int pageSize) {

        PageHelper.startPage(pageNum, pageSize);

        PageInfo<FileTransferVo> pageInfo =
                new PageInfo<FileTransferVo>(fileTransferAssembly.toFileTransferVoList(fileDao.getFileByPageInfo()));

        return new BaseVO<PageInfo<FileTransferVo>>(BaseVO.SUCCESS_CODE, "查询成功！", pageInfo);
    }

    @Override
    public BaseVO<String> uploadFileToFastDFS(FileTransferVo fileTransferVo) throws Exception {

        //判读传输的文件是图片还是文件
        String fileType = fastDFSUtil.getFileType(fastDFSUtil.generateImage(fileTransferVo.getImageString()));
        fileTransferVo.setFileType(fileType);
        //设置当前时间戳
        fileTransferVo.setCreateTime(DateUtils.getCurrentTimeMill());

        return new BaseVO<String>(BaseVO.SUCCESS_CODE, "上传成功，返回路径", fastDFSUtil.uploadImage(fileTransferVo));
    }

    @Override
    public BaseVO<FileTransferVo> uploadImage(FileTransferVo fileTransferVo) throws Exception {

        //上传原图，上传缩略图
        fileTransferVo = imageUploadHelper.uploadImageAndThumbnail(fileTransferVo);
        if (StringUtils.isEmpty(fileTransferVo.getPath())) {
            return new BaseVO(BaseVO.OTHER_CODE, "操作失败");
        } else if (StringUtils.isEmpty(fileTransferVo.getThumbnail())) {
            return new BaseVO(BaseVO.OTHER_CODE, "缩略图上传失败", fileTransferVo);
        }
        //保存到数据库
        saveFileTransferInDB(fileTransferVo);
        //设置当前时间戳
        fileTransferVo.setCreateTime(DateUtils.getCurrentTimeMill());

        fileTransferVo.setInputStream(null);
        fileTransferVo.setImageString(null);

        return new BaseVO(BaseVO.SUCCESS_CODE, "操作成功", fileTransferVo);
    }

    /**
     * 上传完成后保存信息到数据库
     *
     * @param fileTransferVo 保存文件对象
     */
    @Override
    public void saveFileTransferInDB(FileTransferVo fileTransferVo) {

        FileTransfer fileTransfer = new FileTransfer(fileTransferVo.getId(), fileTransferVo.getFileName(),
                fileTransferVo.getThumbnail(), fileTransferVo.getPath(), fileTransferVo.getFileType());

        fileDao.save(fileTransfer);
    }

    @Override
    public BaseVO<FileTransferVo> uploadFile(FileTransferVo fileTransferVo) throws Exception {

        //1.redis记录分片信息，当前分片下标
        String uploadKey = "fileKey:" + fileTransferVo.getId();
        String currChunkKey = uploadKey + ":currChunkKey";

        try {
            //3.分片上传
            uploadChunk(fileTransferVo, uploadKey, currChunkKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //设置当前时间戳
        fileTransferVo.setCreateTime(DateUtils.getCurrentTimeMill());

        fileTransferVo.setInputStream(null);
        fileTransferVo.setImageString(null);
        fileTransferVo.setBytes(null);
        return new BaseVO(BaseVO.SUCCESS_CODE, "操作成功!", fileTransferVo);
    }

    /**
     * 上传文件的分片信息，所有分片上传完成后保存到数据库
     *
     * @param fileTransferVo 上传对象
     * @param uploadKey      上传key
     * @param currChunkKey   分片下标
     * @return 上传对象
     * @throws Exception 抛出异常
     */
    private FileTransferVo uploadChunk(FileTransferVo fileTransferVo, String uploadKey, String currChunkKey) throws Exception {

        String fdfsPathKey = uploadKey + ":fdfsPathKey";
        StorePath path = new StorePath();
        //1.循环遍历文件，开始上传，
        List<byte[]> list = fileTransferVo.getBytes();
        for (int i = 0; i < list.size(); i++) {

            if (returnActualLength(list.get(i)) == 0 && i < list.size() - 1) {
                throw new Exception("传输的数据有问题！");
            }
            //获取当前上传分片编号
            Object cNum = redisTemplate.opsForValue().get(currChunkKey);
            int chunkCurrNumber = cNum != null ? (int) cNum : 0;
            if (i == chunkCurrNumber) {

                //2.开始上传分片
                InputStream inputStream = new ByteArrayInputStream(list.get(i));
                if (i == 0) {
                    path = appendFileStorageClient.uploadAppenderFile("group1", inputStream, list.get(i).length,
                            FileUtil.extName(fileTransferVo.getFileName()));
                    if (path == null) {
                        throw new Exception("获取远程文件路径出错!");
                    }
                    redisTemplate.opsForValue().set(fdfsPathKey, path.getPath());

                } else {
                    String noGroupPath = (String) redisTemplate.opsForValue().get(fdfsPathKey);
                    if (StringUtils.isEmpty(noGroupPath)) {
                        throw new Exception("无法获取上传远程服务器文件出错！");
                    }
                    appendFileStorageClient.modifyFile("group1", noGroupPath, inputStream, list.get(i).length,
                            (long) chunkCurrNumber * chunkSize);
                }

                //3.一个分片上传完成
                if (i + 1 < list.size()) {
                    redisTemplate.opsForValue().set(currChunkKey, "" + (i + 1));
                } else if (i + 1 == list.size()) {
                    fileTransferVo.setPath(path.getGroup() + "/" + path.getPath());
                    //拼接访问ip地址
                    fileTransferVo.setPath(fileServerUrl + "/" + fileTransferVo.getPath());
                    saveFileTransferInDB(fileTransferVo);
//                    System.out.println(fileTransferVo.getPath());
                }
            }
        }
        //设置当前时间戳
        fileTransferVo.setCreateTime(DateUtils.getCurrentTimeMill());
        return fileTransferVo;
    }

    /**
     * 获取数组实际使用长度
     *
     * @param data 分片文件
     * @return 文件实际长度
     */
    private int returnActualLength(byte[] data) {

        int i = 0;
        for (; i < data.length; i++) {
            if (data[i] == '\0') {
                break;
            }
        }

        return i;
    }

    /**
     * 根据id删除数据库数据，删除fastdfs服务器文件
     *
     * @param id
     * @return
     */
    @Override
    public BaseVO deleteFile(Long id) {

        FileTransfer fileTransfer = fileDao.getFileById(id);
        if (!StringUtils.isEmpty(fileTransfer.getThumbnail())) {
            //删除缩略图
            StorePath thumbnailPath = StorePath.praseFromUrl(fileTransfer.getThumbnail());
            storageClient.deleteFile(thumbnailPath.getGroup(), thumbnailPath.getPath());
        }
        StorePath storePath = StorePath.praseFromUrl(fileTransfer.getPath());
        storageClient.deleteFile(storePath.getGroup(), storePath.getPath());

        int resultCount = fileDao.deleteById(id);
        if (resultCount == 0) {
            return new BaseVO(BaseVO.OTHER_CODE, "删除失败!");
        }
        return new BaseVO(BaseVO.SUCCESS_CODE, "删除成功!");
    }


    /**
     * 下载文件
     *
     * @param filesName 文件名称
     * @param path      文件路径
     * @param response  相应
     * @return BaseVO
     * @throws IOException
     */
    @Override
    public BaseVO downloadFile(String filesName, String path, HttpServletResponse response) throws IOException {

        StorePath storePath = StorePath.praseFromUrl(path);
        DownloadByteArray downloadByteArray = new DownloadByteArray();
        byte[] bytes = this.storageClient.downloadFile(storePath.getGroup(), storePath.getPath(), downloadByteArray);
//        System.out.println(bytes.length);
        //支持中文名称，避免乱码
        response.setContentType("application/force-download");
        response.addHeader("Content-Disposition", "attachment;fileName=" + new String(filesName.getBytes("UTF-8"), "iso-8859-1"));
        response.setCharacterEncoding("UTF-8");
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
        return new BaseVO(BaseVO.SUCCESS_CODE, "下载成功!");
    }

    /**
     * 根据id查询数据库数据
     *
     * @param id
     * @return
     */
    @Override
    public BaseVO<FileTransferVo> selectById(Long id) {
        FileTransfer f = fileDao.getFileById(id);
        if (f == null) {
            //表示数据库无匹配数据
            return new BaseVO<>(BaseVO.ILLEGAL_PARAM_CODE, "无该条记录!");
        }
        return new BaseVO<>(BaseVO.SUCCESS_CODE, "查询成功!", fileTransferAssembly.toFileTransferVo(f));
    }

    /**
     * 查询所有
     *
     * @return <List<FileTransferVo>
     */
    @Override
    public BaseVO<List<FileTransferVo>> findAll() {

        List<FileTransfer> list = fileDao.selectAll();
        if (CollectionUtils.isEmpty(list)) {
            return new BaseVO<>(BaseVO.SUCCESS_CODE, "查询成功!", fileTransferAssembly.toFileTransferVoList(list));
        } else {
            return new BaseVO<>(BaseVO.OTHER_CODE, "查询失败!");
        }
    }
}
