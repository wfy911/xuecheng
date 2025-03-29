package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaFileService mediaFileService;
    @Autowired
    MinioClient minioClient;

    @Value("${minio.bucket.files}")
    String bucket_files;
    @Value("${minio.bucket.videofiles}")
    String bucket_video;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath,String objectName) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            XueChengPlusException.cast("文件不存在");
        }
        //获取MD5值并比较
        String md5 = getFileMd5(file);
        if (md5 == null) {
            XueChengPlusException.cast("md5为空");
        }
        //组合objectName并上传
        String filename = uploadFileParamsDto.getFilename();
        String[] split = filename.split("\\.");
        String extension = split[split.length - 1];
        if (StringUtils.isEmpty(objectName)){
            objectName = getDefaultFolderPath() + md5 + "." + extension;
        }
//  String mimeType = getMimeType(extension);
        boolean flag = UploadFileToMinIo(extension, localFilePath, objectName, bucket_files);
        if (!flag) {
            XueChengPlusException.cast("文件上传到minio失败");
        }
        //上传到mediafiles
        MediaFiles mediaFiles = addMediaFilesToDb(companyId, md5, uploadFileParamsDto, objectName, bucket_files);
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        return folder;
    }

    @Override
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String objectName, String bucket) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            //填充mediafiles
            mediaFiles.setId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setFileId(fileMd5);
            String url = "/" + bucket+ "/" + objectName;
            mediaFiles.setUrl(url);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setChangeDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.error("保存文件信息到数据库失败,{}", mediaFiles.toString());
                XueChengPlusException.cast("导入mediafiles失败");
            }
            addWaitingTask(mediaFiles);

        }
        log.debug("保存文件信息到数据库成功,{}", mediaFiles);
        return mediaFiles;
    }

    private void addWaitingTask(MediaFiles mediaFiles){
        String filename = mediaFiles.getFilename();
        String contension = filename.substring(filename.lastIndexOf("."));
        if (!contension.equals(".avi")){
            return;
        }
        MediaProcess mediaProcess=new MediaProcess();
        BeanUtils.copyProperties(mediaFiles,mediaProcess);
        mediaProcess.setStatus("1");
        mediaProcess.setCreateDate(LocalDateTime.now());
        mediaProcess.setFailCount(0);
        mediaProcess.setUrl(null);
        int insert = mediaProcessMapper.insert(mediaProcess);
        if (insert<0){
            XueChengPlusException.cast("出入mediaprocess失败");
        }
    }



    public String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/chunk/";
    }

    @Override
    public RestResponse<Boolean> checkfile(String fileMd5) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(mediaFiles.getBucket())
                    .object(mediaFiles.getFilePath()).build();
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if (inputStream != null) {
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkchunk(String fileMd5, int chunk) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(chunkFileFolderPath + chunk).build();
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if (inputStream != null) {
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String absolutePath) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        String objectName = chunkFileFolderPath + chunk;
        boolean b = UploadFileToMinIo("", absolutePath, objectName, bucket_video);
        if (!b) {
            return RestResponse.validfail(false, "上传分块失败");
        }
        return RestResponse.success(true);

    }

    @Override
    public RestResponse<Boolean> mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //合并分块
        String filename = uploadFileParamsDto.getFilename();
        String contension = filename.substring(filename.lastIndexOf("."));
        String mergeName = getFilePathByMd5(fileMd5, contension);
        try {
            String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
            //组成将分块文件路径组成 List<ComposeSource>
            List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> ComposeSource.builder()
                            .bucket(bucket_video)
                            .object(chunkFileFolderPath.concat(Integer.toString(i)))
                            .build()).collect(Collectors.toList());
            ComposeObjectArgs composeObjectArgs = ComposeObjectArgs
                    .builder()
                    .bucket(bucket_video)
                    .sources(sourceObjectList)
                    .object(mergeName).build();
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("companyId:{} fileMd5:{} chunktotal:{} uploadFileParamsDto:{}", companyId, fileMd5, chunkTotal, uploadFileParamsDto);
            return RestResponse.validfail(false, "合并模块失败");
        }
        //校验MD5值
        File file = downloadFileFromMinIO(bucket_video, mergeName);
        uploadFileParamsDto.setFileSize(file.length());
        try {
            FileInputStream inputStream = new FileInputStream(file);
            String md5Hex = DigestUtils.md5Hex(inputStream);
            if (!fileMd5.equals(md5Hex)) {
                return RestResponse.validfail(false, "校验MD5值错误");
            }
        } catch (Exception e) {
            log.error("校验文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage(), e);
            return RestResponse.validfail(false, "校验MD5值错误");
        }
        //上传文件到mediafiles

        MediaFiles mediaFiles = mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, mergeName, bucket_video);
        if (mediaFiles == null) {
            return RestResponse.validfail(false, "上传文件到mediafiles失败");
        }
        //清除chunk文件
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        clearChunkFiles(chunkFileFolderPath, chunkTotal);
        return RestResponse.success(true);
    }

    /**
     * 清除分块文件
     *
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal          分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {

        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清除分块文件失败,objectname:{}", deleteError.objectName(), e);
                    XueChengPlusException.cast("清除分块文件失败");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
            XueChengPlusException.cast("清除分块文件失败");
        }
    }


    /**
     * 从minio下载文件
     *
     * @param bucket     桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            if (outputStream != null) {
                return minioFile;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("bucket:{} objectName:{} ", bucket, objectName);
        }
        return null;
    }

    public String getFileMd5(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            String md5 = DigestUtils.md5Hex(fileInputStream);
            return md5;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("file:{}", file);
            return null;
        }
    }

    private String getMimeType(String extension) {
        if (extension == null)
            extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    public String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    @Override
    public MediaFiles getFileById(String mediaId) {
        return mediaFilesMapper.selectById(mediaId);
    }


    //上传到minio
    public boolean UploadFileToMinIo(String extension, String localFilePath, String objectName, String bucket) {
        try {
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
            String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
            if (extensionMatch != null) {
                mimeType = extensionMatch.getMimeType();
            }
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs
                    .builder()
                    .bucket(bucket)
                    .filename(localFilePath)
                    .object(objectName)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("extension:{} localFilePath:{} objectName:{}", extension, localFilePath, objectName);
            return false;
        }
        return true;
    }
}
