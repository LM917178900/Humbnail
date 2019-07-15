package cn.cncommdata.file.dao;

import cn.cncommdata.file.model.FileTransfer;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: leimin
 * @description:
 * @create: on 2019/03/26
 **/
@Mapper
public interface FileDao {

    /**
     * 获取分页数据
     *
     * @return 分页数据
     */
    @Results(id = "fileTransferMap",
            value = {
                    @Result(column = "id", property = "id"),
                    @Result(column = "file_name", property = "fileName"),
                    @Result(column = "file_type", property = "fileType"),
                    @Result(column = "thumbnail", property = "thumbnail"),
                    @Result(column = "path", property = "path")
            }
    )
    @Select("select * from file order by create_time desc ")
    List<FileTransfer> getFileByPageInfo();

    /**
     * 新增文件记录
     *
     * @param fileTransfer 文件对象
     * @return 数据操作条数
     */
    @Insert("Insert into file(id,file_name,thumbnail,path,file_type,create_time)values(#{fileTransfer.id},"
            + "#{fileTransfer.fileName},#{fileTransfer.thumbnail},#{fileTransfer.path},#{fileTransfer.fileType},now())")
    int save(@Param("fileTransfer") FileTransfer fileTransfer);

    /**
     * 根据id查询对象
     *
     * @param id 文件id
     * @return 文件对象
     */
    @ResultMap("fileTransferMap")
    @Select("select * from file where id=#{id}")
    FileTransfer getFileById(@Param("id") Long id);

    /**
     * 查询所有
     *
     * @return 所有文件对象
     */
    @ResultMap("fileTransferMap")
    @Select("select * from file")
    List<FileTransfer> selectAll();

    /**
     * 根据id删除
     *
     * @param id 主键id
     * @return  int
     */
    @Delete("delete from file where id = #{id}")
    int deleteById(@Param("id") Long id);
}
