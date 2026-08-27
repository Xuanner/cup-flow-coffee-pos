# US-S2-SEC-01 验收记录

| 项目 | 结果 |
| --- | --- |
| Story | `US-S2-SEC-01` 安全保存和校验密码 |
| 验收 Task | `TASK-S2-SEC-01-03` |
| 验收日期 | 2026-08-27 |
| 环境 | JDK 25、PostgreSQL 18.4 Testcontainers、Spring Boot |
| 结论 | 通过；密码安全基础能力可供登录应用服务使用 |

算法参数依据 OWASP Password Storage Cheat Sheet：PBKDF2-HMAC-SHA256 使用 600,000 次迭代，并为每个摘要生成独立随机盐。参考：<https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html>。

## 验收标准与证据

| 验收标准 | 自动化或检查证据 | 结果 |
| --- | --- | --- |
| 数据库只保存批准的自适应单向摘要 | `password_hash` 字段、`Pbkdf2PasswordHasher`；PBKDF2-HMAC-SHA256，600,000 次迭代 | 通过 |
| 每个摘要使用独立随机盐，相同密码的摘要不同且均可验证 | `TC-S2-PASS-003` | 通过 |
| 正确密码通过，错误密码失败 | `TC-S2-PASS-001`、`002` | 通过 |
| 摘要使用严格格式解析和常量时间派生结果比较 | `Pbkdf2PasswordHasher.matches`、`TC-S2-PASS-001` 至 `005` | 通过 |
| 未知账号与错误密码走同类昂贵校验路径 | `AccountPasswordVerifier` 使用启动时随机诱饵摘要；`TC-S2-PASS-004` 断言仍执行一次摘要校验且结果失败 | 通过 |
| 损坏、降级或不支持的摘要安全失败且不回显 | `TC-S2-PASS-005` | 通过 |
| 认证对象、异常、日志和测试产物不输出密码或摘要 | `PasswordHash.toString` 脱敏、认证组件无凭证日志、`TC-S2-AUDIT-009` | 通过 |
| 敏感值扫描器能发现泄漏且自身不回显敏感值 | `TC-S2-AUDIT-010` | 通过 |

密码输入不会被去空白、改变大小写或修改 Unicode 内容；接口层后续按契约拒绝空密码及超过 128 字符的输入。校验器对越界输入直接返回不匹配，不进行不可控的昂贵计算。

## 执行记录

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw -Dtest=Pbkdf2PasswordHasherTest,AccountPasswordVerifierTest,SensitiveValueScannerTest test
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ./mvnw clean verify
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

全量测试报告使用专用测试敏感值扫描，无命中；`git diff --check` 通过。测试凭证仅存在于测试源代码，不写入日志、异常、测试报告或快照。

## 后续接入要求

- `TASK-S2-AUTH-01-01` 必须通过 `AccountPasswordVerifier` 校验密码，不得自行比较摘要或在未知账号时提前返回。
- 账号状态、统一认证失败、限流和会话创建属于后续登录 Story；密码校验成功本身不得创建会话。
