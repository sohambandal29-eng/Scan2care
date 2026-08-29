package com.example.patient

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dob: String,
    val gender: String,
    val aadhaarId: String,
    val address: String,
    val contactNumber: String = "",
    val bloodGroup: String = "",
    val bp: String = "",
    val pulse: String = "",
    val temp: String = "",
    val spo2: String = "",
    val weight: String = "",
    val emergencyContact: String = "",
    val chiefComplaints: String = "",
    val pastHistory: String = "",
    val allergies: String = "",
    val diagnosis: String = "",
    val investigations: String = "",
    val medications: String = "",
    val followUp: String = "",
    val status: String = "PENDING", // PENDING, VERIFIED, APPOINTMENT, COMPLETED
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PatientDao {
    @Insert
    suspend fun insert(patient: Patient)

    @Query("SELECT * FROM patients ORDER BY timestamp DESC")
    fun getAllPatients(): Flow<List<Patient>>

    @Update
    suspend fun update(patient: Patient)

    @Query("SELECT * FROM patients WHERE status = :status ORDER BY timestamp DESC")
    fun getPatientsByStatus(status: String): Flow<List<Patient>>
}

@Database(entities = [Patient::class], version = 3)
abstract class PatientDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao

    companion object {
        @Volatile
        private var INSTANCE: PatientDatabase? = null

        fun getDatabase(context: android.content.Context): PatientDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PatientDatabase::class.java,
                    "patient_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
