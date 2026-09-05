package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CatalogDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.model.CatalogItem
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.toCatalogItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [Product::class, Sale::class, SaleItem::class, CatalogItem::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atelier_control_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.productDao(), database.saleDao(), database.catalogDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(productDao: ProductDao, saleDao: SaleDao, catalogDao: CatalogDao) {
            // Seed official catalog in catalog table (does NOT appear in main products until included)
            if (catalogDao.getCount() == 0) {
                val catalogItems = OfficialCatalog.getOfficialCatalog().map { it.toCatalogItem() }
                catalogDao.insertAll(catalogItems)
            }

            if (productDao.getCount() > 0) return

            // Seed only the base registered products for Cadastros
            val baseProducts = listOf(
                Product(
                    name = "Ímã",
                    category = "Ímas",
                    price = 10.00
                ),
                Product(
                    name = "Quadro",
                    category = "Obras de Arte",
                    price = 10000.00
                )
            )
            productDao.insertAll(baseProducts)

            // Seed sales matching September 4, 2026 from the video
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.SEPTEMBER)
                set(Calendar.DAY_OF_MONTH, 4)
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 28)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val timeQuadro = cal.timeInMillis

            cal.set(Calendar.MINUTE, 27)
            val timeIma = cal.timeInMillis

            // Sale 1: Quadro with 10% off (R$ 9.000,00) via PIX
            val saleQuadro = Sale(
                timestamp = timeQuadro,
                customerName = "Cliente Balcão",
                paymentMethod = "PIX",
                discount = 1000.00,
                totalAmount = 9000.00,
                totalCost = 4500.00,
                notes = "10% off"
            )
            val saleQuadroId = saleDao.insertSale(saleQuadro)
            saleDao.insertSaleItems(
                listOf(
                    SaleItem(
                        saleId = saleQuadroId,
                        productId = 2,
                        productName = "Quadro",
                        unitPrice = 10000.00,
                        unitCost = 4500.00,
                        quantity = 1,
                        subtotal = 10000.00
                    )
                )
            )

            // Sale 2: Ímã (R$ 10,00) via PIX
            val saleIma = Sale(
                timestamp = timeIma,
                customerName = "Cliente Balcão",
                paymentMethod = "PIX",
                discount = 0.0,
                totalAmount = 10.00,
                totalCost = 3.00,
                notes = ""
            )
            val saleImaId = saleDao.insertSale(saleIma)
            saleDao.insertSaleItems(
                listOf(
                    SaleItem(
                        saleId = saleImaId,
                        productId = 1,
                        productName = "Ímã",
                        unitPrice = 10.00,
                        unitCost = 3.00,
                        quantity = 1,
                        subtotal = 10.00
                    )
                )
            )
        }
    }
}
