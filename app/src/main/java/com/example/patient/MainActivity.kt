package com.example.patient

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.util.*
import java.io.File
import java.text.SimpleDateFormat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var database: PatientDatabase
    private lateinit var firestoreService: FirestoreService
    
    var currentScreen by mutableStateOf("splash")
    var currentUserRole by mutableStateOf("PATIENT")
    
    var name by mutableStateOf("Not found")
    var dob by mutableStateOf("Not found")
    var gender by mutableStateOf("Not found")
    var id by mutableStateOf("Not found")
    var address by mutableStateOf("Not found")
    var scanStep by mutableStateOf("FRONT")
    var isProcessing by mutableStateOf(false)
    
    // Clinical Fields
    var contactNumber by mutableStateOf("")
    var bloodGroup by mutableStateOf("")
    var bp by mutableStateOf("")
    var pulse by mutableStateOf("")
    var temp by mutableStateOf("")
    var spo2 by mutableStateOf("")
    var weight by mutableStateOf("")
    var emergencyContact by mutableStateOf("")
    var complaints by mutableStateOf("")
    var medicalHistory by mutableStateOf("")
    var allergies by mutableStateOf("")
    var diagnosis by mutableStateOf("")
    var investigations by mutableStateOf("")
    var medications by mutableStateOf("")
    var followUpDate by mutableStateOf("")

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            processImageFromUri(this, uri, this)
        }
    }

    private fun processImageFromUri(context: Context, uri: Uri, activity: MainActivity) {
        isProcessing = true
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    activity.processRawText(visionText.text)
                    isProcessing = false
                    if (activity.scanStep == "FRONT") {
                        activity.scanStep = "BACK"
                    } else {
                        activity.scanStep = "COMPLETE"
                        activity.currentScreen = "edit"
                    }
                }
                .addOnFailureListener {
                    isProcessing = false
                    Toast.makeText(context, "OCR Failed", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            isProcessing = false
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("pdf_download", "PDF Downloads", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showDownloadNotification(context: Context, uri: Uri, filename: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, "pdf_download")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Report Downloaded")
            .setContentText(filename)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private fun populateStateFromPatient(p: Patient) {
        name = p.name
        id = p.aadhaarId
        dob = p.dob
        gender = p.gender
        address = p.address
        contactNumber = p.contactNumber
        bloodGroup = p.bloodGroup
        bp = p.bp
        pulse = p.pulse
        temp = p.temp
        spo2 = p.spo2
        weight = p.weight
        emergencyContact = p.emergencyContact
        complaints = p.chiefComplaints
        medicalHistory = p.pastHistory
        allergies = p.allergies
        diagnosis = p.diagnosis
        investigations = p.investigations
        medications = p.medications
        followUpDate = p.followUp
    }

    private fun clearPatientState() {
        name = "Not found"
        dob = "Not found"
        gender = "Not found"
        id = "Not found"
        address = "Not found"
        contactNumber = ""
        bloodGroup = ""
        bp = ""
        pulse = ""
        temp = ""
        spo2 = ""
        weight = ""
        emergencyContact = ""
        complaints = ""
        medicalHistory = ""
        allergies = ""
        diagnosis = ""
        investigations = ""
        medications = ""
        followUpDate = ""
        scanStep = "FRONT"
    }

    fun processRawText(text: String) {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        
        // Aadhaar ID: Search for the first 12-digit sequence (with or without spaces)
        val idRegex = Regex("\\d{4}\\s?\\d{4}\\s?\\d{4}")
        idRegex.find(text)?.let { id = it.value }

        // Date of Birth / Year of Birth
        val dobRegex = Regex("\\d{2}/\\d{2}/\\d{4}")
        val yobRegex = Regex("(?:Year of Birth|YOB)[:\\s]*(\\d{4})", RegexOption.IGNORE_CASE)
        dobRegex.find(text)?.let { dob = it.value } ?: yobRegex.find(text)?.let { 
            if (dob == "Not found" || dob.isEmpty()) dob = "01/01/${it.groupValues[1]}" 
        }

        // Gender
        if (text.contains("Female", ignoreCase = true) || text.contains("महिला", ignoreCase = true)) gender = "Female"
        else if (text.contains("Male", ignoreCase = true) || text.contains("पुरुष", ignoreCase = true)) gender = "Male"

        // Improved Name Extraction (Front Side)
        if (scanStep == "FRONT") {
            val exclusionKeywords = listOf("Government", "India", "भारत", "सरकार", "Male", "Female", "Birth", "Enrollment", "VID", "Aadhaar", "Unique", "Help", "www.", "yob", "Address", "Father", "Husband", "Wife")
            
            // Heuristic: Name is usually uppercase, 2-4 words, no numbers, and appears before DOB/Gender
            val potentialNames = lines.filter { line ->
                line.length in 5..40 && 
                !line.any { it.isDigit() } &&
                exclusionKeywords.none { kw -> line.contains(kw, true) } &&
                line.split(" ").size >= 2
            }
            
            if (potentialNames.isNotEmpty()) {
                // Usually the first such candidate in the list is the name
                name = potentialNames[0]
            }
        }

        // Improved Address Extraction (Back Side)
        if (scanStep == "BACK" || scanStep == "COMPLETE") {
            val addrMarkers = listOf("Address", "पता", "S/O", "D/O", "W/O", "C/O", "Care of")
            var startIndex = -1
            for (marker in addrMarkers) {
                startIndex = lines.indexOfFirst { it.contains(marker, ignoreCase = true) }
                if (startIndex != -1) break
            }

            if (startIndex != -1) {
                val addrLines = mutableListOf<String>()
                for (i in startIndex until lines.size) {
                    var line = lines[i]
                    // Clean marker from the first line
                    if (i == startIndex) {
                        for (marker in addrMarkers) {
                            line = line.replace(Regex(".*$marker[:\\s]*", RegexOption.IGNORE_CASE), "").trim()
                        }
                    }
                    
                    if (line.isNotEmpty()) {
                        addrLines.add(line)
                    }
                    
                    // Stop if we find a 6-digit pincode
                    if (Regex("\\d{6}$").containsMatchIn(line) || Regex("\\d{6}\\b").containsMatchIn(line)) break
                    if (addrLines.size > 6) break
                }
                if (addrLines.isNotEmpty()) {
                    address = addrLines.joinToString(", ").replace(", ,", ",").trim()
                }
            } else {
                // Fallback: search for a 6-digit pincode and work backwards
                val pincodeIndex = lines.indexOfFirst { Regex("\\d{6}").containsMatchIn(it) }
                if (pincodeIndex != -1) {
                    val start = maxOf(0, pincodeIndex - 4)
                    val addrLines = lines.subList(start, pincodeIndex + 1)
                    address = addrLines.joinToString(", ")
                }
            }
        }
    }

    fun generatePDF(context: Context) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        val paint = Paint()
        
        // Draw Page Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.BLACK
        canvas.drawRect(20f, 20f, pageWidth - 20f, pageHeight - 20f, paint)

        val textPaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = android.graphics.Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.rgb(0, 99, 155) // Medical Blue
        }
        val subHeaderPaint = Paint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }
        val titlePaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldPaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 50f
        val margin = 50f
        val rightMargin = pageWidth - margin
        val contentWidth = pageWidth - (2 * margin)

        // Header - SAWKAR HOSPITAL (Centered)
        headerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("SAWKAR HOSPITAL", pageWidth / 2f, y, headerPaint)
        y += 15f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("247, Penmalewadi, Maharashtra, Satara 415015", pageWidth / 2f, y, textPaint)
        y += 25f
        
        // Title
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Patient Registration & Hospital Case Record", pageWidth / 2f, y, titlePaint)
        y += 20f

        // Reset text alignment
        textPaint.textAlign = Paint.Align.LEFT
        headerPaint.textAlign = Paint.Align.LEFT

        // Top Info Row
        val regNo = "REG-${(100000..999999).random()}"
        val currentDate = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        
        canvas.drawText("Registration No: ", margin, y, boldPaint)
        canvas.drawText(regNo, margin + boldPaint.measureText("Registration No: "), y, textPaint)
        
        val dateText = "Date: $currentDate"
        canvas.drawText(dateText, rightMargin - textPaint.measureText(dateText), y, textPaint)
        y += 15f
        
        canvas.drawText("Time: $currentTime", margin, y, textPaint)
        val deptText = "Department: General Medicine"
        canvas.drawText(deptText, rightMargin - textPaint.measureText(deptText), y, textPaint)
        y += 25f

        // Patient Demographics
        canvas.drawText("Patient Demographics", margin, y, subHeaderPaint)
        y += 15f
        
        fun drawField(label: String, value: String, x: Float, currentY: Float) {
            canvas.drawText("$label: ", x, currentY, boldPaint)
            canvas.drawText(value, x + boldPaint.measureText("$label: "), currentY, textPaint)
        }

        drawField("Full Name", name, margin, y)
        y += 15f
        val ageStr = calculateAge(dob)
        drawField("Age", ageStr, margin, y)
        drawField("Gender", gender, margin + 120f, y)
        drawField("Blood Group", bloodGroup, margin + 250f, y)
        y += 15f
        drawField("Contact Number", contactNumber, margin, y)
        y += 15f
        
        canvas.drawText("Address: ", margin, y, boldPaint)
        val addrX = margin + boldPaint.measureText("Address: ")
        val addrLines = wrapText(address, rightMargin - addrX, textPaint)
        addrLines.forEachIndexed { index, line ->
            canvas.drawText(line, if (index == 0) addrX else margin + 10f, y, textPaint)
            y += 12f
        }
        y += 5f
        drawField("Emergency Contact (Name & Relation)", emergencyContact, margin, y)
        y += 25f

        // Vitals Table
        canvas.drawText("Vitals", margin, y, subHeaderPaint)
        y += 10f
        
        val col1Width = 150f
        val col2Width = 150f
        val rowHeight = 18f
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = android.graphics.Color.BLACK
        
        // Header Row
        canvas.drawRect(margin, y, margin + col1Width + col2Width, y + rowHeight, paint)
        canvas.drawText("Vital Sign", margin + 10f, y + 13f, boldPaint)
        canvas.drawText("Measurement", margin + col1Width + 10f, y + 13f, boldPaint)
        y += rowHeight
        
        val vitals = listOf(
            "Blood Pressure (BP)" to if(bp.isNotEmpty()) "$bp mmHg" else "___ / ___ mmHg",
            "Pulse Rate (PR)" to if(pulse.isNotEmpty()) "$pulse bpm" else "___ bpm",
            "Temperature" to if(temp.isNotEmpty()) "$temp °F" else "___ °F",
            "SpO2 (Oxygen)" to if(spo2.isNotEmpty()) "$spo2 %" else "___ %",
            "Weight" to if(weight.isNotEmpty()) "$weight kg" else "___ kg"
        )
        
        vitals.forEach { (sign, value) ->
            canvas.drawRect(margin, y, margin + col1Width + col2Width, y + rowHeight, paint)
            canvas.drawText(sign, margin + 10f, y + 13f, textPaint)
            canvas.drawText(value, margin + col1Width + 10f, y + 13f, textPaint)
            y += rowHeight
        }
        y += 20f

        // Clinical Assessment
        canvas.drawText("Clinical Assessment", margin, y, subHeaderPaint)
        y += 15f
        
        fun drawSection(label: String, value: String) {
            canvas.drawText(label, margin, y, boldPaint)
            y += 12f
            val displayVal = if (value.isBlank()) "______________________________________________________" else value
            val lines = wrapText(displayVal, contentWidth, textPaint)
            lines.forEach { line ->
                canvas.drawText(line, margin + 10f, y, textPaint)
                y += 12f
            }
            y += 5f
        }

        drawSection("Chief Complaints (with duration):", complaints)
        drawSection("Past Medical / Surgical History:", medicalHistory)
        drawSection("Known Allergies: [ None / Specify: ]", allergies)
        y += 10f

        // Diagnosis & Treatment Plan
        canvas.drawText("Diagnosis & Treatment Plan", margin, y, subHeaderPaint)
        y += 15f
        drawSection("Provisional Diagnosis:", diagnosis)
        drawSection("Advised Investigations (Pathology/Radiology):", investigations)
        drawSection("Prescribed Medications:", medications)
        drawField("Follow-up Date", if(followUpDate.isEmpty()) "__/__/____" else followUpDate, margin, y)
        y += 40f

        // Patient Declaration
        canvas.drawText("Patient Declaration & Consent", margin, y, subHeaderPaint)
        y += 15f
        val consentText = "I hereby declare that the information provided regarding my medical history is correct to the best of my knowledge. I consent to the initial examination, diagnostic procedures, and standard medical care provided by the attending healthcare professionals."
        wrapText(consentText, contentWidth, textPaint).forEach { line ->
            canvas.drawText(line, margin, y, textPaint)
            y += 12f
        }
        
        y += 40f
        canvas.drawText("Patient / Guardian Signature: ___________", margin, y, textPaint)
        y += 25f
        canvas.drawText("Attending Doctor's Signature: ___________", margin, y, textPaint)
        y += 25f
        canvas.drawText("Doctor's Registration Number: ___________", margin, y, textPaint)

        pdfDocument.finishPage(page)

        val cleanName = name.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val filename = "Medical_Record_${cleanName}_${System.currentTimeMillis()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/PatientReports")
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                Toast.makeText(context, "Hospital Record PDF Downloaded!", Toast.LENGTH_LONG).show()
                showDownloadNotification(context, it, filename)
                
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(it, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(openIntent, "Open Report"))
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        pdfDocument.close()
    }

    private fun wrapText(text: String, width: Float, paint: Paint): List<String> {
        val result = mutableListOf<String>()
        val words = text.split(" ")
        var line = StringBuilder()
        for (word in words) {
            if (paint.measureText(line.toString() + word) < width) {
                line.append(word).append(" ")
            } else {
                result.add(line.toString().trim())
                line = StringBuilder(word).append(" ")
            }
        }
        result.add(line.toString().trim())
        return result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = PatientDatabase.getDatabase(this)
        firestoreService = FirestoreService()
        createNotificationChannel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF00639B),
                    onPrimary = Color.White,
                    secondary = Color(0xFFE1E2EC),
                    onSecondary = Color(0xFF191C20),
                    surface = Color(0xFFFDFBFF),
                    onSurface = Color(0xFF191C20),
                    surfaceVariant = Color(0xFFE1E2EC),
                    outline = Color(0xFF757780)
                )
            ) {
                val localPatients by database.patientDao().getAllPatients().collectAsState(initial = emptyList())
                val remotePatients by firestoreService.getPatientsFlow().collectAsState(initial = emptyList())

                val patients = if (currentUserRole == "PATIENT") localPatients else remotePatients
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    when (currentScreen) {
                        "splash" -> SplashScreen(onFinished = { currentScreen = "login" })
                        "login" -> LoginScreen(onLoginSuccess = { role -> 
                            currentUserRole = role
                            currentScreen = "home" 
                        })
                        "home" -> HomeScreen(
                            role = currentUserRole,
                            onLogout = { currentScreen = "login" },
                            onStartScanner = {
                                clearPatientState()
                                currentScreen = "camera"
                            },
                            name = name,
                            dob = dob,
                            gender = gender,
                            id = id,
                            address = address,
                            isComplete = scanStep == "COMPLETE",
                            onDownload = { generatePDF(this@MainActivity) },
                            onSave = {
                                val patient = Patient(
                                    aadhaarId = id, name = name, dob = dob, gender = gender, address = address,
                                    contactNumber = contactNumber, bloodGroup = bloodGroup, bp = bp, pulse = pulse, temp = temp,
                                    spo2 = spo2, weight = weight, emergencyContact = emergencyContact,
                                    chiefComplaints = complaints, pastHistory = medicalHistory, allergies = allergies,
                                    diagnosis = diagnosis, investigations = investigations, medications = medications,
                                    followUp = followUpDate,
                                    status = if (currentUserRole == "PATIENT") "PENDING" else "VERIFIED"
                                )
                                lifecycleScope.launch {
                                    database.patientDao().insert(patient)
                                    firestoreService.uploadPatient(patient)
                                    Toast.makeText(this@MainActivity, "Record Submitted for Verification!", Toast.LENGTH_SHORT).show()
                                    if (currentUserRole == "PATIENT") {
                                        clearPatientState()
                                    }
                                }
                            },
                            onShowHistory = { currentScreen = "history" },
                            recentPatients = patients,
                            onPatientClick = { p ->
                                populateStateFromPatient(p)
                                if (currentUserRole == "DOCTOR") {
                                    currentScreen = "view"
                                } else {
                                    currentScreen = "edit"
                                }
                            }
                        )
                        "camera" -> CameraPreviewScreen(
                            onBack = { currentScreen = "home" },
                            isProcessing = isProcessing,
                            scanStep = scanStep,
                            onGalleryPick = { 
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                            }
                        )
                        "edit" -> EditRecordScreen(
                            onBack = { currentScreen = "home" },
                            onSave = {
                                val currentPatientId = patients.find { it.aadhaarId == id }?.id ?: 0
                                val patient = Patient(
                                    id = currentPatientId,
                                    aadhaarId = id, name = name, dob = dob, gender = gender, address = address,
                                    contactNumber = contactNumber, bloodGroup = bloodGroup, bp = bp, pulse = pulse, temp = temp,
                                    spo2 = spo2, weight = weight, emergencyContact = emergencyContact,
                                    chiefComplaints = complaints, pastHistory = medicalHistory, allergies = allergies,
                                    diagnosis = diagnosis, investigations = investigations, medications = medications,
                                    followUp = followUpDate,
                                    status = if (currentUserRole == "STAFF") "VERIFIED" else "APPOINTMENT"
                                )
                                lifecycleScope.launch {
                                    if (currentPatientId != 0) {
                                        database.patientDao().update(patient)
                                    } else {
                                        database.patientDao().insert(patient)
                                    }
                                    firestoreService.uploadPatient(patient)
                                    generatePDF(this@MainActivity)
                                    Toast.makeText(this@MainActivity, "Record Updated & Verified", Toast.LENGTH_SHORT).show()
                                    if (currentUserRole == "PATIENT") clearPatientState()
                                    currentScreen = "home"
                                }
                            },
                            name, { name = it }, dob, { dob = it }, gender, { gender = it }, id, { id = it }, address, { address = it },
                            contactNumber, { contactNumber = it }, bloodGroup, { bloodGroup = it }, bp, { bp = it }, pulse, { pulse = it },
                            temp, { temp = it }, spo2, { spo2 = it }, weight, { weight = it }, emergencyContact, { emergencyContact = it },
                            complaints, { complaints = it }, medicalHistory, { medicalHistory = it }, allergies, { allergies = it },
                            diagnosis, { diagnosis = it }, investigations, { investigations = it }, medications, { medications = it },
                            followUpDate, { followUpDate = it }
                        )
                        "view" -> ViewRecordScreen(
                            onBack = { currentScreen = "home" },
                            onDownload = { generatePDF(this@MainActivity) },
                            name, dob, gender, id, address, contactNumber, bloodGroup, bp, pulse, temp, spo2, weight,
                            emergencyContact, complaints, medicalHistory, allergies, diagnosis, investigations, medications, followUpDate
                        )
                        "history" -> HistoryScreen(
                            role = currentUserRole,
                            patients = patients, 
                            onBack = { currentScreen = "home" },
                            onPatientClick = { p ->
                                populateStateFromPatient(p)
                                if (currentUserRole == "DOCTOR") currentScreen = "view"
                                else currentScreen = "edit"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.HealthAndSafety, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("scan2care", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Secure Portal Access", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = username, onValueChange = { username = it }, label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(), singleLine = true
        )

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val role = when {
                    username == "patient" && password == "patient123" -> "PATIENT"
                    username == "staff" && password == "staff123" -> "STAFF"
                    username == "doctor" && password == "doctor123" -> "DOCTOR"
                    username == "admin" && password == "admin123" -> "ADMIN"
                    else -> null
                }
                if (role != null) onLoginSuccess(role)
                else error = "Invalid username or password"
            },
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)
        ) {
            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Demo: patient/patient123, staff/staff123, doctor/doctor123, admin/admin123",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    role: String, onLogout: () -> Unit, onStartScanner: () -> Unit,
    name: String, dob: String, gender: String, id: String, address: String,
    isComplete: Boolean, onDownload: () -> Unit, onSave: () -> Unit, onShowHistory: () -> Unit,
    recentPatients: List<Patient>, onPatientClick: (Patient) -> Unit
) {
    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HealthAndSafety, "Logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("scan2care", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = { IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") } },
                actions = { 
                    IconButton(onClick = onShowHistory) { Icon(Icons.Default.History, "History") } 
                }
            ) 
        },
        floatingActionButton = { 
            when (role) {
                "PATIENT" -> {
                    ExtendedFloatingActionButton(
                        onClick = onStartScanner, icon = { Icon(Icons.Default.CloudUpload, null) },
                        text = { Text("Upload ID Document") },
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
                "STAFF" -> {
                    ExtendedFloatingActionButton(
                        onClick = onStartScanner, icon = { Icon(Icons.Default.AddCircle, null) },
                        text = { Text("New Registration") },
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary
                    ) 
                }
                "DOCTOR" -> {
                    ExtendedFloatingActionButton(
                        onClick = onShowHistory, icon = { Icon(Icons.Default.Search, null) },
                        text = { Text("Patient Search") },
                        containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = when(role) {
                        "DOCTOR" -> MaterialTheme.colorScheme.secondaryContainer
                        "PATIENT" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }, shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when(role) {
                                "DOCTOR" -> Icons.Default.MedicalServices
                                "PATIENT" -> Icons.Default.Person
                                else -> Icons.Default.Badge
                            }, null, 
                            tint = when(role) {
                                "DOCTOR" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "PATIENT" -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Logged in as", 
                                style = MaterialTheme.typography.labelSmall,
                                color = when(role) {
                                    "DOCTOR" -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    "PATIENT" -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                }
                            )
                            Text(
                                role, 
                                style = MaterialTheme.typography.titleSmall, 
                                fontWeight = FontWeight.Bold,
                                color = when(role) {
                                    "DOCTOR" -> MaterialTheme.colorScheme.onSecondaryContainer
                                    "PATIENT" -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }
            }

            // Role-Specific Action Cards
            if (role == "PATIENT" && (name == "Not found" || id == "Not found")) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Pending Actions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Aadhaar Verification Pending", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (role == "DOCTOR") {
                item {
                    Text(
                        "Daily Schedule Summary",
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Today's Appointments", 
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    "${recentPatients.size} Patients",
                                    style = MaterialTheme.typography.headlineMedium, 
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Button(
                                onClick = onShowHistory, 
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("View All", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (name != "Not found" || id != "Not found") {
                item {
                    Text(
                        if (role == "PATIENT") "Your Information" else "Latest Scan Result",
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("Aadhaar ID: $id", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            val age = calculateAge(dob)
                            DetailRow("Date of Birth", if (age != "___") "$dob (Age: $age)" else dob, Icons.Default.CalendarToday)
                            DetailRow("Gender", gender, if (gender == "Female") Icons.Default.Female else Icons.Default.Male)
                            DetailRow("Address", address, Icons.Default.LocationOn)

                            if (isComplete && (role == "STAFF" || role == "DOCTOR" || role == "ADMIN" || role == "PATIENT")) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row {
                                    Button(
                                        onClick = onSave, modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text("Save Record")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = onDownload, modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text("PDF")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (role == "PATIENT" || role == "STAFF") {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text("Ready to Scan", fontWeight = FontWeight.Bold)
                            Text("Scan Aadhaar to begin registration", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            // Removed the if (role != "PATIENT") check to show history to everyone
            item {
                Text(
                    if (role == "PATIENT") "Your Registration History" else "Recent Registrations", 
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp), 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
            }

            if (recentPatients.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No records found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(recentPatients) { patient ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onPatientClick(patient) },
                        colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text(patient.name.take(1).uppercase(), fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(patient.name, fontWeight = FontWeight.SemiBold)
                                Text("ID: ${patient.aadhaarId}", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.size(18.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(role: String, patients: List<Patient>, onBack: () -> Unit, onPatientClick: (Patient) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Registration History") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            items(patients) { patient ->
                Card(
                    modifier = Modifier.padding(16.dp, 8.dp).fillMaxWidth().clickable { onPatientClick(patient) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(patient.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Aadhaar: ${patient.aadhaarId}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Date: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(patient.timestamp))}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(targetValue = if (startAnimation) 1f else 0f, animationSpec = tween(1500))
    val scaleAnim = animateFloatAsState(targetValue = if (startAnimation) 1f else 0.7f, animationSpec = tween(1500))
    LaunchedEffect(true) { startAnimation = true; delay(2500); onFinished() }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00639B)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.HealthAndSafety, null, modifier = Modifier.size(100.dp).scale(scaleAnim.value), tint = Color.White)
            Spacer(Modifier.height(24.dp))
            Text("scan2care", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.alpha(alphaAnim.value).scale(scaleAnim.value))
        }
    }
}

@Composable
fun CameraPreviewScreen(onBack: () -> Unit, isProcessing: Boolean, scanStep: String, onGalleryPick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build() 
    }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
            }
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }, modifier = Modifier.fillMaxSize())
        
        // Guidelines for 3:4
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f/4f)
                .background(Color.Transparent)
                .padding(16.dp)
            ) {
                // Border/Corners for Aadhaar framing
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(2.dp)) {
                   // Optional: add visual guide lines here
                }
            }
        }

        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }

        if (isProcessing) { 
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White) 
        } else {
            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (scanStep == "FRONT") "Scan Front of Aadhaar" else "Scan Back of Aadhaar", 
                    color = Color.White, 
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onGalleryPick,
                        modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                    ) {
                        Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    Button(
                        onClick = { capturePhoto(context, imageCapture, activity) },
                        modifier = Modifier.height(56.dp).padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (scanStep == "FRONT") "Capture Front" else "Capture Back")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordScreen(
    onBack: () -> Unit, onSave: () -> Unit,
    name: String, onNameChange: (String) -> Unit, dob: String, onDobChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit, id: String, onIdChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit, contact: String, onContactChange: (String) -> Unit,
    bGroup: String, onBGroupChange: (String) -> Unit, bp: String, onBpChange: (String) -> Unit,
    pulse: String, onPulseChange: (String) -> Unit, temp: String, onTempChange: (String) -> Unit,
    spo2: String, onSpo2Change: (String) -> Unit, weight: String, onWeightChange: (String) -> Unit,
    emergency: String, onEmergencyChange: (String) -> Unit, complaints: String, onComplaintsChange: (String) -> Unit,
    history: String, onHistoryChange: (String) -> Unit, allergies: String, onAllergiesChange: (String) -> Unit,
    diagnosis: String, onDiagnosisChange: (String) -> Unit, investigations: String, onInvestigationsChange: (String) -> Unit,
    meds: String, onMedsChange: (String) -> Unit, followUp: String, onFollowUpChange: (String) -> Unit
) {
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Edit Record") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { Button(onClick = onSave) { Text("Save & PDF") } }
            ) 
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                Text("Personal Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                EditField("Name", name, onNameChange)
                EditField("DOB", dob, onDobChange)
                EditField("Gender", gender, onGenderChange)
                EditField("Aadhaar", id, onIdChange)
                EditField("Address", address, onAddressChange)
                EditField("Contact", contact, onContactChange)
                EditField("Blood Group", bGroup, onBGroupChange)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Vitals", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                Row {
                    Box(modifier = Modifier.weight(1f)) { EditField("BP", bp, onBpChange) }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) { EditField("Pulse", pulse, onPulseChange) }
                }
                Row {
                    Box(modifier = Modifier.weight(1f)) { EditField("Temp", temp, onTempChange) }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) { EditField("SpO2", spo2, onSpo2Change) }
                }
                EditField("Weight", weight, onWeightChange)
                EditField("Emergency Contact", emergency, onEmergencyChange)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Clinical Assessment", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                EditField("Complaints", complaints, onComplaintsChange)
                EditField("History", history, onHistoryChange)
                EditField("Allergies", allergies, onAllergiesChange)
                EditField("Diagnosis", diagnosis, onDiagnosisChange)
                EditField("Investigations", investigations, onInvestigationsChange)
                EditField("Medications", meds, onMedsChange)
                EditField("Follow-up Date", followUp, onFollowUpChange)
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewRecordScreen(
    onBack: () -> Unit, onDownload: () -> Unit,
    name: String, dob: String, gender: String, id: String, address: String,
    contact: String, bGroup: String, bp: String, pulse: String, temp: String, spo2: String, weight: String,
    emergency: String, complaints: String, history: String, allergies: String,
    diagnosis: String, investigations: String, meds: String, followUp: String
) {
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Patient Record") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = onDownload) { Icon(Icons.Default.PictureAsPdf, null) } }
            ) 
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Aadhaar: $id", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                ViewSection("Personal Info") {
                    ViewRow("DOB", dob)
                    ViewRow("Gender", gender)
                    ViewRow("Contact", contact)
                    ViewRow("Blood Group", bGroup)
                    ViewRow("Address", address)
                }

                Spacer(modifier = Modifier.height(16.dp))
                ViewSection("Vitals") {
                    Row {
                        Box(Modifier.weight(1f)) { ViewRow("BP", bp) }
                        Box(Modifier.weight(1f)) { ViewRow("Pulse", "$pulse bpm") }
                    }
                    Row {
                        Box(Modifier.weight(1f)) { ViewRow("Temp", "$temp °F") }
                        Box(Modifier.weight(1f)) { ViewRow("SpO2", "$spo2 %") }
                    }
                    ViewRow("Weight", "$weight kg")
                    ViewRow("Emergency", emergency)
                }

                Spacer(modifier = Modifier.height(16.dp))
                ViewSection("Clinical Details") {
                    ViewRow("Complaints", complaints)
                    ViewRow("History", history)
                    ViewRow("Allergies", allergies)
                    ViewRow("Diagnosis", diagnosis)
                    ViewRow("Medications", meds)
                    ViewRow("Follow-up", followUp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ViewSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        content()
    }
}

@Composable
fun ViewRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun EditField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
}

fun calculateAge(dob: String): String {
    return try {
        val year = if (dob.contains("/")) dob.split("/").last().toInt() else dob.toInt()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        (currentYear - year).toString()
    } catch (e: Exception) { "___" }
}

private fun capturePhoto(context: Context, imageCapture: ImageCapture, activity: MainActivity) {
    activity.isProcessing = true
    val file = File(context.cacheDir, "scan.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = try {
                InputImage.fromFilePath(context, Uri.fromFile(file))
            } catch (e: Exception) {
                activity.isProcessing = false
                Toast.makeText(context, "Image error", Toast.LENGTH_SHORT).show()
                return
            }
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    activity.processRawText(visionText.text)
                    activity.isProcessing = false
                    if (activity.scanStep == "FRONT") {
                        activity.scanStep = "BACK"
                    } else {
                        activity.scanStep = "COMPLETE"
                        activity.currentScreen = "edit"
                    }
                }
                .addOnFailureListener {
                    activity.isProcessing = false
                    Toast.makeText(context, "OCR failed", Toast.LENGTH_SHORT).show()
                }
        }
        override fun onError(exc: ImageCaptureException) {
            activity.isProcessing = false
            Toast.makeText(context, "Capture Failed: ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    })
}
