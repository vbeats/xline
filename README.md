# xline

节点式拓扑数据流框架：数据按 `xline.struct` 定义的拓扑**单向传递**（上一层推给下一层），
各节点按配置条件化初始化；同时支持 **JDK 1.8**（SpringBoot 2.7）与 **JDK 21**（SpringBoot 4.1），
由 Maven profile 按运行 JDK 自动切换。

架构图：[doc/xline-architecture.html](doc/xline-architecture.html)（可交互，支持暗/亮主题）

## 模块

| 模块 | 说明 |
| --- | --- |
| `xline-core` | 节点模型、struct 拓扑、事件总线、数据处理契约、配置载体、基础依赖 |
| `xline-db-mysql` | MySQL 节点 |
| `xline-db-postgresql` | PostgreSQL 节点 |
| `xline-db-sqlite` | SQLite 节点 |
| `xline-db-sqlserver-msssql` | SQL Server 节点（mssql-jdbc 驱动） |
| `xline-db-sqlserver-jtds` | SQL Server 节点（jTDS 驱动） |
| `xline-tcp` | TCP 节点（JDK socket） |
| `xline-http` | HTTP 节点（Forest） |
| `xline-app` | 可启动应用（默认端口 10010，不提供任何 Web 接口） |

**不引入某个节点模块，对应类型的节点就永远不会被初始化**；SQL Server 的两个驱动模块二选一引入。

## 构建与运行

```bash
mvn clean install -DskipTests                    # 按当前 JDK 自动选分支
mvn -P '!jdk21,jdk8' clean install -DskipTests   # 强制 jdk8 分支（两个 profile 互斥）
java -jar xline-app/target/xline-app-1.0.0.jar   # 启动，默认端口 10010
```

启动日志示例：

```
Tomcat started on port 10010
【DataSource - SQLite】reg success: local
Started XlineApp in 2.3 seconds
```

## 快速开始

```yaml
xline:
  nodes:
    - name: local
      sqlite:                 # 书写哪个配置段，节点类型即推断为该类型
        db-path: ":memory:"
    - name: archive
      sqlite:
        db-path: ":memory:"
  struct:                     # key 为上游节点，value 为下游节点列表
    local:
      - archive
```

```java
@Component("archive")          // Bean 名与节点名一致
public class ArchiveHandler implements SqliteDataHandler {
    @Override public void init(SqliteNode node) { }
    @Override public void rec(SqliteNode node, Object data) { }   // 收到上游数据
    @Override public void destroy(SqliteNode node) { }
}
```

推送：注入 `XLineContext` 后调用 `pushMsg("local", data)` → 事件总线解析 `struct` → `archive.receiveData(data)`。
容器中有多个处理器实现时按 Bean 名与节点名匹配，只有一个实现时所有节点共用。

## 数据库节点

配置字段：`host`、`port`、`database-name`、`username`、`password`、`driver-class-name`、`jdbc-url`、`flyway`。
配置 `jdbc-url` 后忽略自动拼装；`flyway: true` 时按 `classpath:<数据库类型>/<节点名>` 执行迁移。

| 数据库 | 默认端口 | 配置段 | 业务契约 |
| --- | --- | --- | --- |
| MySQL | 3306 | `mysql` | `MysqlDataHandler` |
| PostgreSQL | 5432 | `postgresql` | `PostgresDataHandler` |
| SQLite | — | `sqlite`（字段 `db-path`，支持 `:memory:`） | `SqliteDataHandler` |
| SQL Server（mssql-jdbc） | 1433 | `mssql` | `MssqlDataHandler` |
| SQL Server（jTDS） | 1433 | `mssql` | `JtdsDataHandler` |

SQL Server 额外支持 `instance`（实例名）、`domain`（域认证）、`encrypt`、`trust-server-certificate`。
两个驱动模块的差异仅在 URL 拼接：

| 模块 | URL 形态 |
| --- | --- |
| `xline-db-sqlserver-msssql` | `jdbc:sqlserver://host:1433;databaseName=db;encrypt=false;...` |
| `xline-db-sqlserver-jtds` | `jdbc:jtds:sqlserver://host:1433/db` |

数据源对外有两种用法：

- **anyline**：`ServiceProxy.service(节点名)` 直接取服务；
- **MyBatis-Plus**：注入容器中的路由 `DataSource`，用 `NodeContext.use("节点名")` 指定节点（用完 `clear()`，未指定时路由到首个就绪节点）。

## TCP 节点

```yaml
- name: device
  tcp:
    mode: server          # server 监听 / client 连接，默认 server
    port: 10011           # 必填：server 监听端口，client 对端端口
    host: 127.0.0.1       # client 必填；server 为绑定地址，留空绑定全部
    charset: UTF-8
    append-new-line: true # 发送时追加换行
    reconnect: true       # client 断线重连
```

- **对端 → 下游**：读到一行数据即以本节点为源头推给下游；
- **上游 → 对端**：在 `TcpDataHandler.rec` 中调用 `node.send(data)`（server 模式广播给全部在线连接）。

## HTTP 节点

请求由 Forest 实现；`base-url` 必填，节点未配置的项沿用 `forest.*` 全局配置。

```yaml
forest:
  connect-timeout: 3000
  max-connections: 1000

xline:
  nodes:
    - name: api
      http:
        base-url: http://192.168.1.10:8080
        read-timeout: 8888     # 覆盖全局的 read-timeout
```

```java
public interface UserApi {
    @Request(url = "/user/list", type = "GET")
    String list();
}

// 处理器内
String result = node.client(UserApi.class).list();   // 绑定本节点配置
String url = node.url("/user/list");                 // 仅拼接 base-url + path
```

响应非 2xx 时 Forest 抛 `ForestNetworkException`，可在接口上用 `@SuccessWhen` 自定义成功条件。

## 配置项一览

| 配置 | 说明 |
| --- | --- |
| `xline.nodes[].name` | 节点名，用于 `struct`、处理器匹配与数据源注册 |
| `xline.nodes[].<类型段>` | 可选 `mysql` / `postgresql` / `sqlite` / `mssql` / `tcp` / `http`，重复配置不同类型会报错 |
| `xline.struct` | 单向拓扑：key 上游，value 下游列表 |
| `forest.*` | Forest 全局配置（引入 `xline-http` 时生效） |
| `spring.flyway.enabled` | 必须为 `false`：迁移由节点按 `flyway` 开关执行 |

开发约定、扩展步骤与双分支机制见 [AGENTS.md](AGENTS.md)。
