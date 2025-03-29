package com.xuecheng.content.api;


import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.*;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
public class CourseBaseInfoController {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;
    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
//    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")
    public PageResult<CourseBase> list(PageParams pageParams,@RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
//        SecurityUtil.XcUser user = SecurityUtil.getUser();
//        String companyId = user.getCompanyId();
        Long companyId=1232141425l;
        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.list(companyId,pageParams, queryCourseParamsDto);
        return courseBasePageResult;
    }

    @ApiOperation("课程添加接口")
    @PostMapping("/course")
    public CourseBaseInfoDto CourseAdd(@RequestBody @Validated AddCourseDto addCourseDto){
        Long companyId=1232141425l;
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.createCourseBase(companyId, addCourseDto);
        return courseBaseInfoDto;
    }

    @ApiOperation("根据id查询课程信息")
    @GetMapping("course/{id}")
    public CourseBaseInfoDto selectById(@PathVariable Long id){
        //取出当前用户身份
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Object credentials = SecurityContextHolder.getContext().getAuthentication().getCredentials();
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
//        Long companyId=1232141425l;
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.selectById(id);
        return courseBaseInfoDto;
    }

    @ApiOperation("修改课程信息")
    @PutMapping("course")
    public CourseBaseInfoDto updateById(@RequestBody @Validated EditCourseDto editCourseDto){
        Long companyId=1232141425l;
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.updateById(editCourseDto, companyId);
        return courseBaseInfoDto;
    }

    @ApiOperation("添加章节")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachPlanDto> getTreeNodes(@PathVariable Long courseId){
        List<TeachPlanDto> teachPlanDto = teachplanService.findTeachplanTree(courseId);
        return teachPlanDto;
    }

    @ApiOperation("删除课程")
    @DeleteMapping("/course/{courseId}")
    public void deleteById(@PathVariable Long courseId){
        Long companyId=1232141425l;
        courseBaseInfoService.deleteById(courseId,companyId);
    }
}
