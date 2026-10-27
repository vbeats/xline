package com.csf.xline.db.mysql;

import com.csf.xline.core.Node;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MySQL 节点。
 *
 * <p>数据源的建立与释放委托给 {@link DataSourceInitializer}；
 * 生命周期与数据处理由 {@link Node} 的模板统一完成：
 * 未注册 {@code MysqlDataHandler} 时节点仍可作为纯数据源使用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MysqlNode extends Node {

    @Override
    protected void initDataSource() throws Exception {
        DataSourceInitializer.initDataSource(this);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        DataSourceInitializer.destroy(this);
    }
}
