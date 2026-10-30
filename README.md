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
| `xline-serial-port` | 串口节点（jSerialComm，232 / 485） |
| `xline-app` | 可启动应用（默认端口 10010，不提供任何 Web 接口） |

**不引入某个节点模块，对应类型的节点就永远不会被初始化**；SQL Server 的两个驱动模块二选一引入。

## 构建与运行

```bash
mvn clean install -DskipTests                    # 按当前 JDK 自动选分支
mvn -P '!jdk21,jdk8' clean install -DskipTests   # 强制 jdk8 分支（两个 profile 互斥）
java -jar target/xline.jar     # 启动，默认端口 10010
```

运行包固定输出到**工程根目录**的 `target/xline.jar`（`xline-app` 的构建目录被覆写，模块自身不再生成 `target`），
文件名不带版本号；其余模块仍各自输出到模块目录下的 `target`。

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

推送：业务处理器拿到的就是节点自身，直接 `node.pushMsg(data)` 即以本节点为出发节点投递
（`pushMsg` 是 `INode` 契约方法，出发节点名由节点自己提供，**不需要传节点名**）→
事件总线解析 `struct` → `archive.receiveData(data)`。
`XLineContext.pushMsg(节点名, data)` 是按名注入事件总线的底层路由 API，业务代码不要直接用。
容器中有多个处理器实现时按 Bean 名与节点名匹配，只有一个实现时所有节点共用。

## 优雅停机

`server.shutdown: graceful` 下，收到停止信号（Ctrl+C / systemd / 服务停止）时按序完成：

1. 等待在途 Web 请求完成；
2. `ContextClosedEvent` 由 `XLineShutdownListener` 统一销毁节点：先回调业务处理器的 `destroy`（数据源仍在，可做收尾写入），再释放节点资源 —— 关闭数据库连接池（Druid）并注销 anyline 数据源、关闭 TCP 连接与线程、释放串口占用并停掉重开线程；
3. Spring 销毁剩余 Bean（含事件总线 Disruptor 的关闭），之后 JVM 退出（Ctrl+C 停机退出码 130）。

大规模资源下销毁按「分组并行」执行：共享同一业务处理器的节点串行销毁（处理器实现可能非线程安全），其余节点并行，并行度取节点组数与 CPU 数的较小值；TCP server 节点的几十万客户端连接也是并行关闭。总时限 30 秒：单个节点销毁卡死不会无限期拖住停机，超时后中断并告警，剩余资源由操作系统回收。

节点不是 Spring Bean，Spring 的销毁回调够不到它们 —— 没有该监听器时，
节点资源（连接池、socket、串口）只能靠进程死亡由操作系统回收，对端会看到连接骤断而非优雅关闭。

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

请求由 Forest 实现；**`http` 配置段整体可选**，未配置的项沿用 `forest.*` 全局配置。

只写 `type: http`、不写 `http` 段即可 —— 此时地址由业务接口自带的 `@Address` / `@Request(url)` 决定：

```yaml
xline:
  nodes:
    - name: api
      type: http              # 不需要任何 http 属性
```

需要节点级地址或覆盖全局超时时才写 `http` 段（写了则以节点值优先）：

```yaml
forest:
  connect-timeout: 3000
  max-connections: 1000

xline:
  nodes:
    - name: api
      type: http
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
String url = node.url("/user/list");                 // 拼接 base-url + path，未配置 http 段时报错提示
```

响应非 2xx 时 Forest 抛 `ForestNetworkException`，可在接口上用 `@SuccessWhen` 自定义成功条件。

## 串口节点

串口既是数据源头（读到字节推向下游），也是数据出口（把上游数据写入串口）。

```yaml
xline:
  nodes:
    - name: c1
      serial-port:
        device: COM1                # Windows 为 COM1，Linux / macOS 为 /dev/ttyS0
        baud-rate: 9600             # 默认 115200
        data-bits: 8                # 默认 8
        stop-bits: 1                # 默认 1
        parity: 0                   # 0 无 / 1 奇 / 2 偶 / 3 MARK / 4 SPACE，默认 0
        use-rs485-mode: false       # 默认 false
        reconnect: true             # 打开失败或串口拔出后自动重开，默认 true
        reconnect-interval-millis: 3000
        charset: UTF-8              # 上游推送文本数据时按此编码写出
    - name: c2
      serial-port:
        device: COM2
  struct:
    c1:                             # 两个方向各配一条边，即成对串口双向转发
      - c2
    c2:
      - c1
```

- **串口 → 下游**：读到字节即以本节点为源头推给下游（数据为 `byte[]`）；
- **上游 → 串口**：有业务处理器时由 `SerialPortDataHandler.rec` 决定；
  **未注册处理器时节点自动把原始字节写入串口** —— 成对串口无需任何业务代码即可双向转发；
- 收发原始数据统一以**大写十六进制**记录（`01AB0C`），避免非 ASCII 字节打印成乱码。

串口不存在、被占用或被拔掉时不阻断启动：记录告警后由后台线程按 `reconnect` 重开。
需要参与业务时注册处理器（Bean 名与节点名一致即可按名匹配）：

```java
@Component("c1")
public class C1Handler implements SerialPortDataHandler {
    @Override public void init(SerialPortNode node) { }
    @Override public void rec(SerialPortNode node, Object data) { node.send(data); }   // 转发到串口
    @Override public void destroy(SerialPortNode node) { }
}
```

## 配置项一览

| 配置 | 说明 |
| --- | --- |
| `xline.nodes[].name` | 节点名，用于 `struct`、处理器匹配与数据源注册 |
| `xline.nodes[].type` | 节点类型（可选）：书写类型配置段时自动推断，也可显式声明；`http` 允许只写类型、不写配置段 |
| `xline.nodes[].<类型段>` | 可选 `mysql` / `postgresql` / `sqlite` / `mssql` / `tcp` / `http` / `serial-port`，重复配置不同类型会报错 |
| `xline.struct` | 单向拓扑：key 上游，value 下游列表 |
| `forest.*` | Forest 全局配置（引入 `xline-http` 时生效） |
| `spring.flyway.enabled` | 必须为 `false`：迁移由节点按 `flyway` 开关执行 |

开发约定、扩展步骤与双分支机制见 [AGENTS.md](AGENTS.md)。
