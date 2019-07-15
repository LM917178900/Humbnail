package cn.cncommdata.file.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 文件处理工具，分片工具类
 * @Author: niulibing
 * @Create: 2019/4/3 16:27
 **/
@Component
public class ChunkUtil {

    /**
     * 分片尺寸
     */
    @Value("${upload.chunkSize}")
    private int chunkSize;



    /**
     * 文件分片后的字节数据列表
     *
     * @param filePath 文件路径
     * @return 分片数据
     * @throws Exception 抛出异常
     */
    private List<byte[]> getFileChunkList(String filePath) throws Exception {

        InputStream in = new FileInputStream(filePath);
        List<byte[]> bytesList = toByteArray(in);
        in.close();
        return bytesList;
    }

    /**
     * 具体的分片过程
     *
     * @param in 文件流
     * @return 分片数据
     * @throws IOException 抛出异常
     */
    public List<byte[]> toByteArray(InputStream in) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[chunkSize];
        ArrayList<byte[]> byteList = new ArrayList<byte[]>();
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            outputStream.write(buffer, 0, n);
            byte[] bytes = outputStream.toByteArray();
            byteList.add(bytes);
        }
        return byteList;
    }



}
