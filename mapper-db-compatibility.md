# Mapper XML 数据库兼容性对比报告

> MySQL vs PostgreSQL — xxl-job-admin/src/main/resources/mapper/

## 差异类型总览

| 差异类型 | MySQL | PostgreSQL | 出现次数 |
|---------|-------|-----------|---------|
| 列名引号 | `` `col` `` | `col` | 30+ |
| 字符串拼接 | `CONCAT(CONCAT('%',x),'%')` | `'%' \|\| x \|\| '%'` | 10+ |
| 分页语法 | `LIMIT offset, size` | `LIMIT size OFFSET offset` | 6 |
| UPSERT | `ON DUPLICATE KEY UPDATE` | `ON CONFLICT (...) DO UPDATE SET` | 2 |
| 日期运算 | `DATE_ADD(t, INTERVAL -n SECOND)` | `CAST(t AS TIMESTAMP) - n * INTERVAL '1 SECOND'` | 2 |
| NULL 函数 | `IFNULL(v, 0)` | `COALESCE(v, 0)` | 1 |
| NOT 运算符 | `!(...)` | `NOT (...)` | 1 |
| 语句结尾分号 | 有 `;` | 无 | 5+ |
| 关键字大小写 | 混合 | 倾向大写 | 多处 |
| FOR UPDATE 锁 | 相同 | 相同 | 1 |

---

## 逐文件对比

### 1. XxlJobGroupMapper.xml

| Statement | 差异 |
|-----------|------|
| `findAll` | 无 |
| `findByAddressType` | 无 |
| `save` | 列名引号, 分号 |
| `update` | 列名引号 |
| `remove` | 无 |
| `load` | 无 |
| `pageList` | 字符串拼接, 分页语法 |
| `pageListCount` | 字符串拼接 |

#### save
```diff
-INSERT INTO xxl_job_group ( `app_name`, `title`, `address_type`, `address_list`, `update_time`)
-values ( #{appname}, #{title}, #{addressType}, #{addressList}, #{updateTime} );
+INSERT INTO xxl_job_group (app_name, title, address_type, address_list, update_time)
+VALUES (#{appname}, #{title}, #{addressType}, #{addressList}, #{updateTime})
```

#### update
```diff
-SET `app_name` = #{appname},
-    `title` = #{title},
-    `address_type` = #{addressType},
-    `address_list` = #{addressList},
-    `update_time` = #{updateTime}
+SET app_name = #{appname},
+    title = #{title},
+    address_type = #{addressType},
+    address_list = #{addressList},
+    update_time = #{updateTime}
```

#### pageList
```diff
-AND t.app_name like CONCAT(CONCAT('%', #{appname}), '%')
-AND t.title like CONCAT(CONCAT('%', #{title}), '%')
-LIMIT #{offset}, #{pagesize}
+AND t.app_name LIKE '%' || #{appname} || '%'
+AND t.title LIKE '%' || #{title} || '%'
+LIMIT #{pagesize} OFFSET #{offset}
```

---

### 2. XxlJobInfoMapper.xml

| Statement | 差异 |
|-----------|------|
| `pageList` | 字符串拼接, 分页语法 |
| `pageListCount` | 字符串拼接 |
| `save` | 分号, 注释块 |
| `loadById` | 无 |
| `update` | 无 |
| `delete` | 无 |
| `getJobsByGroup` | 无 |
| `findAllCount` | 无 |
| `scheduleJobQuery` | 无 |
| `scheduleUpdate` | 无 |

#### pageList
```diff
-AND t.job_desc like CONCAT(CONCAT('%', #{jobDesc}), '%')
-AND t.executor_handler like CONCAT(CONCAT('%', #{executorHandler}), '%')
-AND t.author like CONCAT(CONCAT('%', #{author}), '%')
-LIMIT #{offset}, #{pagesize}
+AND t.job_desc LIKE '%' || #{jobDesc} || '%'
+AND t.executor_handler LIKE '%' || #{executorHandler} || '%'
+AND t.author LIKE '%' || #{author} || '%'
+LIMIT #{pagesize} OFFSET #{offset}
```

---

### 3. XxlJobLockMapper.xml

**完全相同** — `SELECT ... FOR UPDATE` 两者语法一致。

---

### 4. XxlJobLogGlueMapper.xml

| Statement | 差异 |
|-----------|------|
| `save` | 列名引号, 分号 |
| `findByJobId` | 无 |
| `removeOld` | 列名引号, 分页语法 |
| `deleteByJobId` | 列名引号 |

#### removeOld
```diff
 DELETE FROM xxl_job_logglue
-WHERE id NOT in(
+WHERE id NOT IN(
     SELECT id FROM(
         SELECT id FROM xxl_job_logglue
-        WHERE `job_id` = #{jobId}
-        ORDER BY update_time desc
-        LIMIT 0, #{limit}
+        WHERE job_id = #{jobId}
+        ORDER BY update_time DESC
+        LIMIT #{limit}
     ) t1
-) AND `job_id` = #{jobId}
+) AND job_id = #{jobId}
```

---

### 5. XxlJobLogMapper.xml（差异最多）

| Statement | 差异 |
|-----------|------|
| `pageList` | 分页语法 |
| `pageListCount` | 无 |
| `load` | 无 |
| `save` | 列名引号, 分号 |
| `updateTriggerInfo` | 列名引号 |
| `updateHandleInfo` | 列名引号 |
| `delete` | 关键字大小写 |
| `findLogReport` | **IFNULL → COALESCE** |
| `findClearLogIds` | 分页语法, 大小写 |
| `clearLog` | 大小写 |
| `findFailJobLogIds` | 列名引号, **! → NOT** |
| `updateAlarmStatus` | 列名引号 |
| `findLostJobIds` | 分号 |

#### findLogReport — NULL 处理函数
```diff
 SELECT
-    IFNULL(COUNT(handle_code),0) triggerDayCount,
-    IFNULL(SUM(CASE WHEN (trigger_code in (0, 200) and handle_code = 0) then 1 else 0 end),0) as triggerDayCountRunning,
-    IFNULL(SUM(CASE WHEN handle_code = 200 then 1 else 0 end),0) as triggerDayCountSuc
+    COALESCE(COUNT(handle_code), 0) triggerDayCount,
+    COALESCE(SUM(CASE WHEN (trigger_code in (0, 200) and handle_code = 0) then 1 else 0 end), 0) as triggerDayCountRunning,
+    COALESCE(SUM(CASE WHEN handle_code = 200 then 1 else 0 end), 0) as triggerDayCountSuc
```

#### findFailJobLogIds — NOT 运算符
```diff
-SELECT id FROM `xxl_job_log`
-WHERE !(
+SELECT id FROM xxl_job_log
+WHERE NOT (
     (trigger_code in (0, 200) and handle_code = 0)
     OR
     (handle_code = 200)
 )
-AND `alarm_status` = 0
+AND alarm_status = 0
```

---

### 6. XxlJobLogReportMapper.xml

| Statement | 差异 |
|-----------|------|
| `saveOrUpdate` | 列名引号, **UPSERT 语法** |
| `queryLogReport` | 无 |
| `queryLogReportTotal` | 无 |

#### saveOrUpdate — UPSERT 语法
```diff
 INSERT INTO xxl_job_log_report (
-    `trigger_day`, `running_count`, `suc_count`, `fail_count`
+    trigger_day, running_count, suc_count, fail_count
 ) VALUES (
      #{triggerDay}, #{runningCount}, #{sucCount}, #{failCount}
  )
-ON DUPLICATE KEY UPDATE
-    `running_count` = #{runningCount},
-    `suc_count` = #{sucCount},
-    `fail_count` = #{failCount}
+ON CONFLICT (trigger_day) DO UPDATE SET
+    running_count = #{runningCount},
+    suc_count = #{sucCount},
+    fail_count = #{failCount}
```

---

### 7. XxlJobRegistryMapper.xml

| Statement | 差异 |
|-----------|------|
| `findDead` | **日期运算** |
| `removeDead` | 无 |
| `findAll` | **日期运算** |
| `registrySaveOrUpdate` | 列名引号, **UPSERT 语法** |
| `registryDelete` | 无 |
| `removeByRegistryGroupAndKey` | 无 |

#### findDead / findAll — 日期运算
```diff
-WHERE t.update_time < DATE_ADD(#{nowTime},INTERVAL -#{timeout} SECOND)
+WHERE t.update_time < (CAST(#{nowTime} AS TIMESTAMP) - (CAST(#{timeout} AS INTEGER) * INTERVAL '1 SECOND'))
```

#### registrySaveOrUpdate — UPSERT 语法
```diff
-INSERT INTO xxl_job_registry( `registry_group` , `registry_key` , `registry_value`, `update_time`)
-VALUES( #{registryGroup}  , #{registryKey} , #{registryValue}, #{updateTime})
-ON DUPLICATE KEY UPDATE
-    `update_time` = #{updateTime}
+INSERT INTO xxl_job_registry(registry_group, registry_key, registry_value, update_time)
+VALUES(#{registryGroup}, #{registryKey}, #{registryValue}, #{updateTime})
+ON CONFLICT (registry_group, registry_key, registry_value) DO UPDATE SET
+    update_time = #{updateTime}
```

---

### 8. XxlJobUserMapper.xml

| Statement | 差异 |
|-----------|------|
| `pageList` | 字符串拼接, 分页语法 |
| `pageListCount` | 字符串拼接 |
| `loadByUserName` | 无 |
| `loadById` | 无 |
| `save` | 分号 |
| `update` | 无 |
| `delete` | 无 |
| `updateToken` | 无 |

#### pageList
```diff
-AND t.username like CONCAT(CONCAT('%', #{username}), '%')
-LIMIT #{offset}, #{pagesize}
+AND t.username like '%' || #{username} || '%'
+LIMIT #{pagesize} OFFSET #{offset}
```

---

## 差异分析

### 语法层差异（机械转换，无语义影响）

- **列名引号**: MySQL 反引号是保留字转义习惯，PostgreSQL 用双引号（此处直接去掉即可）
- **分页语法**: 参数位置互换，语义等价
- **字符串拼接**: `CONCAT` vs `||`，语义等价
- **关键字大小写 / 分号**: 风格差异，无功能影响

### 功能层差异（需理解语义）

- **UPSERT**: MySQL 的 `ON DUPLICATE KEY UPDATE` 依赖唯一索引自动匹配；PostgreSQL 的 `ON CONFLICT` 需显式指定冲突列，语义更精确
- **日期运算**: PostgreSQL 需要显式 `CAST` 确保类型匹配，MySQL 的 `DATE_ADD` 隐式处理
- **IFNULL vs COALESCE**: `COALESCE` 是 SQL 标准函数，支持多参数；`IFNULL` 是 MySQL 专有
- **! vs NOT**: `!` 是 MySQL 非标准语法，`NOT` 是 SQL 标准

### 完全兼容的 SQL

`SELECT ... FOR UPDATE`、基础 CRUD（无引号/拼接的）、`BETWEEN`、`CASE WHEN`、`LEFT JOIN`、`IN` 子句等标准 SQL 在两者间完全一致。
