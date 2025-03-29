package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CourseTeacherService courseTeacherService;

    @Autowired
    CourseBaseMapper courseBaseMapper;


    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {

        //课程基本信息、营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.selectById(courseId);

        //课程计划信息
        List<TeachPlanDto> teachplanTree= teachplanService.findTeachplanTree(courseId);

        //教师计划信息
        List<CourseTeacher> courseTeachers = courseTeacherService.selectById(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);
        coursePreviewDto.setCourseTeachers(courseTeachers);
        return coursePreviewDto;
    }

    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.selectById(courseId);
        if (!courseBaseInfoDto.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("companyid不一致");
        }
        //课程审核状态
        String auditStatus = courseBaseInfoDto.getAuditStatus();
        //当前审核状态为已提交不允许再次提交
        if("202003".equals(auditStatus)){
            XueChengPlusException.cast("当前为等待审核状态，审核完成可以再次提交。");
        }
        //课程图片是否填写
        if(StringUtils.isEmpty(courseBaseInfoDto.getPic())){
            XueChengPlusException.cast("提交失败，请上传课程图片");
        }
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfoDto,coursePublishPre);
        //课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转为json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        //将课程营销信息json数据放入课程预发布表
        coursePublishPre.setMarket(courseMarketJson);

        //查询课程计划信息
        List<TeachPlanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if(teachplanTree.size()<=0){
            XueChengPlusException.cast("提交失败，还没有添加课程计划");
        }
        //转json
        String teachplanTreeString = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeString);

        //查询教师信息
        List<CourseTeacher> courseTeachers = courseTeacherService.selectById(courseId);
        //转json
        String courseTeachersString = JSON.toJSONString(courseTeachers);
        coursePublishPre.setTeachers(courseTeachersString);
        //设置预发布记录状态,已提交
        coursePublishPre.setStatus("202003");
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPreUpdate == null){
            //添加课程预发布记录
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更新课程基本表的审核状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    @Transactional
    @Override
    public void coursePublish(Long companyId, Long courseId) {
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre==null){
            XueChengPlusException.cast("课程未欲发布");
        }
        if (!companyId.equals(coursePublishPre.getCompanyId())){
            XueChengPlusException.cast("companyid不一致");
        }
        if (!coursePublishPre.getStatus().equals("202004")){
            XueChengPlusException.cast("未审核通过");
        }
        //添加到课程发布表
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        CoursePublish coursePublish1 = coursePublishMapper.selectById(courseId);
        if (coursePublish1==null){
            coursePublishMapper.insert(coursePublish);
        }else {
            coursePublishMapper.updateById(coursePublish);
        }
        //添加到消息表
        mqMessageService.addMessage("course_publish",String.valueOf(courseId),null,null);

        //删除课程预发布表对应记录
        coursePublishPreMapper.deleteById(courseId);

    }

    @Override
    public File generateCourseHtml(Long courseId) {
        File tempHtml=null;
        try {
            //配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());
            //加载模板
            //选指定模板路径,classpath下templates下
            //得到classpath路径
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);
            //静态化
            //参数1：模板，参数2：数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            System.out.println(content);
            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);
            //输出流
            tempHtml = File.createTempFile("course", ".html");
            FileOutputStream outputStream = new FileOutputStream(tempHtml);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
            XueChengPlusException.cast("生成静态化页面失败");
        }
        return tempHtml;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.upload(multipartFile, "course/"+courseId+".html");
        if(course==null){
            XueChengPlusException.cast("上传静态文件异常");
        }
    }

    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        if (coursePublish==null){
            XueChengPlusException.cast("课程未出版");
        }
        return coursePublish;
    }

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Override
    public CoursePublish getCoursePublishCathe(Long courseId) {
        Object jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
        if (jsonObj!=null){
            String jsonString = jsonObj.toString();
            if (jsonString.equals("null")){
                return null;
            }
            CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
            return coursePublish;
        }
//        redisTemplate.opsForValue().set
        RLock lock = redissonClient.getLock("courslock" + courseId);
        lock.lock();
        try {
            jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
            if (jsonObj!=null){
                String jsonString = jsonObj.toString();
                if (jsonString.equals("null")){
                    return null;
                }
                CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
                return coursePublish;
            }
            CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
            redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish), 30 + new Random().nextInt(100), TimeUnit.SECONDS);
            return coursePublish;
        }finally {
            System.out.println("释放锁");
            lock.unlock();
        }
    }

}
