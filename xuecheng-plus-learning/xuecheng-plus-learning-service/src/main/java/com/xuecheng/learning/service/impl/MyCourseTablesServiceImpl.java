package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;
    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish==null){
            XueChengPlusException.cast("课程未发布");
        }
        String charge = coursepublish.getCharge();
        XcChooseCourse chooseCourse;
        //课程免费
        if (charge.equals("201000")){
            //写入选课记录表
            chooseCourse = addFreeCourse(userId, coursepublish);
            //写入我的课程表
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);
        }else {
            //课程收费
            //写入选课记录表
            chooseCourse = addChargeCourse(userId, coursepublish);
        }
        //获取学习资格
        XcCourseTablesDto learningStatus = getLearningStatus(userId, courseId);
        String learnStatus = learningStatus.getLearnStatus();
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,xcChooseCourseDto);
        xcChooseCourseDto.setLearnStatus(learnStatus);
        return xcChooseCourseDto;
    }

    //XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},
    // {"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        XcCourseTables xcCourseTables = existCourseTables(userId, courseId);
        if (xcCourseTables==null){
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        LocalDateTime validtimeEnd = xcCourseTables.getValidtimeEnd();
        boolean before = validtimeEnd.isBefore(LocalDateTime.now());
        if (before){
            xcCourseTablesDto.setLearnStatus("702003");
        }else {
            xcCourseTablesDto.setLearnStatus("702001");
        }
        return xcCourseTablesDto;
    }

    private XcCourseTables existCourseTables(String userId, Long courseId){
        LambdaQueryWrapper<XcCourseTables> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(XcCourseTables::getCourseId,courseId)
                .eq(XcCourseTables::getUserId,userId);
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(queryWrapper);
        return xcCourseTables;
    }

    private XcCourseTables addCourseTables(XcChooseCourse chooseCourse) {
        Long courseId = chooseCourse.getCourseId();
        String userId = chooseCourse.getUserId();
        XcCourseTables xcCourseTables = existCourseTables(userId, courseId);
        if (xcCourseTables!=null){
            return xcCourseTables;
        }
        xcCourseTables=new XcCourseTables();
        BeanUtils.copyProperties(chooseCourse,xcCourseTables);
        xcCourseTables.setChooseCourseId(chooseCourse.getId());
        xcCourseTables.setCourseType(chooseCourse.getOrderType());
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if (insert<=0){
            XueChengPlusException.cast("课程表添加失败");
        }
        return xcCourseTables;
    }

    private XcChooseCourse addChargeCourse(String userId, CoursePublish coursepublish) {
        Long courseId = coursepublish.getId();
        LambdaQueryWrapper<XcChooseCourse> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getCourseId,courseId)
                .eq(XcChooseCourse::getUserId,userId)
                .eq(XcChooseCourse::getOrderType,"700002")
                .eq(XcChooseCourse::getStatus,"701002");
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses.size()>0){
            return xcChooseCourses.get(0);
        }
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setStatus("701002");
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        xcChooseCourse.setRemarks(coursepublish.getRemark());
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert<=0){
            XueChengPlusException.cast("选课表添加失败");
        }
        return xcChooseCourse;
    }

    private XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        Long courseId = coursepublish.getId();
        LambdaQueryWrapper<XcChooseCourse> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getCourseId,courseId)
                .eq(XcChooseCourse::getUserId,userId)
                .eq(XcChooseCourse::getOrderType,"700001")
                .eq(XcChooseCourse::getStatus,"701001");
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses.size()>0){
            return xcChooseCourses.get(0);
        }
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setStatus("701001");
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        xcChooseCourse.setRemarks(coursepublish.getRemark());
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert<=0){
            XueChengPlusException.cast("选课表添加失败");
        }
        return xcChooseCourse;
    }


}
