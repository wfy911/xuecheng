package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeachplanController {

    @Autowired
    TeachplanService teachplanService;
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody @Validated TeachPlanDto teachPlanDto){
        teachplanService.saveTeachplan(teachPlanDto);
    }

    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachplan(@PathVariable Long id){
        teachplanService.deleteTeachplan(id);
    }

    @PostMapping("/teachplan/{move}/{id}")
    public void moveById(@PathVariable String move,@PathVariable Long id){
        teachplanService.moveById(move,id);
    }

    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto){
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }

    @DeleteMapping("/teachplan/association/media/{teachPlanId}/{mediaId}")
    public void deleteTeachplanMedia(@PathVariable Long teachPlanId,@PathVariable Long mediaId){

    }

}
