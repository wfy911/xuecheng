package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.FileOutputStream;
import java.io.FilterInputStream;

public class minioTest {
    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();
    @Test
    public void upload() throws Exception {
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".jpg");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }

        UploadObjectArgs testBucket=UploadObjectArgs
                .builder()
                .bucket("testbucket")
                .filename("F:\\1733908794224.temp")
                .object("1.jpg")
                .contentType(mimeType)
                .build();
        minioClient.uploadObject(testBucket);
    }
    @Test
    public void delete() throws Exception{
        RemoveObjectArgs testbucket = RemoveObjectArgs
                .builder()
                .bucket("testbucket")
                .object("1.jpg")
                .build();
        minioClient.removeObject(testbucket);
    }

    @Test
    public void getFile(){
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("1.jpg").build();
        try(
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                FileOutputStream outputStream = new FileOutputStream("F:\\2.jpg");
        ) {
            IOUtils.copy(inputStream,outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
