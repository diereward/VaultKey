package com.vaultkey.data.database;

import android.content.Context;
import android.database.Cursor;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.vaultkey.data.model.PasswordEntry;

@Database(entities = {PasswordEntry.class}, version = 8, exportSchema = false)
public abstract class VaultDatabase extends RoomDatabase {

    private static volatile VaultDatabase INSTANCE;

    public abstract PasswordDao passwordDao();

    private static final Migration[] MIGRATIONS_TO_V6;
    static {
        MIGRATIONS_TO_V6 = new Migration[5];
        for (int i = 0; i < 5; i++) {
            MIGRATIONS_TO_V6[i] = new Migration(i + 1, 6) {
                @Override public void migrate(SupportSQLiteDatabase db) { migrateToV6(db); }
            };
        }
    }

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!hasColumn(db, "passwords", "deleted"))
                db.execSQL("ALTER TABLE passwords ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
            if (!hasColumn(db, "passwords", "deletedAt"))
                db.execSQL("ALTER TABLE passwords ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `passwords_v8` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `username` TEXT, `password` TEXT, `url` TEXT, `notes` TEXT, `category` TEXT, `strengthScore` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL DEFAULT 0, `updatedAt` INTEGER NOT NULL DEFAULT 0, `avatarPath` TEXT NOT NULL DEFAULT '')");
            db.execSQL("INSERT INTO passwords_v8 (id, title, username, password, url, notes, category, strengthScore, createdAt, updatedAt, avatarPath) SELECT id, title, username, password, url, notes, category, strengthScore, createdAt, updatedAt, avatarPath FROM passwords");
            db.execSQL("DROP TABLE passwords");
            db.execSQL("ALTER TABLE passwords_v8 RENAME TO passwords");
        }
    };

    public static VaultDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (VaultDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), VaultDatabase.class, "vault.db")
                        .addMigrations(MIGRATIONS_TO_V6)
                        .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void migrateToV6(SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS passwords_new");
        db.execSQL("CREATE TABLE IF NOT EXISTS passwords_new ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
            + "title TEXT, username TEXT, password TEXT, url TEXT, notes TEXT, category TEXT,"
            + "strengthScore INTEGER NOT NULL DEFAULT 0,"
            + "createdAt INTEGER NOT NULL DEFAULT 0,"
            + "updatedAt INTEGER NOT NULL DEFAULT 0,"
            + "avatarPath TEXT NOT NULL DEFAULT '')");

        if (tableExists(db, "passwords")) {
            db.execSQL("INSERT INTO passwords_new (id, title, username, password, url, notes, category, strengthScore, createdAt, updatedAt, avatarPath) SELECT "
                + col(db, "id", "NULL") + ", " + col(db, "title", "''") + ", " + col(db, "username", "''") + ", "
                + col(db, "password", "''") + ", " + col(db, "url", "''") + ", " + col(db, "notes", "''") + ", "
                + col(db, "category", "'Other'") + ", " + col(db, "strengthScore", "0") + ", "
                + col(db, "createdAt", "0") + ", " + col(db, "updatedAt", "0") + ", " + col(db, "avatarPath", "''")
                + " FROM passwords");
        }

        db.execSQL("DROP TABLE IF EXISTS passwords");
        db.execSQL("ALTER TABLE passwords_new RENAME TO passwords");
        db.execSQL("DROP TABLE IF EXISTS bank_cards");
        db.execSQL("DROP TABLE IF EXISTS notes");
        db.execSQL("DROP TABLE IF EXISTS receipts");
        db.execSQL("DROP TABLE IF EXISTS personal_info");
    }

    private static String col(SupportSQLiteDatabase db, String column, String def) {
        return hasColumn(db, "passwords", column) ? "COALESCE(" + column + ", " + def + ")" : def;
    }

    private static boolean tableExists(SupportSQLiteDatabase db, String table) {
        try (Cursor c = db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='" + table + "' LIMIT 1")) {
            return c.moveToFirst();
        }
    }

    private static boolean hasColumn(SupportSQLiteDatabase db, String table, String column) {
        if (!tableExists(db, table)) return false;
        try (Cursor c = db.query("PRAGMA table_info(" + table + ")")) {
            int idx = c.getColumnIndex("name");
            if (idx == -1) return false;
            while (c.moveToNext()) if (column.equals(c.getString(idx))) return true;
        }
        return false;
    }
}
