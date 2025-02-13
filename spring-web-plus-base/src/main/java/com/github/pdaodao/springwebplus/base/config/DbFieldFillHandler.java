package com.github.pdaodao.springwebplus.base.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.github.pdaodao.springwebplus.base.util.RequestUtil;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * 创建时间、更新时间
 */
public class DbFieldFillHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        final Date now = new Date();
        this.setFieldValByName("createTime", now, metaObject);
        this.setFieldValByName("updateTime", now, metaObject);
        this.setFieldValByName("creatorId", RequestUtil.getUserId(), metaObject);
        this.setFieldValByName("creatorTitle", RequestUtil.getUserNickname(), metaObject);
        this.setFieldValByName("updatorId", RequestUtil.getUserId(), metaObject);
        this.setFieldValByName("updatorTitle", RequestUtil.getUserNickname(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        final Date now = new Date();
        this.setFieldValByName("updateTime", now, metaObject);
        this.setFieldValByName("updatorId", RequestUtil.getUserId(), metaObject);
        this.setFieldValByName("updatorTitle", RequestUtil.getUserNickname(), metaObject);
    }
}

