package com.xuecheng.content.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author itcast
 */
@Data
@AllArgsConstructor
@TableName("operate_log")
public class OperateLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String operateUser;

    @TableField("operateTime")
    private LocalDateTime operatetime;

    @TableField("className")
    private String classname;

    @TableField("methodName")
    private String methodname;

    @TableField("methodParams")
    private String methodparams;

    @TableField("returnValue")
    private String returnvalue;

    @TableField("costTime")
    private long costtime;



}
