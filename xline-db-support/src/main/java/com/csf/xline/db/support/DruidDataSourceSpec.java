package com.csf.xline.db.support;

import com.alibaba.druid.DbType;
import com.csf.xline.core.enums.NodeType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.anyline.metadata.type.DatabaseType;

/**
 * Druid 数据源的数据库差异参数。
 *
 * <p>连接池、注册、迁移与销毁由公共支撑统一处理；各驱动模块只提供连接地址、
 * 驱动类和数据库类型等差异信息。
 */
@Getter
@Setter
@Accessors(chain = true)
public class DruidDataSourceSpec {

    private final String nodeName;

    private final NodeType nodeType;

    private final DbType druidType;

    private final DatabaseType databaseType;

    private String jdbcUrl;

    private String driverClassName;

    private String username;

    @ToString.Exclude
    private String password;

    private String logName;

    public DruidDataSourceSpec(String nodeName, NodeType nodeType, DbType druidType,
                               DatabaseType databaseType) {
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.druidType = druidType;
        this.databaseType = databaseType;
    }
}
