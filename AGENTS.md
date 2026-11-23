# AGENTS.md

xline 是 Maven 多模块、节点式单向数据流框架；同一套 Java 8 语法源码同时支持 JDK 8（Spring Boot 2.7）与 JDK 21（Spring Boot 4.1）。模块用法见 [README.md](README.md)，架构图见 [doc/xline-architecture.png](doc/xline-architecture.png) 与 [交互版](doc/xline-architecture.html)。

## 验证命令

```bash
mvn clean install -DskipTests
mvn -P '!jdk21,jdk8' clean install -DskipTests
java -jar target/xline.jar --spring.config.additional-location=file:./demo/h2-test.yml
```

- 仓库没有 `src/test`；改动后必须完成双分支构建，并至少启动一次，确认出现 `reg success` / `Started XlineApp` 且无 `ERROR`。
- `xline-app` 是非 Web 应用，不提供页面或 HTTP 接口；产物固定为根目录 `target/xline.jar`，切换分支会覆盖它。
- 版本只改根 POM 的 `<revision>`；不用 Maven Wrapper，不生成 `mvnw`。

## 核心模型

| 层 | 关键类 | 职责 |
| --- | --- | --- |
| core | `Node` / `INode` | 配置绑定、生命周期与 `pushMsg` |
| core | `AbstractNodeAutoConfiguration<N,H>` | 筛选、复制、匹配处理器、初始化、登记 |
| core | `XLineContext` | 节点/数据源注册表与底层按名路由 API |
| core | `XLineEventHandler` / `XLineDispatcher` | Disruptor 路由、节点队列和异步执行 |
| core | `XLineRoutingDataSource` | 按 `NodeContext` 路由 `javax.sql.DataSource` |
| 节点模块 | `XxxNode` / `XxxNodeAutoConfiguration` / `XxxDataHandler` | 类型资源、装配声明与数据处理 |
| app | `XlineApp` | 非 Web 启动与运行验证 |

- 业务和链路内部统一调用 `node.pushMsg(data)`；`XLineContext.pushMsg(节点名, data)` 只用于底层按 `struct` 注入。
- 节点在 `ApplicationReadyEvent` 初始化，晚于 `CommandLineRunner`。
- 总线消费线程和工作线程不得阻塞等待。反压只允许外部生产者等待；JDK 21 的阻塞 IO 外避免 `synchronized`，使用 `ReentrantLock`。

## 强制约束

1. 公共源码和 `java-jdk8` 仅用 Java 8 语法/API：禁用 `var`、`record`、文本块、switch 表达式、`List.of/Map.of`、`Stream.toList()`、模式匹配。
2. core 不引入数据库实现；Druid、AnyLine、Flyway、spring-jdbc、ORM 只放具体数据库模块。Netty、Hutool、Fastjson2、Guava、Disruptor 只由 core 声明。
3. 第三方版本统一放根 POM `dependencyManagement`；子模块 parent 与模块间依赖使用 `${revision}`，保留 `flatten-maven-plugin`。
4. 类型配置段 setter 必须判空后再绑定类型；`BeanUtil.copyProperties` 会以 `null` 调用未配置段 setter。
5. 不创建 `META-INF/spring.factories` 或 `AutoConfiguration.imports`；使用组件扫描和 `@ConfigurationPropertiesScan`。
6. `application.yml` 必须保留 `spring.flyway.enabled: false`；迁移由数据库节点按自身 `flyway` 开关执行。
7. 分支差异用 profile 属性解决；Forest 两分支统一使用 `forest-spring-boot-starter`。
8. 使用构造器注入；不直接使用 `ObjectMapper`；不引入 JPA、Bean Validation 等命名空间迁移依赖。
9. `Node` 的 `dataHandler` / `context` 必须禁用 Lombok getter/setter，读取走 `dataHandler()` / `context()`，注入走 `attach(...)`，避免暴露为配置项。
10. 类型配置段可省略；节点只在真正使用必填字段时给出明确错误，不能因配置段为 `null` 抛 NPE。
11. 资源归节点所有；初始化失败、未初始化和重复销毁都必须安全。
12. 不使用废弃 API；需要时以 `-Dmaven.compiler.showDeprecation=true` 构建检查。

## 数据库与外部资源

- 接入 Druid 的关系型节点继承 `JdbcProperties`（网络库用 `JdbcServerProperties`），复用连接池、连接属性和 Flyway 配置。
- MQTT 仅 v5，接收以 `MqttEnvelope` 下推，发布走 `node.publish()`；重连回调不能阻塞等待订阅确认。
- Redis 的 `config-location` 由 Redisson 原生读取，不与内联配置合并；资源必须用流读取，不能依赖 `getFile()`。
- H2 接入 Druid / AnyLine / 路由数据源 / 可选 Flyway；DuckDB 只持有独立 JDBC 连接。

## 新增节点类型

1. core 增加 `NodeType`、`XxxProperties`，并在 `Node` 增加字段和判空 setter。
2. 新模块只引入该类型所需依赖，并同步根 POM 的 `<modules>` 与 `dependencyManagement`。
3. `XxxNode` 实现资源初始化/销毁；`XxxDataHandler` 实现 `init` / `rec` / `destroy`。
4. `XxxNodeAutoConfiguration` 声明 `nodeType()` / `nodeClass()` / `handlerClass()`，构造器透传 `(XLineProperties, XLineContext)`；额外运行期依赖放在 `beforeInit()`。
5. `xline-app` 按需引入模块。同一 `NodeType` 的多个实现会跳过已登记同名节点并告警。

## 目录约定

| 位置 | 内容 |
| --- | --- |
| `src/main/java` / `java-jdk8` / `java-jdk21` | 公共与分支专属源码 |
| `xline-app/src/main/resources/application.yml` | 框架配置；不放节点 |
| `demo/*.yml` | 节点与 `struct` 示例，通过 `--spring.config.additional-location` 叠加 |
| `classpath:<数据库类型>/<节点名>` | 启用 Flyway 时的脚本目录 |
| `.archify/` / `doc/` | 架构图源与导出产物 |
