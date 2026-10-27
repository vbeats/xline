# AGENTS.md

xline：Maven 多模块、节点式单向数据流框架，一套 Java 8 语法源码同时支持 JDK 8（SpringBoot 2.7）与 JDK 21（SpringBoot 4.1）。
本文件只写"动手前必须知道"的东西；模块用法见 [README.md](README.md)，架构图见 [doc/xline-architecture.html](doc/xline-architecture.html)。

## 命令

```bash
mvn clean install -DskipTests                  # 默认按当前 JDK 选分支
mvn -P '!jdk21,jdk8' clean install -DskipTests # 强制 jdk8 分支（两 profile 互斥，不能只写 -Pjdk8）
mvn -pl xline-http -am clean install -DskipTests   # 单模块改动
java -jar xline-app/target/xline-app-1.0.0.jar     # 运行，端口 10010
```

- 仓库**没有任何测试**（无 `src/test`）。改动后必须：双分支构建 + 启动一次看日志（`reg success` / `Started XlineApp` / 无 ERROR）。
- 版本 `1.0.0` 来自根 POM 的 `<revision>`，改版本只改这一处。
- 不使用 Maven Wrapper，不生成 `mvnw`。

## 架构速查

| 层 | 关键类 | 职责 |
| --- | --- | --- |
| core | `Node` / `INode` | 配置绑定载体 + 生命周期模板（`init` / `destroy` / `receiveData`） |
| core | `AbstractNodeAutoConfiguration<N,H>` | 装配模板：筛选 → 拷贝 → 匹配处理器 → `beforeInit` → 初始化 → 登记 |
| core | `XLineContext` | `NODES` / `DATA_SOURCES` 注册表、`pushMsg` |
| core | `XLineEventHandler` | Disruptor 消费者，按 `struct` 投递下游 |
| core | `XLineRoutingDataSource` | 实现 `javax.sql.DataSource`，按 `NodeContext` 路由 |
| 节点模块 | `XxxNode` + `XxxNodeAutoConfiguration` + `XxxDataHandler` | 只声明 `nodeType()` / `nodeClass()` / `handlerClass()` |
| app | `XlineApp` | 仅启动类，无 Controller、无 Web 配置 |

数据流单向：`pushMsg(上游, data)` → 事件总线 → `struct` 下游 → `receiveData` → `DataHandler.rec`。
节点在 `ApplicationReadyEvent` 初始化（晚于 `CommandLineRunner`）。

## 红线

1. **Java 8 语法**：`src/main/java` 与 `src/main/java-jdk8` 禁用 `var`、`record`、文本块、switch 表达式、`List.of/Map.of`、`Stream.toList()`、`instanceof` 模式匹配。
2. **core 不含数据库依赖**：Druid、anyline、Flyway、spring-jdbc、ORM 只能出现在具体节点模块；core 只用 `javax.sql.DataSource`。
3. **基础依赖只在 core 声明**：Netty、Hutool、Fastjson2、Guava、Disruptor；版本统一在根 POM `dependencyManagement`，子模块不写 `<version>`。
4. **版本占位符**：子模块 `parent.version` 与模块间依赖一律 `${revision}`，靠 `flatten-maven-plugin` 在 install 时展开（删掉它会让下游解析失败）。
5. **配置段 setter 必须判空**：
   ```java
   public void setHttp(HttpProperties http) {
       this.http = http;
       if (http != null) { this.type = bindType(NodeType.HTTP); }   // 漏判空 → 类型被最后一个段覆盖
   }
   ```
   `BeanUtil.copyProperties` 会以 `null` 调用未配置段的 setter。
6. **不创建** `META-INF/spring.factories` 与 `AutoConfiguration.imports`：全部走组件扫描 + `@ConfigurationPropertiesScan`。
7. **`application.yml` 必须保留** `spring.flyway.enabled: false`，否则 jdk8 分支启动即失败（Flyway 抢在节点就绪前取路由数据源连接）。
8. **分支差异靠属性，不靠代码分支**：`${web.starter.artifactId}`、`${mybatis-plus.starter.artifactId}`、`${forest.starter.artifactId}` 由 profile 切换。
9. 构造器注入；不直接用 `ObjectMapper`（Jackson 2 仅为 anyline 运行期）；不引入 JPA / Bean Validation 等命名空间迁移件。

## 新增一种节点类型

1. core：`NodeType` 加枚举项 + `com.csf.xline.core.<类型>.XxxProperties`，并在 `Node` 加字段与判空 setter；
2. 新建模块：只引入该类型需要的依赖（数据库模块才引驱动、Druid、Flyway、anyline）；
3. `XxxNode extends Node`：只实现 `initDataSource()` / `destroyDataSource()`；
4. `XxxNodeAutoConfiguration extends AbstractNodeAutoConfiguration<XxxNode, XxxDataHandler>`：三个类型方法；需要运行期依赖时覆写 `beforeInit()`（参考 tcp 注入线程工厂/上下文、http 注入全局 ForestConfiguration）；
5. `XxxDataHandler extends DataHandler<XxxNode>`：`init` / `rec` / `destroy` 三个方法都要实现（无默认实现）；
6. 根 POM 的 `<modules>`、`dependencyManagement` 同步，`xline-app` 按需引入。

同类多模块（如 SQL Server 的 jTDS / mssql-jdbc）处理同一 `NodeType` 时，装配模板会跳过已登记的同名节点并告警，不会重复建连。

## 资源与目录

| 位置 | 内容 |
| --- | --- |
| `src/main/java` / `java-jdk8` / `java-jdk21` | 公共 / 分支专属源码，同一时刻只编译一个专属目录 |
| `xline-app/src/main/resources/application.yml` | 节点与 `struct` 配置（必含 `spring.flyway.enabled: false`） |
| `xline-app/src/main/resources/logback-xline.xml` | 日志配置；`<conversionRule>` 用 `converterClass`，写成 `class` 会导致启动失败 |
| `classpath:<数据库类型>/<节点名>` | Flyway 脚本目录（仅 `flyway: true` 的节点执行） |
| `doc/` | 架构图产物 |
| `.archify/` | 架构图源 JSON（已 gitignore） |

## 已知环境坑

- **Windows + archify**：`finalize` / `deliver` 的 sidecar 写入会误判硬链接（`links: 2`）而失败；改用 `render` 出产物，再单独 `check` / `browser-check`。
- **架构图源码链接**：仓库无 commit 时无法 pin `revision`，`meta.repository` 与组件 `sources` 必须留空。
- **IDE**：`src/main/java-jdk21` 需手动标记 Sources Root（不影响命令行构建）。
