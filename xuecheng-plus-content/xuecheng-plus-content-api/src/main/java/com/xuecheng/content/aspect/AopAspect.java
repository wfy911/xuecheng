package com.xuecheng.content.aspect;

import com.alibaba.fastjson.JSONObject;
import com.xuecheng.content.mapper.OperateLogMapper;
import com.xuecheng.content.model.po.OperateLog;
import com.xuecheng.content.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Component
@Aspect
public class AopAspect {

    @Autowired
    OperateLogMapper operateLogMapper;
    @Autowired
    HttpServletRequest request;
    @Around("@annotation(com.xuecheng.content.aspect.MyLog)")
//    @Around("@annotation(myLog)")
    public Object recordTime(ProceedingJoinPoint joinPoint) throws Throwable {
        //操作人
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        String operateUser = user.getId();
        //操作时间
        LocalDateTime operateTime=LocalDateTime.now();
        //操作类名
        String className = joinPoint.getTarget().getClass().getName();
        //操作方法名
        String methodName = joinPoint.getSignature().getName();

        //操作方法参数
        Object[] args = joinPoint.getArgs();
        String methodParams= Arrays.toString(args);
        //操作方法返回值
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();
        String returnValue = JSONObject.toJSONString(result);
        //操作耗时
        long costTime=end-start;
        OperateLog operateLog=new OperateLog(null,operateUser,operateTime,className,methodName,methodParams,returnValue,costTime);
        operateLogMapper.insert(operateLog);
        return result;
    }
}
