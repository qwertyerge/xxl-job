# PRD: xxl-job Mapper TestContainers 集成测试

## Introduction

为 xxl-job-admin 的全部 8 个 MyBatis Mapper 引入 TestContainers + MySQL 集成测试，替代现有依赖外部 MySQL 实例的测试。现有 6 个测试无断言、无数据隔离、缺少 2 个 Mapper 测试。本次重写全部 8 个测试，采用完整断言 + `@Transactional` 回滚 + `@MybatisTest` 切片测试。

## Goals

- 全部 8 个 Mapper 拥有独立的集成测试，使用 TestContainers MySQL 容器
- 每个 Mapper 方法至少一个测试用例，关键路径有边界测试
- 使用 AssertJ 完整断言，验证返回值和数据状态
- `@Transactional` 自动回滚，测试间完全隔离
- `@MybatisTest` 切片加载，避免启动完整 Spring Boot 上下文

## User Stories

### US-001: 添加 TestContainers 依赖和基础设施
**Description:** 作为开发者，我需要 TestContainers 基础设施，以便所有 Mapper 测试共享一个 MySQL 容器。

**Acceptance Criteria:**
- [ ] 根 `pom.xml` 的 `dependencyManagement` 添加 `testcontainers-bom`
- [ ] `xxl-job-admin/pom.xml` 添加 `testcontainers:junit-jupiter`、`testcontainers:mysql`、`mybatis-spring-boot-starter-test` 依赖
- [ ] 创建 `src/test/resources/db/tables_xxl_job.sql`（从 `doc/db/` 复制，移除末尾 `commit;`）
- [ ] 创建 `MySQLContainerHolder.java`（Singleton 容器，`mysql:8.4` 镜像）
- [ ] 创建 `MapperITBase.java`（抽象基类，`@MybatisTest` + `@DynamicPropertySource` + `@Transactional`）
- [ ] 创建 `TestFixtures.java`（测试数据工厂）
- [ ] 编译通过

### US-002: XxlJobGroupMapper 集成测试
**Description:** 作为开发者，我需要验证 XxlJobGroupMapper 的全部 8 个方法正确执行 SQL。

**Acceptance Criteria:**
- [ ] 创建 `XxlJobGroupMapperIT.java`，继承 `MapperITBase`
- [ ] 测试 save + load 往返、update、remove、findAll、findByAddressType、pageList/pageListCount
- [ ] 每个测试方法独立，使用 AssertJ 断言
- [ ] 所有测试通过

### US-003: XxlJobInfoMapper 集成测试
**Description:** 作为开发者，我需要验证 XxlJobInfoMapper 的全部 10 个方法，特别是调度相关的 scheduleJobQuery 和 scheduleUpdate。

**Acceptance Criteria:**
- [ ] 创建 `XxlJobInfoMapperIT.java`
- [ ] 测试 CRUD、分页、按 group 查询、findAllCount
- [ ] 测试 scheduleJobQuery 只返回 triggerStatus=1 且 triggerNextTime 满足条件的任务
- [ ] 测试 scheduleUpdate 的 WHERE trigger_status=1 守卫条件
- [ ] 所有测试通过

### US-004: XxlJobLockMapper 集成测试（新增）
**Description:** 作为开发者，我需要验证 scheduleLock() 的 SELECT ... FOR UPDATE 悲观锁行为。

**Acceptance Criteria:**
- [ ] 创建 `XxlJobLockMapperIT.java`
- [ ] 测试基本调用返回 lock_name
- [ ] 测试并发场景：线程 A 持锁时线程 B 超时（使用 `Propagation.NOT_SUPPORTED` + `TransactionTemplate`）
- [ ] 所有测试通过

### US-005: XxlJobLogGlueMapper 集成测试
**Description:** 作为开发者，我需要验证 GLUE 代码版本管理的 4 个方法。

**Acceptance Criteria:**
- [ ] 创建 `XxlJobLogGlueMapperIT.java`
- [ ] 测试 save + findByJobId 往返（验证 DESC 排序）
- [ ] 测试 removeOld 保留最新 N 条记录
- [ ] 测试 deleteByJobId 清除全部
- [ ] 所有测试通过

### US-006: XxlJobLogMapper 集成测试
**Description:** 作为开发者，我需要验证任务日志管理的全部 13 个方法，包括复杂的报表统计和丢失任务检测。

**Acceptance Criteria:**
- [ ] 创建 `XxlJobLogMapperIT.java`
- [ ] 测试 CRUD、分页、updateTriggerInfo、updateHandleInfo
- [ ] 测试 findLogReport 日期范围统计
- [ ] 测试 findClearLogIds + clearLog 日志清理
- [ ] 测试 findFailJobLogIds 只返回失败且未告警的日志
- [ ] 测试 updateAlarmStatus 乐观锁（old→new 状态检查）
- [ ] 测试 findLostJobIds（需构造 registry 数据验证 LEFT JOIN）
- [ ] 所有测试通过

### US-007: XxlJobLogReportMapper 集成测试
**Description:** 作为开发者，我需要验证日志报表的 ON DUPLICATE KEY UPDATE 和统计查询。

**Acceptance Criteria:**
- [ ] 创建 `XxlJobLogReportMapperIT.java`
- [ ] 测试 saveOrUpdate 插入新记录
- [ ] 测试 saveOrUpdate 对相同 trigger_day 执行更新
- [ ] 测试 queryLogReport 日期范围过滤
- [ ] 测试 queryLogReportTotal 汇总统计
- [ ] 所有测试通过

### US-008: XxlJobRegistryMapper 集成测试
**Description:** 作为开发者，我需要验证执行器注册管理的 6 个方法，包括心跳超时检测。

**Acceptance Criteria:**
- [ ] 创建 `XxlJobRegistryMapperIT.java`
- [ ] 测试 registrySaveOrUpdate 插入和更新路径
- [ ] 测试 findDead 超时检测（构造过期 updateTime）
- [ ] 测试 findAll 只返回存活注册
- [ ] 测试 removeDead 批量删除
- [ ] 测试 registryDelete 和 removeByRegistryGroupAndKey
- [ ] 所有测试通过

### US-009: XxlJobUserMapper 集成测试（新增）
**Description:** 作为开发者，我需要验证用户管理的全部 8 个方法，包括条件密码更新。

**Acceptance Criteria:**
- [ ] 创建 `XxlJobUserMapperIT.java`
- [ ] 测试 save + loadById/loadByUserName 往返
- [ ] 测试 update 当 password 为 null 时不更新密码
- [ ] 测试 delete、updateToken
- [ ] 测试 pageList/pageListCount 的 username 模糊搜索和 role 过滤
- [ ] 所有测试通过

## Functional Requirements

- FR-1: 根 POM `dependencyManagement` 添加 `testcontainers-bom:1.20.4`
- FR-2: admin POM 添加 `testcontainers:junit-jupiter`、`testcontainers:mysql`、`mybatis-spring-boot-starter-test:4.0.1`（scope=test）
- FR-3: 创建 Singleton MySQL 容器 `MySQLContainerHolder`，使用 `mysql:8.4` 镜像，`withInitScript` 加载建表 SQL
- FR-4: 创建抽象基类 `MapperITBase`，使用 `@MybatisTest` + `@AutoConfigureTestDatabase(replace=NONE)` + `@Transactional` + `@DynamicPropertySource`
- FR-5: 创建 `TestFixtures` 工厂类，为每个 Model 提供最小有效对象构造方法
- FR-6: 8 个 Mapper 各创建独立的 `*MapperIT.java` 测试类
- FR-7: 测试命名使用 `*IT.java` 后缀区分集成测试，保留旧 `*Test.java` 不删除
- FR-8: `XxlJobLockMapperIT` 的并发测试使用 `Propagation.NOT_SUPPORTED` 挂起外层事务

## Non-Goals

- 不删除现有的 6 个 `*MapperTest.java` 文件
- 不修改 `maven.test.skip=true`（用户可通过 `-Dmaven.test.skip=false` 运行）
- 不修改业务代码或 Mapper XML
- 不添加 Service 层测试
- 不引入 mock 框架

## Technical Considerations

- **Spring Boot 4.0.1 + MyBatis 4.0.1**: `@MybatisTest` 来自 `mybatis-spring-boot-starter-test`，需确认与 Spring Boot 4.x 兼容
- **SQL 脚本种子数据**: 初始化脚本插入了 2 个 group、3 个 job、1 个 user、1 个 lock 行，测试断言需考虑这些预置数据
- **FOR UPDATE 测试**: `scheduleLock()` 在 `@Transactional` 回滚模式下可正常测试基本功能；并发测试需手动管理事务
- **ON DUPLICATE KEY UPDATE**: `saveOrUpdate` 类方法需用相同唯一键值调用两次来测试更新路径
- **`commit;` 移除**: 测试用 SQL 脚本需移除末尾 `commit;`，否则 TestContainers ScriptRunner 可能报错

## Success Metrics

- 全部 8 个 Mapper 的所有方法（共 53 个）均有对应测试覆盖
- 所有测试在 TestContainers MySQL 容器上通过
- 测试间无数据泄漏（@Transactional 回滚验证）
- 测试启动速度：@MybatisTest 切片加载 < 10 秒（不含容器首次启动）

## 文件清单

### 新建文件
| 文件路径 | 说明 |
|---------|------|
| `xxl-job-admin/src/test/resources/db/tables_xxl_job.sql` | 测试用建表脚本（无 commit） |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/test/support/MySQLContainerHolder.java` | Singleton 容器 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/test/support/MapperITBase.java` | 抽象基类 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/test/support/TestFixtures.java` | 测试数据工厂 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/XxlJobGroupMapperIT.java` | Group 测试 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/XxlJobInfoMapperIT.java` | Info 测试 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/XxlJobLockMapperIT.java` | Lock 测试 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/XxlJobLogGlueMapperIT.java` | LogGlue 测试 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/XxlJobLogMapperIT.java` | Log 测试 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/XxlJobLogReportMapperIT.java` | LogReport 测试 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/XxlJobRegistryMapperIT.java` | Registry 测试 |
| `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/XxlJobUserMapperIT.java` | User 测试 |

### 修改文件
| 文件路径 | 修改内容 |
|---------|---------|
| `pom.xml`（根） | `dependencyManagement` 添加 `testcontainers-bom` |
| `xxl-job-admin/pom.xml` | `dependencies` 添加 3 个 test scope 依赖 |

## 实施顺序

1. **Phase 1**: 修改 POM 依赖 → 创建测试 SQL 脚本 → 创建基础设施（MySQLContainerHolder、MapperITBase、TestFixtures）
2. **Phase 2**: 逐个创建 8 个 MapperIT 测试类（从简单到复杂：Group → User → LogGlue → LogReport → Registry → Info → Log → Lock）
3. **Phase 3**: 运行全部测试验证通过

## Open Questions

- `mybatis-spring-boot-starter-test:4.0.1` 是否已正式支持 Spring Boot 4.x？需在实施时验证
- 是否需要为 `findLostJobIds` 测试额外注入 `XxlJobRegistryMapper`，还是用 `JdbcTemplate` 直接插入 registry 数据？
