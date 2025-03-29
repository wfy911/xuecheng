package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Override
    public List<CourseTeacher> selectById(Long id) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,id);
        List<CourseTeacher> courseTeacher = courseTeacherMapper.selectList(queryWrapper);
        return courseTeacher;
    }

    @Override
    public CourseTeacher insertCourseTeacher(CourseTeacher courseTeacher,Long companyId) {
        Long courseId = courseTeacher.getCourseId();
        Long companyId1 = courseBaseMapper.selectById(courseId).getCompanyId();
        if (!companyId.equals(companyId1)){
            XueChengPlusException.cast("机构id不一致");
        }
        Long id = courseTeacher.getId();
        if (id==null){
            courseTeacherMapper.insert(courseTeacher);
            return courseTeacher;
        }
        CourseTeacher courseTeacher1 = courseTeacherMapper.selectById(id);
        if (courseTeacher1==null){
            XueChengPlusException.cast("id不存在");
        }
        courseTeacherMapper.updateById(courseTeacher);
        return courseTeacher;
    }

    @Override
    public void deleteById(Long courseId, Long id,Long companyId) {
        Long companyId1 = courseBaseMapper.selectById(courseId).getCompanyId();
        if (!companyId.equals(companyId1)){
            XueChengPlusException.cast("机构id不一致");
        }
        LambdaQueryWrapper<CourseTeacher> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getId,id).eq(CourseTeacher::getCourseId,courseId);
        courseTeacherMapper.delete(queryWrapper);
    }

}
