package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;
    @Override
    public List<TeachPlanDto> findTeachplanTree(Long courseId) {
        List<TeachPlanDto> teachPlanDto = teachplanMapper.selectTreeNodes(courseId);
        return teachPlanDto;
    }

    @Override
    public void saveTeachplan(TeachPlanDto teachPlanDto) {
        Long id = teachPlanDto.getId();
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (teachplan==null){
            teachplan = new Teachplan();
            BeanUtils.copyProperties(teachPlanDto,teachplan);
            Integer count = orderBy(teachplan);
            teachplan.setOrderby(count);
            teachplanMapper.insert(teachplan);
        }else {
            BeanUtils.copyProperties(teachPlanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    @Override
    public void deleteTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        Long parentid = teachplan.getParentid();
        if (parentid==0){
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid,id);
            Integer count = teachplanMapper.selectCount(queryWrapper);
            if (count==0){
                teachplanMapper.deleteById(id);
            }else {
                XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
            }
        }else {
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId,id);
            teachplanMediaMapper.delete(queryWrapper);
            teachplanMapper.deleteById(id);
        }
    }

    @Override
    public void moveById(String move, Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        Long courseId = teachplan.getCourseId();
        Long parentid = teachplan.getParentid();
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper=queryWrapper.eq(Teachplan::getCourseId,courseId).eq(Teachplan::getParentid,parentid);
        List<Teachplan> teachplans = teachplanMapper.selectList(queryWrapper);
        if ("moveup".equals(move)){
            Collections.sort(teachplans, (o1, o2) -> o2.getOrderby() - o1.getOrderby());
            for (Teachplan teachplan1 : teachplans) {
                if (teachplan1.getOrderby()<teachplan.getOrderby()){
                    Integer temp=teachplan.getOrderby();
                    teachplan.setOrderby(teachplan1.getOrderby());
                    teachplan1.setOrderby(temp);
                    teachplanMapper.updateById(teachplan);
                    teachplanMapper.updateById(teachplan1);
                    return;
                }
            }
        }else {
            Collections.sort(teachplans, (o1, o2) -> o1.getOrderby() - o2.getOrderby());
            for (Teachplan teachplan1 : teachplans) {
                if (teachplan1.getOrderby()>teachplan.getOrderby()){
                    Integer temp=teachplan.getOrderby();
                    teachplan.setOrderby(teachplan1.getOrderby());
                    teachplan1.setOrderby(temp);
                    teachplanMapper.updateById(teachplan);
                    teachplanMapper.updateById(teachplan1);
                    return;
                }
            }
        }
    }

    @Override
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        //验证teachplan
        if(teachplan==null){
            XueChengPlusException.cast("教学计划不存在");
        }
        Integer grade = teachplan.getGrade();
        if(grade!=2){
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }

        String mediaId = bindTeachplanMediaDto.getMediaId();
        String fileName = bindTeachplanMediaDto.getFileName();
        LambdaQueryWrapper<TeachplanMedia> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId,teachplanId);
        teachplanMediaMapper.delete(queryWrapper);
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setMediaId(mediaId);
        teachplanMedia.setMediaFilename(fileName);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
    }

    @Override
    public void deleteTeachplanMedia(Long teachPlanId, Long mediaId) {
        LambdaQueryWrapper<TeachplanMedia> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId,teachPlanId).eq(TeachplanMedia::getMediaId,mediaId);
        teachplanMediaMapper.delete(queryWrapper);
    }

    public Integer orderBy(Teachplan teachplan){
        Long courseId = teachplan.getCourseId();
        Long parentid = teachplan.getParentid();
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper=queryWrapper.eq(Teachplan::getCourseId,courseId).eq(Teachplan::getParentid,parentid);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count+1;
    }
}
