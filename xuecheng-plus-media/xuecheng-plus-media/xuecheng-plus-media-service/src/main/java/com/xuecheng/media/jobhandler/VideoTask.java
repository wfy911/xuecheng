package com.xuecheng.media.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.service.MediaProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 利用分布所和xxljob进行视频处理
 *
 */
@Slf4j
@Component
public class VideoTask {
    private static Logger logger = LoggerFactory.getLogger(VideoTask.class);

    @Autowired
    MediaProcessService mediaProcessService;

    @Autowired
    MediaFileService mediaFileService;
    /**
     * 2、分片广播任务
     */
    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws InterruptedException {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        int processors = Runtime.getRuntime().availableProcessors();
        List<MediaProcess> mediaProcesses = mediaProcessService.selectListByShardIndex(shardTotal, shardIndex, processors);
        int size=mediaProcesses.size();
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        CountDownLatch countDownLatch= new CountDownLatch(size);
        mediaProcesses.forEach(mediaProcess -> executorService.execute(()->{
            try {
                //抢占任务
                Long id = mediaProcess.getId();
                Boolean b = mediaProcessService.startTask(id);
                if (!b){
                    log.debug("为将占到任务，taskid:{}",id);
                    return;
                }
                //从服务器上下载文件'
                String fileId=mediaProcess.getFileId();
                String filename = mediaProcess.getFilename();
                String bucket = mediaProcess.getBucket();
                String objectName = mediaProcess.getFilePath();
                File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                if (file==null){
                    log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "下载待处理文件失败");
                    return;

                }
                //处理结束的视频文件
                File mp4File = null;
                try {
                    mp4File = File.createTempFile("mp4", ".mp4");
                } catch (IOException e) {
                    log.error("创建mp4临时文件失败");
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "创建mp4临时文件失败");
                    return;
                }
                //转码
                //ffmpeg的路径
                String ffmpeg_path = "F:\\develop\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe";//ffmpeg的安装位置
                //源avi视频的路径
                String video_path = file.getAbsolutePath();
                //转换后mp4文件的名称
                String mp4_name = mp4File.getName();
                //转换后mp4文件的路径
                String mp4_path = mp4File.getAbsolutePath();
                //创建工具类对象
                Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpeg_path,video_path,mp4_name,mp4_path);
                //开始视频转换，成功将返回success
                String s = videoUtil.generateMp4();
                if (!"success".equals(s)){
                    log.error("转换mp4文件失败");
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "转换mp4文件失败");
                    return;
                }
                //计算objectName
                String filePathByMd5 = mediaFileService.getFilePathByMd5(fileId, ".mp4");
                //上传到minio
                boolean flag = mediaFileService.UploadFileToMinIo(".mp4", mp4File.getAbsolutePath(), filePathByMd5, bucket);
                if (!flag){
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "上传到minio失败");
                    return;
                }
                ///mediafiles/2024/12/18/0716c21e226b2abc0e42bbfaf30955f0.jpg
                String url="/"+bucket+"/"+filePathByMd5;
                //更新状态
                mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(),"2",fileId,url,null);
            } finally {
                countDownLatch.countDown();
            }

        }));
        countDownLatch.await(30, TimeUnit.MINUTES);


    }


    /**
     * 3、命令行任务
     */
    @XxlJob("commandJobHandler")
    public void commandJobHandler() throws Exception {
        String command = XxlJobHelper.getJobParam();
        int exitValue = -1;

        BufferedReader bufferedReader = null;
        try {
            // command process
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            //Process process = Runtime.getRuntime().exec(command);

            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));

            // command log
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                XxlJobHelper.log(line);
            }

            // command exit
            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            XxlJobHelper.log(e);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        if (exitValue == 0) {
            // default success
        } else {
            XxlJobHelper.handleFail("command exit value("+exitValue+") is failed");
        }

    }


    /**
     * 4、跨平台Http任务
     *  参数示例：
     *      "url: http://www.baidu.com\n" +
     *      "method: get\n" +
     *      "data: content\n";
     */
    @XxlJob("httpJobHandler")
    public void httpJobHandler() throws Exception {

        // param parse
        String param = XxlJobHelper.getJobParam();
        if (param==null || param.trim().length()==0) {
            XxlJobHelper.log("param["+ param +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }

        String[] httpParams = param.split("\n");
        String url = null;
        String method = null;
        String data = null;
        for (String httpParam: httpParams) {
            if (httpParam.startsWith("url:")) {
                url = httpParam.substring(httpParam.indexOf("url:") + 4).trim();
            }
            if (httpParam.startsWith("method:")) {
                method = httpParam.substring(httpParam.indexOf("method:") + 7).trim().toUpperCase();
            }
            if (httpParam.startsWith("data:")) {
                data = httpParam.substring(httpParam.indexOf("data:") + 5).trim();
            }
        }

        // param valid
        if (url==null || url.trim().length()==0) {
            XxlJobHelper.log("url["+ url +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }
        if (method==null || !Arrays.asList("GET", "POST").contains(method)) {
            XxlJobHelper.log("method["+ method +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }
        boolean isPostMethod = method.equals("POST");

        // request
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            // connection
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            // connection setting
            connection.setRequestMethod(method);
            connection.setDoOutput(isPostMethod);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(5 * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            // do connection
            connection.connect();

            // data
            if (isPostMethod && data!=null && data.trim().length()>0) {
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(data.getBytes("UTF-8"));
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            // valid StatusCode
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                throw new RuntimeException("Http Request StatusCode(" + statusCode + ") Invalid.");
            }

            // result
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            String responseMsg = result.toString();

            XxlJobHelper.log(responseMsg);

            return;
        } catch (Exception e) {
            XxlJobHelper.log(e);

            XxlJobHelper.handleFail();
            return;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                XxlJobHelper.log(e2);
            }
        }

    }

    /**
     * 5、生命周期任务示例：任务初始化与销毁时，支持自定义相关逻辑；
     */
    @XxlJob(value = "demoJobHandler2", init = "init", destroy = "destroy")
    public void demoJobHandler2() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");
    }
    public void init(){
        logger.info("init");
    }
    public void destroy(){
        logger.info("destroy");
    }


}
