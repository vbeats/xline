package com.csf.xline.db.sqlserver.jtds;

import com.csf.xline.core.Node;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SQL Server 节点（jTDS 驱动）。
 *
 * <p>数据源的建立与释放委托给 {@link DataSourceInitializer}；
 * 生命周期与数据处理由 {@link Node} 的模板统一完成。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JtdsNode extends Node {

    @Override
    protected void initDataSource() throws Exception {
        DataSourceInitializer.initDataSource(this);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        DataSourceInitializer.destroy(this);
    }
}
