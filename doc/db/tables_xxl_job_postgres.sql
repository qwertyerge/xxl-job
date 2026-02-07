--
-- XXL-JOB PostgreSQL DDL
-- Copyright (c) 2015-present, xuxueli.
--

-- job group and registry

CREATE TABLE xxl_job_group
(
    id           SERIAL       NOT NULL,
    app_name     varchar(64)  NOT NULL,
    title        varchar(12)  NOT NULL,
    address_type SMALLINT     NOT NULL DEFAULT 0,
    address_list text,
    update_time  TIMESTAMP             DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE xxl_job_registry
(
    id             SERIAL       NOT NULL,
    registry_group varchar(50)  NOT NULL,
    registry_key   varchar(255) NOT NULL,
    registry_value varchar(255) NOT NULL,
    update_time    TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE (registry_group, registry_key, registry_value)
);

-- job info

CREATE TABLE xxl_job_info
(
    id                        SERIAL       NOT NULL,
    job_group                 INTEGER      NOT NULL,
    job_desc                  varchar(255) NOT NULL,
    add_time                  TIMESTAMP             DEFAULT NULL,
    update_time               TIMESTAMP             DEFAULT NULL,
    author                    varchar(64)           DEFAULT NULL,
    alarm_email               varchar(255)          DEFAULT NULL,
    schedule_type             varchar(50)  NOT NULL DEFAULT 'NONE',
    schedule_conf             varchar(128)          DEFAULT NULL,
    misfire_strategy          varchar(50)  NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy   varchar(50)           DEFAULT NULL,
    executor_handler          varchar(255)          DEFAULT NULL,
    executor_param            varchar(512)          DEFAULT NULL,
    executor_block_strategy   varchar(50)           DEFAULT NULL,
    executor_timeout          INTEGER      NOT NULL DEFAULT 0,
    executor_fail_retry_count INTEGER      NOT NULL DEFAULT 0,
    glue_type                 varchar(50)  NOT NULL,
    glue_source               TEXT,
    glue_remark               varchar(128)          DEFAULT NULL,
    glue_updatetime           TIMESTAMP             DEFAULT NULL,
    child_jobid               varchar(255)          DEFAULT NULL,
    trigger_status            SMALLINT     NOT NULL DEFAULT 0,
    trigger_last_time         BIGINT       NOT NULL DEFAULT 0,
    trigger_next_time         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE xxl_job_logglue
(
    id          SERIAL       NOT NULL,
    job_id      INTEGER      NOT NULL,
    glue_type   varchar(50)  DEFAULT NULL,
    glue_source TEXT,
    glue_remark varchar(128) NOT NULL,
    add_time    TIMESTAMP    DEFAULT NULL,
    update_time TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (id)
);

-- job log and report

CREATE TABLE xxl_job_log
(
    id                        BIGSERIAL    NOT NULL,
    job_group                 INTEGER      NOT NULL,
    job_id                    INTEGER      NOT NULL,
    executor_address          varchar(255)          DEFAULT NULL,
    executor_handler          varchar(255)          DEFAULT NULL,
    executor_param            varchar(512)          DEFAULT NULL,
    executor_sharding_param   varchar(20)           DEFAULT NULL,
    executor_fail_retry_count INTEGER      NOT NULL DEFAULT 0,
    trigger_time              TIMESTAMP             DEFAULT NULL,
    trigger_code              INTEGER      NOT NULL,
    trigger_msg               TEXT,
    handle_time               TIMESTAMP             DEFAULT NULL,
    handle_code               INTEGER      NOT NULL,
    handle_msg                TEXT,
    alarm_status              SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX I_trigger_time ON xxl_job_log (trigger_time);
CREATE INDEX I_handle_code ON xxl_job_log (handle_code);
CREATE INDEX I_jobid_jobgroup ON xxl_job_log (job_id, job_group);
CREATE INDEX I_job_id ON xxl_job_log (job_id);

CREATE TABLE xxl_job_log_report
(
    id            SERIAL    NOT NULL,
    trigger_day   TIMESTAMP          DEFAULT NULL,
    running_count INTEGER   NOT NULL DEFAULT 0,
    suc_count     INTEGER   NOT NULL DEFAULT 0,
    fail_count    INTEGER   NOT NULL DEFAULT 0,
    update_time   TIMESTAMP          DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX i_trigger_day ON xxl_job_log_report (trigger_day);

-- lock

CREATE TABLE xxl_job_lock
(
    lock_name varchar(50) NOT NULL,
    PRIMARY KEY (lock_name)
);

-- user

CREATE TABLE xxl_job_user
(
    id         SERIAL       NOT NULL,
    username   varchar(50)  NOT NULL,
    password   varchar(100) NOT NULL,
    token      varchar(100) DEFAULT NULL,
    role       SMALLINT     NOT NULL,
    permission varchar(255) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX i_username ON xxl_job_user (username);

-- default data

INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time)
    VALUES (1, 'xxl-job-executor-sample', '通用执行器Sample', 0, NULL, now()),
           (2, 'xxl-job-executor-sample-ai', 'AI执行器Sample', 0, NULL, now());

SELECT setval('xxl_job_group_id_seq', 2);

INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, alarm_email,
                         schedule_type, schedule_conf, misfire_strategy, executor_route_strategy,
                         executor_handler, executor_param, executor_block_strategy, executor_timeout,
                         executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime,
                         child_jobid)
VALUES (1, 1, '示例任务01', now(), now(), 'XXL', '', 'CRON', '0 0 0 * * ? *',
        'DO_NOTHING', 'FIRST', 'demoJobHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        now(), ''),
       (2, 2, 'Ollama示例任务01', now(), now(), 'XXL', '', 'NONE', '',
        'DO_NOTHING', 'FIRST', 'ollamaJobHandler', '{
    "input": "慢SQL问题分析思路",
    "prompt": "你是一个研发工程师，擅长解决技术类问题。",
    "model": "qwen3:0.6b"
}', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        now(), ''),
       (3, 2, 'Dify示例任务', now(), now(), 'XXL', '', 'NONE', '',
        'DO_NOTHING', 'FIRST', 'difyWorkflowJobHandler', '{
    "inputs":{
        "input":"查询班级各学科前三名"
    },
    "user": "xxl-job",
    "baseUrl": "http://localhost/v1",
    "apiKey": "app-OUVgNUOQRIMokfmuJvBJoUTN"
}', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        now(), '');

SELECT setval('xxl_job_info_id_seq', 3);

INSERT INTO xxl_job_user(id, username, password, role, permission)
VALUES (1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, NULL);

SELECT setval('xxl_job_user_id_seq', 1);

INSERT INTO xxl_job_lock (lock_name)
VALUES ('schedule_lock');
