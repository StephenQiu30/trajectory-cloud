# SQL 数据库初始化脚本

本目录包含 `轨迹-基于AIGC的数据可视化平台` 全系统所需的数据库定义（DDL）。系统已完成数据库统一工作，所有服务共享同一个
`trajectory` 数据库。

## 🗄️ 数据库架构

系统采用统一数据库设计，所有业务表均存放在 `trajectory` 库中：

| 领域 | 核心表                                                                 |
|:---|:--------------------------------------------------------------------|
| 用户 | `user`, `user_login_log`                                            |
| AI | `ai_chat_record`                                                    |
| 通知 | `notification`, `email_record`                                      |
| 系统 | `api_access_log`, `operation_log`, `file_upload_record`, `undo_log` |

## 🛠️ 初始化步骤

### 一键初始化 (推荐)

我们提供了一个交互式脚本来一键完成库创建和表初始化：

```bash
chmod +x init_mysql.sh
./init_mysql.sh
```

### 手动初始化

如果你希望手动执行，请直接运行全量脚本：

```bash
mysql -u root -p < trajectory_all.sql
```

## 📝 开发规范

- **字符集**: 统一使用 `utf8mb4`。
- **公共字段**: 所有表包含 `id`, `create_time`, `update_time`, `is_delete`。
- **幂等性**: 每个脚本均包含 `DROP TABLE IF EXISTS`，可重复执行。

## ⚠️ 注意事项

1. **数据备份**: 执行初始化脚本会删除现有表（Drop Table），请务必先备份重要数据。
2. **Seata 支持**: `undo_log` 表已内置在全局脚本中，无需单独创建。
