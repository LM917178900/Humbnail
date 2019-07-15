package cn.cncommdata.file.fastDFS;

import cn.cncommdata.file.vo.FileTransferVo;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * @author： leimin
 * @DESCRIPTION：
 * @create： on 2019/03/27
 **/
@Component
public class FastDFSUtil {
    /**
     * 日志文件
     */
    private static Logger log = LoggerFactory.getLogger(FastDFSUtil.class);

    /**
     * 解析文件的类型
     *
     * @param bytes 文件bytes流
     * @return 文件类型
     */
    public String getFileType(byte[] bytes) {
        final int len = 4;
        byte[] fileTypeByte = new byte[len];
        System.arraycopy(bytes, 0, fileTypeByte, 0, fileTypeByte.length);
        return getTypeByStream(fileTypeByte);
    }

    /**
     * 解析图片的类型
     *
     * @param bytes 文件bytes流
     * @return 文件类型
     */
    public String getImageType(byte[] bytes) {
        final int len = 4;
        byte[] fileTypeByte = new byte[len];
        System.arraycopy(bytes, 0, fileTypeByte, 0, fileTypeByte.length);
        return getImageTypeByStream(fileTypeByte);
    }

    /**
     * 获取图片的类型
     *
     * @param fileTypeByte 文件bytes流
     * @return 文件类型
     */
    private static String getTypeByStream(byte[] fileTypeByte) {

        String type = bytesToHexString(fileTypeByte).toUpperCase();
        if (type.contains("FFD8FF")) {
            return "jpg";
        } else if (type.contains("89504E47")) {
            return "png";
        } else if (type.contains("47494638")) {
            return "gif";
        } else if (type.contains("49492A00")) {
            return "tif";
        } else if (type.contains("424D")) { //后面的不是图片
            return "bmp";
        } else if (type.contains("41433130")) {
            return "dwg";
        } else if (type.contains("38425053")) {
            return "psd";
        } else if (type.contains("3C3F786D6C")) {
            return "xml";
        } else if (type.contains("68746D6C3E")) {
            return "html";
        } else if (type.contains("D0CF11E0")) {
            return "doc";
        } else if (type.contains("255044462D312E")) {
            return "pdf";
        } else if (type.contains("504B0304")) {
            return "zip";
        } else if (type.contains("52617221")) {
            return "rar";
        } else if (type.contains("57415645")) {
            return "wav";
        } else if (type.contains("41564920")) {
            return "avi";
        } else if (type.contains("2E7261FD")) {
            return "ram";
        } else if (type.contains("2E524D46")) {
            return "rm";
        } else if (type.contains("000001BA")) {
            return "mpg";
        } else if (type.contains("3026B2758E66CF11")) {
            return "asf";
        } else if (type.contains("4D546864")) {
            return "mid";
        } else if (type.contains("000000186674797033677035")) {
            return "mp4";
        } else { //txt
            return "";
        }
    }

    /**
     * 获取图片的类型
     *
     * @param fileTypeByte  文件bytes流
     * @return  文件类型
     */
    private static String getImageTypeByStream(byte[] fileTypeByte) {

        String type = bytesToHexString(fileTypeByte).toUpperCase();
        if (type.contains("FFD8FF")) {
            return "jpg";
        } else if (type.contains("89504E47")) {
            return "png";
        } else if (type.contains("47494638")) {
            return "gif";
        } else if (type.contains("49492A00")) {
            return "tif";
        } else if (type.contains("424D")) { //后面的不是图片
            return "bmp";
        } else {
            return "notImage";
        }
    }

    /**
     * 将byte数组转换为16进制字符串
     *
     * @param src  文件bytes流
     * @return 16进制字符串
     */
    private static String bytesToHexString(byte[] src) {
        final int len = 2;
        final int n = 0xFF;
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & n;
            String hv = Integer.toHexString(v);
            if (hv.length() < len) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * 将文件转化为base64 String
     *
     * @param file 文件
     * @return base64 String
     */
    public String fileToBase64(File file) {
        String base64 = null;
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            in.close();
            base64 = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.info("你传输的文件类型不对，请选择文件！");
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return base64;
    }

    /**
     * 将图片文件转化为base64 String
     *
     * @param imageUploadFile 文件
     * @return base64 String
     */
    public String imageToBase64(MultipartFile imageUploadFile) {
        byte[] data = null;
        //读取图片字节数组
        try {
            InputStream in = imageUploadFile.getInputStream();
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //对字节数组Base64编码并返回Base64编码过的字节数组字符串
        return new BASE64Encoder().encode(data);
    }

    /**
     * 接收到的图片形式将是base64String类型
     *
     * @param base64 base64 String
     * @return 图片bytes流
     * @throws Exception 抛出异常
     */
    public byte[] generateImage(String base64) throws Exception {
        final int len = 256;
        //图像数据为空
        if (null == base64) {
            throw new Exception("空值");
        }
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            //Base64解码
            byte[] b = decoder.decodeBuffer(base64);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) { //调整异常数据
                    b[i] += len;
                }
            }
            return b;
        } catch (Exception e) {
            throw new Exception("base64编码转换为图片流错误");
        }
    }

    /**
     * 上传图片
     *
     * @param fileTransferVo 上传图片对象
     * @return 上传后返回地址
     * @throws Exception 抛出异常
     */
    public String uploadImage(FileTransferVo fileTransferVo) throws Exception {

        StringBuffer path = new StringBuffer();

        try {
            String[] strings = getStorageClient().upload_file(inputToByte(fileTransferVo.getInputStream()),
                    fileTransferVo.getFileType(), null);

            path.append(strings[0]).append("/").append(strings[1]);

            log.info("上传路径=107.182.23.5:8888/" + path.toString());
        } catch (MyException e) {
            throw new Exception("获取连接失败！");
        } catch (IOException e) {
            throw new Exception("获取连接失败！");
        }
        return path.toString();
    }

    /**
     * 获取fastDFS的storageClient
     *
     * @return storageClient
     * @throws Exception 抛出异常
     */
    private StorageClient getStorageClient() throws Exception {
        // 初始化文件资源
        ClassPathResource cpr = new ClassPathResource("fdfs_client.conf");
        ClientGlobal.init(cpr.getClassLoader().getResource("fdfs_client.conf").getPath());
        // 链接FastDFS服务器，创建tracker和Storage
        TrackerClient trackerClient = new TrackerClient();

        // 3、使用 TrackerClient 对象创建连接，获得一个 TrackerServer 对象。
        TrackerServer trackerServer = trackerClient.getConnection();
        // 4、创建一个 StorageServer 的引用，值为 null
        StorageServer storageServer = null;
        // 5、创建一个 StorageClient 对象，需要两个参数 TrackerServer 对象、StorageServer 的引用
        return new StorageClient(trackerServer, storageServer);
    }

    /**
     * inputStream 转 bytes数组
     *
     * @param inStream 流
     * @return bytes数组
     * @throws IOException 抛出异常
     */
    public byte[] inputToByte(InputStream inStream)
            throws IOException {
        final int len = 100;
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[len];
        int rc = 0;
        while ((rc = inStream.read(buff, 0, len)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] bytes = swapStream.toByteArray();
        return bytes;
    }

}
