package cn.cncommdata.file.service;


import cn.cncommdata.file.vo.BaseVO;
import cn.cncommdata.file.vo.FileTransferVo;
import com.github.pagehelper.PageInfo;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author: leimin
 * @DESCRIPTION
 * @create: on 2019/03/26
 **/
public interface IFileService {

    /**
     * 分页查询
     *
     * @param pageNum 当前页码
     * @param pageSize 每页展示信息条数
     * @return 分页对象
     */
    BaseVO<PageInfo<FileTransferVo>> getFileByPage(int pageNum, int pageSize);

    /**
     * 保存文件到本地
     * @param fileTransferVo 文件传输对象
     * @return 返回
     * @throws Exception 异常
     */
    BaseVO<String> uploadFileToFastDFS(FileTransferVo fileTransferVo)throws Exception;

    /**
     * 上传图片,并生成缩略图上传，
     *
     * @param fileTransferVo 文件传输对象
     * @return 返回对象
     * @throws Exception 异常
     */
    BaseVO<FileTransferVo> uploadImage(FileTransferVo fileTransferVo)throws Exception;

    /**
     * 跟文件切片上传文件
     *
     * @param fileTransferVo 文件传输对象
     * @return 返回对象
     * @throws Exception 异常
     */
    BaseVO<FileTransferVo> uploadFile(FileTransferVo fileTransferVo)throws Exception;

    /**
     * 根据id删除文件
     * @param id 查询数据的id
     * @return  BaseVO
     */
    BaseVO deleteFile(Long id);

    /**
     * 从fastdfs服务器下载文件
     *
     * @param filesName 文件名称
     * @param path 文件路径
     * @param response 相应
     * @return BaseVO
     * @throws Exception 抛出异常
     */
    BaseVO downloadFile(String filesName, String path, HttpServletResponse response) throws Exception;

    /**
     * 根据id查询
     *
     * @param id 抛出异常
     * @return 返回值
     */
    BaseVO<FileTransferVo> selectById(Long id);

    /**
     * 查询所有
     * @return 所有上传数据
     */
    BaseVO<List<FileTransferVo>> findAll();

    /**
     *  保存单条数据到数据库
     * @param fileTransferVo 文件对象
     */
    void saveFileTransferInDB(FileTransferVo fileTransferVo);
}
