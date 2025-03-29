package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

public interface CourseTeacherService {
    public List<CourseTeacher> selectById(Long id);

    public CourseTeacher insertCourseTeacher(CourseTeacher courseTeacher,Long companyId);

    public void deleteById(Long courseId,Long id,Long companyId);
}
