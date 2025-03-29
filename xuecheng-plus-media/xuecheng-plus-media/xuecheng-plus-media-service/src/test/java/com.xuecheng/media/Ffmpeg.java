package com.xuecheng.media;

import com.xuecheng.base.utils.Mp4VideoUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Ffmpeg {

    @Test
    public void test(){
        //ffmpeg的路径
        String ffmpeg_path = "F:\\develop\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe";//ffmpeg的安装位置
        //源avi视频的路径
        String video_path = "F:\\BaiduNetdiskDownload\\资料\\day36-基础加强（日志，类加载器，单元测试，xml，注解，羊了个羊）\\02-类加载器\\学习视频\\04-类加载的过程-链接.avi";
        //转换后mp4文件的名称
        String mp4_name = "31.mp4";
        //转换后mp4文件的路径
        String mp4_path = "F:\\BaiduNetdiskDownload\\资料\\day36-基础加强（日志，类加载器，单元测试，xml，注解，羊了个羊）\\02-类加载器\\学习视频\\31.mp4";
        //创建工具类对象
        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpeg_path,video_path,mp4_name,mp4_path);
        //开始视频转换，成功将返回success
        String s = videoUtil.generateMp4();
        System.out.println(s);
    }


}

