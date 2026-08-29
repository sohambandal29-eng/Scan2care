package com.example.patient

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()
    private val patientsCollection = db.collection("patients")

    suspend fun uploadPatient(patient: Patient) {
        try {
            val patientMap = mapOf(
                "name" to patient.name,
                "dob" to patient.dob,
                "gender" to patient.gender,
                "aadhaarId" to patient.aadhaarId,
                "address" to patient.address,
                "contactNumber" to patient.contactNumber,
                "bloodGroup" to patient.bloodGroup,
                "bp" to patient.bp,
                "pulse" to patient.pulse,
                "temp" to patient.temp,
                "spo2" to patient.spo2,
                "weight" to patient.weight,
                "emergencyContact" to patient.emergencyContact,
                "chiefComplaints" to patient.chiefComplaints,
                "pastHistory" to patient.pastHistory,
                "allergies" to patient.allergies,
                "diagnosis" to patient.diagnosis,
                "investigations" to patient.investigations,
                "medications" to patient.medications,
                "followUp" to patient.followUp,
                "status" to patient.status,
                "timestamp" to patient.timestamp
            )
            
            // Use Aadhaar + Timestamp to ensure previous entries are preserved as history
            // but also allow uniquely identifying a specific registration event.
            val docId = if (patient.aadhaarId.isNotBlank() && patient.aadhaarId != "Not found") {
                "${patient.aadhaarId.replace(" ", "")}_${patient.timestamp}"
            } else {
                patient.timestamp.toString()
            }
            
            patientsCollection.document(docId)
                .set(patientMap).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPatientsFlow(): Flow<List<Patient>> = callbackFlow {
        val subscription = patientsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    // Don't close the flow with an error to avoid crashing the UI
                    // Instead, we can try to send an empty list or just ignore
                    return@addSnapshotListener
                }
                
                val patients = snapshot?.documents?.mapNotNull { doc ->
                    Patient(
                        id = 0,
                        name = doc.getString("name") ?: "",
                        dob = doc.getString("dob") ?: "",
                        gender = doc.getString("gender") ?: "",
                        aadhaarId = doc.getString("aadhaarId") ?: "",
                        address = doc.getString("address") ?: "",
                        contactNumber = doc.getString("contactNumber") ?: "",
                        bloodGroup = doc.getString("bloodGroup") ?: "",
                        bp = doc.getString("bp") ?: "",
                        pulse = doc.getString("pulse") ?: "",
                        temp = doc.getString("temp") ?: "",
                        spo2 = doc.getString("spo2") ?: "",
                        weight = doc.getString("weight") ?: "",
                        emergencyContact = doc.getString("emergencyContact") ?: "",
                        chiefComplaints = doc.getString("chiefComplaints") ?: "",
                        pastHistory = doc.getString("pastHistory") ?: "",
                        allergies = doc.getString("allergies") ?: "",
                        diagnosis = doc.getString("diagnosis") ?: "",
                        investigations = doc.getString("investigations") ?: "",
                        medications = doc.getString("medications") ?: "",
                        followUp = doc.getString("followUp") ?: "",
                        status = doc.getString("status") ?: "PENDING",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                
                trySend(patients)
            }
        
        awaitClose { subscription.remove() }
    }

    suspend fun getAllPatients(): List<Patient> {
        return try {
            patientsCollection.get().await().map { doc ->
                    Patient(
                        id = 0,
                        name = doc.getString("name") ?: "",
                        dob = doc.getString("dob") ?: "",
                        gender = doc.getString("gender") ?: "",
                        aadhaarId = doc.getString("aadhaarId") ?: "",
                        address = doc.getString("address") ?: "",
                        contactNumber = doc.getString("contactNumber") ?: "",
                        bloodGroup = doc.getString("bloodGroup") ?: "",
                        bp = doc.getString("bp") ?: "",
                        pulse = doc.getString("pulse") ?: "",
                        temp = doc.getString("temp") ?: "",
                        spo2 = doc.getString("spo2") ?: "",
                        weight = doc.getString("weight") ?: "",
                        emergencyContact = doc.getString("emergencyContact") ?: "",
                        chiefComplaints = doc.getString("chiefComplaints") ?: "",
                        pastHistory = doc.getString("pastHistory") ?: "",
                        allergies = doc.getString("allergies") ?: "",
                        diagnosis = doc.getString("diagnosis") ?: "",
                        investigations = doc.getString("investigations") ?: "",
                        medications = doc.getString("medications") ?: "",
                        followUp = doc.getString("followUp") ?: "",
                        status = doc.getString("status") ?: "PENDING",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
