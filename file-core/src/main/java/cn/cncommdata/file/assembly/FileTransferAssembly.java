package cn.cncommdata.file.assembly;

import cn.cncommdata.file.model.FileTransfer;
import cn.cncommdata.file.vo.FileTransferVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author： leimin
 * @description：
 * @create： on 2019/03/26
 **/
@Component
public class FileTransferAssembly {

    /**
     * domain 转 vo
     *
     * @param fileTransfer 文件model
     * @return 文件vo
     */
    public FileTransferVo toFileTransferVo(FileTransfer fileTransfer) {

        FileTransferVo fileTransferVo = new FileTransferVo();

        if (fileTransfer == null) {
            return fileTransferVo;
        }

        BeanUtils.copyProperties(fileTransfer, fileTransferVo);

        return fileTransferVo;
    }

    /**
     * 批量 domain 转 vo List
     *
     * @param fileTransfers model list
     * @return vo list
     */
    public List<FileTransferVo> toFileTransferVoList(List<FileTransfer> fileTransfers) {

        List<FileTransferVo> fileTransferVos = new ArrayList<FileTransferVo>();

        if (CollectionUtils.isEmpty(fileTransfers)) {
            return fileTransferVos;
        }

        for (FileTransfer fileTransfer : fileTransfers) {
            if (fileTransfer != null) {
                fileTransferVos.add(toFileTransferVo(fileTransfer));
            }
        }

        return fileTransferVos;
    }
}
