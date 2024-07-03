package com.github.pdaodao.springwebplus.starter.db.dialect.mysql;

import com.github.pdaodao.springwebplus.starter.db.dialect.base.BaseDataTypeConverter;
import com.github.pdaodao.springwebplus.starter.db.pojo.ColumnInfo;
import com.github.pdaodao.springwebplus.starter.db.pojo.DDLBuildContext;
import com.github.pdaodao.springwebplus.starter.db.pojo.FieldTypeName;

public class MysqlDataTypeConverter extends BaseDataTypeConverter {

    @Override
    protected String genDDLFieldAutoIncrement(ColumnInfo tableColumn, FieldTypeName typeWithDefault, DDLBuildContext context) {
        return "AUTO_INCREMENT";
    }
}
