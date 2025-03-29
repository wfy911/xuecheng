package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.security.SecurityUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Autowired
    CourseTeacherMapper courseTeacherMapper;
    @Override
    public PageResult<CourseBase> list(Long companyId,PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //拼接查询条件
        //根据课程名称模糊查询  name like '%名称%'
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        //根据课程审核状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        //根据课程发布状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());
        if (companyId==null){
            XueChengPlusException.cast("companyId为空");
        }
        queryWrapper.eq(CourseBase::getCompanyId,companyId);
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        //分页查询E page 分页参数, @Param("ew") Wrapper<T> queryWrapper 查询条件
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        //数据
        List<CourseBase> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();

        //准备返回数据 List<T> items, long counts, long page, long pageSize
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());
        return courseBasePageResult;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        CourseBase courseBaseNew = new CourseBase();
        BeanUtils.copyProperties(dto,courseBaseNew);
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        courseBaseNew.setAuditStatus("202002");
        int insert = courseBaseMapper.insert(courseBaseNew);
        courseMarketInsert(dto,courseBaseNew.getId());
        CourseBaseInfoDto courseBaseInfoDto = heBing(courseBaseNew.getId());
        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto selectById(Long id) {
        CourseBaseInfoDto courseBaseInfoDto = heBing(id);
        return courseBaseInfoDto;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto updateById(EditCourseDto editCourseDto, Long companyId) {
        CourseBase courseBase = courseBaseMapper.selectById(editCourseDto.getId());
        if (courseBase==null){
            XueChengPlusException.cast("课程不存在");
        }
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("companyId不一致");
        }
        BeanUtils.copyProperties(editCourseDto,courseBase);
        courseBase.setChangeDate(LocalDateTime.now());
//        courseBase.setCompanyId(companyId);
        courseBaseMapper.updateById(courseBase);
        courseMarketUpdate(editCourseDto);
        CourseBaseInfoDto courseBaseInfoDto = heBing(editCourseDto.getId());
        return courseBaseInfoDto;
    }

    @Override
    public void deleteById(Long courseId,Long companyId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase==null){
            XueChengPlusException.cast("课程不存在");
        }
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("companyId不一致");
        }
        courseBaseMapper.deleteById(courseId);
        courseMarketMapper.deleteById(courseId);
        //课程计划
        LambdaQueryWrapper<Teachplan> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        teachplanMapper.delete(queryWrapper);
        //删除teachplanMeida
        LambdaQueryWrapper<TeachplanMedia> queryWrapperMedia=new LambdaQueryWrapper<>();
        queryWrapperMedia.eq(TeachplanMedia::getCourseId,courseId);
        teachplanMediaMapper.delete(queryWrapperMedia);
        //删除课程教师信息
        LambdaQueryWrapper<CourseTeacher> queryWrapperCourseTeacher=new LambdaQueryWrapper<>();
        queryWrapperCourseTeacher.eq(CourseTeacher::getCourseId,courseId);
        courseTeacherMapper.delete(queryWrapperCourseTeacher);

    }


    //返回相应结果
    public CourseBaseInfoDto heBing(Long id){
        if (id==null){
            throw new RuntimeException("id为空");
        }
        //组合返回结果
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (courseMarket!=null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }
        //课程分类名称查询
        String mtName = courseCategoryMapper.selectById(courseBaseInfoDto.getMt()).getName();
        courseBaseInfoDto.setMtName(mtName);
        String stName = courseCategoryMapper.selectById(courseBaseInfoDto.getSt()).getName();
        courseBaseInfoDto.setStName(stName);
        return courseBaseInfoDto;

    }

    public Long courseMarketInsert(AddCourseDto dto,Long id){
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (dto.getOriginalPrice()<=0 || dto.getPrice()<=0){
            XueChengPlusException.cast("课程价格小于等于0");
        }
        if (courseMarket==null){
            CourseMarket courseMarketNew = new CourseMarket();
            BeanUtils.copyProperties(dto,courseMarketNew);
            courseMarketNew.setId(id);
            int insert = courseMarketMapper.insert(courseMarketNew);
        }else {
            BeanUtils.copyProperties(dto,courseMarket);
            courseMarketMapper.updateById(courseMarket);
        }
        return id;
    }

    public void courseMarketUpdate(EditCourseDto dto){
        CourseMarket courseMarket = courseMarketMapper.selectById(dto.getId());
        if (dto.getOriginalPrice()<=0 || dto.getPrice()<=0){
            XueChengPlusException.cast("课程价格小于等于0");
        }
        if (courseMarket==null){
            CourseMarket courseMarketNew = new CourseMarket();
            BeanUtils.copyProperties(dto,courseMarketNew);
            courseMarketMapper.insert(courseMarketNew);
        }else {
            BeanUtils.copyProperties(dto,courseMarket);
            courseMarketMapper.updateById(courseMarket);
        }
        return;
    }
}
