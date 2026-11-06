package pl.croolyy.timer

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.croolyy.timer.data.CrooDatabase

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = android.app.Application::class)
class MigrationTest {
    @Test fun migrationOneToTwoPreservesLibraryAndDefaultsFinishedToFalse() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${java.util.UUID.randomUUID()}.db"
        val schema = Json.parseToJsonElement(javaClass.getResource("/pl.croolyy.timer.data.CrooDatabase/2.json")!!.readText()).jsonObject
            .getValue("database").jsonObject.getValue("entities").jsonArray
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    schema.forEach { entry ->
                        val entity = entry.jsonObject
                        val table = entity.getValue("tableName").jsonPrimitive.content
                        var sql = entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)
                        if (table == "books") sql = sql.replace(", `finished` INTEGER NOT NULL DEFAULT 0", "")
                        db.execSQL(sql)
                        entity["indices"]?.jsonArray?.forEach { index ->
                            db.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                        }
                    }
                    db.execSQL("INSERT INTO books (id,title,author,totalPages,currentPage,cover,createdAt) VALUES ('book-1','Ocalona książka','Autorka',120,0,NULL,1)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        helper.writableDatabase
        helper.close()
        val migrated = Room.databaseBuilder(context, CrooDatabase::class.java, name).addMigrations(CrooDatabase.MIGRATION_1_2).build()
        try {
            assertEquals("Ocalona książka", migrated.dao().allBooks().single().title)
            assertFalse(migrated.dao().allBooks().single().finished)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
