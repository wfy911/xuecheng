package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

public interface MediaProcessService {
    List<MediaProcess> selectListByShardIndex(int shardTotal, int shardIndex, int count);

    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);

    Boolean startTask(Long taskId);
}
