package com.example.myapplication

import androidx.room.*
import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.RoomDatabase
import java.util.Date
import androidx.room.TypeConverters
import com.example.myapplication.helpers.Converters

// Room Database
@Database(
    entities = [
        StillLocation::class,
        MovementActivity::class,
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ActivityDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    companion object {
        @Volatile
        private var INSTANCE: ActivityDatabase? = null

        fun getDatabase(context: Context): ActivityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ActivityDatabase::class.java,
                    "activity_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity(tableName = "still_locations")
data class StillLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lat: Double?,
    val lng: Double?,
    val startTimeDate: Date,
    val endTimeDate: Date? = null,
    val wasSupposedToBeActivity: String? = null, // If this was detected during a movement activity
    val placeId: String? = null,
    val placeName: String? = null,
    val placeCategory: String? = null,
    val placeAddress: String? = null
)

@Entity(tableName = "movement_activities")
data class MovementActivity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityType: String,
    val startLat: Double?,
    val startLng: Double?,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val startTimeDate: Date,
    val endTimeDate : Date? = null,
)


@Dao
interface ActivityDao {
    @Insert
    suspend fun insertStillLocation(stillLocation: StillLocation): Long

    @Query("DELETE FROM still_locations WHERE id = :id")
    suspend fun deleteStillLocation(id: Long)

    @Query("""UPDATE still_locations SET endTimeDate = :endTimeDate WHERE id = :id""")
    suspend fun endStillLocation(
        id: Long?,
        endTimeDate: Date,
    )
    @Query(""" UPDATE still_locations SET endTimeDate = :endTimeDate WHERE id = :id""")

    suspend fun updateStillEndTime(
        id: Long,
        endTimeDate: Date
    )

    @Query("SELECT * FROM still_locations WHERE id = :id LIMIT 1")
    suspend fun getStillLocationById(id: Long): StillLocation?

    @Insert
    suspend fun insertMovementActivity(movementActivity: MovementActivity): Long

    @Query("DELETE FROM movement_activities WHERE id = :id")
    suspend fun deleteMovementActivity(id: Long)

    @Query("""UPDATE movement_activities SET endLat = :endLatitude, endLng = :endLongitude, endTimeDate= :endTimeDate WHERE id = :id""")
    suspend fun endMovementActivity(
        id: Long?,
        endLatitude: Double?,
        endLongitude: Double?,
        endTimeDate: Date,
    )

    @Query(""" UPDATE movement_activities SET endTimeDate = :endTimeDate WHERE id = :id""")
    suspend fun updateMovementEndTime(
        id: Long,
        endTimeDate: Date
    )
    @Query("""
    SELECT * FROM still_locations
    WHERE (startTimeDate BETWEEN :start AND :end)
       OR (endTimeDate BETWEEN :start AND :end)
""")
    suspend fun getStillForDay(start: Date, end: Date): List<StillLocation>

    @Query("""
    SELECT * FROM movement_activities
    WHERE (startTimeDate BETWEEN :start AND :end)
       OR (endTimeDate BETWEEN :start AND :end)
""")
    suspend fun getMovementForDay(start: Date, end: Date): List<MovementActivity>

    @Transaction
    suspend fun replaceStillWithMovement(id: Long, movement: MovementActivity) {
        deleteStillLocation(id)
        insertMovementActivity(movement)
    }

}


