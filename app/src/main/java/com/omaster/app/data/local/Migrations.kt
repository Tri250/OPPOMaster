package com.omaster.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移脚本
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2
)

/**
 * 版本1到2的迁移
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加新表或修改现有表结构
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS usage_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                presetId TEXT NOT NULL,
                action TEXT NOT NULL,
                metadata TEXT,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """)
        
        // 添加索引
        database.execSQL("CREATE INDEX IF NOT EXISTS index_usage_logs_presetId ON usage_logs(presetId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_usage_logs_createdAt ON usage_logs(createdAt)")
    }
}
