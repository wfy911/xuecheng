package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.TeachPlanDto;

import java.util.List;

public interface TeachplanService {
    public List<TeachPlanDto> findTeachplanTree(Long courseId);

    public void saveTeachplan(TeachPlanDto teachPlanDto);

    public void deleteTeachplan(Long id);

    public void moveById(String move, Long id);

    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);

    public void deleteTeachplanMedia( Long teachPlanId,  Long mediaId);

}
