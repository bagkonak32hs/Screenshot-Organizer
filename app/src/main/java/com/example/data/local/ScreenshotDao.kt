package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

data class CategoryCountTuple(
    val categoryId: String,
    val count: Int
)

@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshots WHERE isHidden = 0 ORDER BY createdAt DESC")
    fun getAllVisibleScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE isHidden = 0 ORDER BY createdAt DESC")
    suspend fun getAllVisibleScreenshotsSync(): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE isHidden = 1 ORDER BY createdAt DESC")
    fun getHiddenScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE categoryId = :categoryId AND isHidden = 0 ORDER BY createdAt DESC")
    fun getScreenshotsByCategory(categoryId: String): Flow<List<ScreenshotEntity>>

    // Dedicated core category queries (İş, Okul, Eğlence, Kişisel)
    @Query("SELECT * FROM screenshots WHERE categoryId = 'work' AND isHidden = 0 ORDER BY createdAt DESC")
    fun getWorkScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE categoryId = 'personal' AND isHidden = 0 ORDER BY createdAt DESC")
    fun getPersonalScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE categoryId = 'ideas' AND isHidden = 0 ORDER BY createdAt DESC")
    fun getIdeasScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE categoryId = 'school' AND isHidden = 0 ORDER BY createdAt DESC")
    fun getSchoolScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE categoryId = 'entertainment' AND isHidden = 0 ORDER BY createdAt DESC")
    fun getEntertainmentScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE isFavorite = 1 AND isHidden = 0 ORDER BY createdAt DESC")
    fun getFavoriteScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE isSensitive = 1 AND isHidden = 0 ORDER BY createdAt DESC")
    fun getSensitiveScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE isAnalyzed = 0 AND isHidden = 0 ORDER BY createdAt DESC")
    fun getPendingScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE id = :id LIMIT 1")
    fun getScreenshotById(id: Long): Flow<ScreenshotEntity?>

    @Query("SELECT * FROM screenshots WHERE id = :id LIMIT 1")
    suspend fun getScreenshotByIdSync(id: Long): ScreenshotEntity?

    @Query("SELECT * FROM screenshots WHERE uri = :uri LIMIT 1")
    suspend fun getScreenshotByUri(uri: String): ScreenshotEntity?

    @Query("""
        SELECT * FROM screenshots 
        WHERE isHidden = 0 AND (
            title LIKE '%' || :query || '%' OR 
            summary LIKE '%' || :query || '%' OR 
            ocrText LIKE '%' || :query || '%' OR 
            company LIKE '%' || :query || '%' OR 
            address LIKE '%' || :query || '%' OR
            tagsJson LIKE '%' || :query || '%' OR
            amount LIKE '%' || :query || '%'
        )
        ORDER BY createdAt DESC
    """)
    fun searchScreenshots(query: String): Flow<List<ScreenshotEntity>>

    @Query("SELECT COUNT(*) FROM screenshots WHERE isHidden = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE isAnalyzed = 1 AND isHidden = 0")
    fun getAnalyzedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE isAnalyzed = 0 AND isHidden = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE isAnalyzed = 0 AND isHidden = 0")
    suspend fun getPendingCountSync(): Int

    @Query("SELECT uri FROM screenshots")
    suspend fun getAllUrisSync(): List<String>

    @Query("SELECT COUNT(*) FROM screenshots WHERE categoryId = :categoryId AND isHidden = 0")
    suspend fun getCountForCategory(categoryId: String): Int

    @Query("SELECT categoryId, COUNT(*) as count FROM screenshots WHERE isHidden = 0 GROUP BY categoryId")
    suspend fun getCategoryDistribution(): List<CategoryCountTuple>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshot(screenshot: ScreenshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshots(screenshots: List<ScreenshotEntity>): List<Long>

    @Update
    suspend fun updateScreenshot(screenshot: ScreenshotEntity)

    @Delete
    suspend fun deleteScreenshot(screenshot: ScreenshotEntity)

    @Query("DELETE FROM screenshots WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM screenshots WHERE id IN (:ids)")
    suspend fun deleteMultiple(ids: List<Long>)

    @Query("UPDATE screenshots SET categoryId = :categoryId WHERE id = :id")
    suspend fun updateCategory(id: Long, categoryId: String)

    @Query("""
        UPDATE screenshots 
        SET categoryId = :categoryId, 
            subcategory = :subcategory, 
            title = :title, 
            summary = :summary, 
            confidence = :confidence, 
            ocrText = :ocrText, 
            tagsJson = :tagsJson, 
            isAnalyzed = 1, 
            analysisStatus = 'COMPLETED' 
        WHERE id = :id
    """)
    suspend fun updateClassification(
        id: Long,
        categoryId: String,
        subcategory: String,
        title: String,
        summary: String,
        confidence: Float,
        ocrText: String,
        tagsJson: String
    )

    @Query("UPDATE screenshots SET suggestedCategoriesJson = :suggestedCategoriesJson WHERE id = :id")
    suspend fun updateSuggestedCategories(id: Long, suggestedCategoriesJson: String)

    @Query("UPDATE screenshots SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE screenshots SET isHidden = :isHidden WHERE id = :id")
    suspend fun updateHidden(id: Long, isHidden: Boolean)

    @Query("DELETE FROM screenshots")
    suspend fun deleteAll()

    @Query("DELETE FROM screenshots WHERE isDemo = 1")
    suspend fun deleteDemoData()
}
