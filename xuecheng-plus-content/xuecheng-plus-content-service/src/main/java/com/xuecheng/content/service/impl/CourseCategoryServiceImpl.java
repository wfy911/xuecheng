package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        Map<String, CourseCategoryTreeDto> map = courseCategoryTreeDtos.stream().filter(key->!id.equals(key.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value,(key1, key2) -> key2));
//        Map<String, CourseCategoryTreeDto> map = courseCategoryTreeDtos.stream().filter(item->!id.equals(item.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        List<CourseCategoryTreeDto> res=new ArrayList<>();
        courseCategoryTreeDtos.forEach(new Consumer<CourseCategoryTreeDto>() {
            @Override
            public void accept(CourseCategoryTreeDto courseCategoryTreeDto) {
                if (courseCategoryTreeDto.getParentid().equals(id)) {
                    res.add(courseCategoryTreeDto);
                }
                CourseCategoryTreeDto parentNode = map.get(courseCategoryTreeDto.getParentid());
                if (parentNode != null) {
                    if (parentNode.getChildrenTreeNodes() == null) {
                        parentNode.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                    }
                    parentNode.getChildrenTreeNodes().add(courseCategoryTreeDto);
                }

            }
        });

        return res;
    }
}
