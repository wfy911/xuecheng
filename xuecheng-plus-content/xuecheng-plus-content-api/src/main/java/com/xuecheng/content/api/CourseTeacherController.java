package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CourseTeacherController {

    @Autowired
    CourseTeacherService courseTeacherService;
    @GetMapping("/courseTeacher/list/{id}")
    public List<CourseTeacher> selectById(@PathVariable Long id){
        List<CourseTeacher> courseTeacher = courseTeacherService.selectById(id);
        return courseTeacher;
    }

    @PostMapping("/courseTeacher")
    public CourseTeacher insertCourseTeacher(@RequestBody CourseTeacher courseTeacher){
        Long companyId=1232141425l;
        return courseTeacherService.insertCourseTeacher(courseTeacher,companyId);
    }

    @DeleteMapping("/ourseTeacher/course/{courseId}/{id}")
    public void deleteById(@PathVariable Long courseId,@PathVariable Long id){
        Long companyId=1232141425l;
        courseTeacherService.deleteById(courseId,id,companyId);
    }

}
