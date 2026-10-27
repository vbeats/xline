package com.csf.xline.db.sqlserver.jtds.handler;

import com.csf.xline.core.handler.DataHandler;
import com.csf.xline.db.sqlserver.jtds.JtdsNode;

/**
 * SQL Server（jTDS）节点数据处理契约。
 *
 * <p>由业务侧实现并注册为 Spring Bean：
 * 容器中有多个实现时按 Bean 名与节点名匹配，只有一个实现时所有节点共用。
 */
public interface JtdsDataHandler extends DataHandler<JtdsNode> {
}
