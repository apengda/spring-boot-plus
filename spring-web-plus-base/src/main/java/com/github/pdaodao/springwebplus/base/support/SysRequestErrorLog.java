package com.github.pdaodao.springwebplus.base.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.pdaodao.springwebplus.base.entity.SnowIdEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "sys_access_error_log", autoResultMap = true)
public class SysRequestErrorLog extends SnowIdEntity {
    // 用户id
    private String userId;

    private String userTitle;

    // 请求路径
    private String path;

    // 错误信息
    private String msg;

    private String trace;

    private Date createTime;
}
