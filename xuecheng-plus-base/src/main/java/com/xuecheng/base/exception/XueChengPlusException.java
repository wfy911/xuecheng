package com.xuecheng.base.exception;

public class XueChengPlusException extends RuntimeException{
    /**
     * 自定义异常
     */
    public XueChengPlusException() {
    }

    public XueChengPlusException(String message) {
        super(message);
    }

    public static void cast(String message){
        throw new XueChengPlusException(message);
    }
    public static void cast(CommonError commonError){
        throw new XueChengPlusException(commonError.getErrMessage());
    }

}
