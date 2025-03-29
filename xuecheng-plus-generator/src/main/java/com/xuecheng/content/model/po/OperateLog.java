package com.xuecheng.content.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * <p>
 * 
 * </p>
 *
 * @author itcast
 */
@Data
@TableName("operate_log")
public class OperateLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer operateUser;

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
    private String costtime;


}
