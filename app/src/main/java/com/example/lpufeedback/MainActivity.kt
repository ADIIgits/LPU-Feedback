package com.example.lpufeedback

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

// ───────────────────────────────────────────────
// GLOBAL DATA OBJECT
// ───────────────────────────────────────────────
object GlobalFeedbackStore {
    val feedbackList = mutableListOf<FeedbackItem>()
}
object GlobalQuestionsStore {
    val questions = mutableListOf<QuestionItem>()
}

data class QuestionItem(
    val id: String,
    val text: String
)



data class FeedbackItem(
    val id: String,
    val studentRef: DocumentReference,
    var regno: String = "",
    var name: String = "",
    var rating: Float = 0f,
    var timestamp: Long = 0L,
    var answers: List<AnswerItem> = emptyList(),
    var expanded: Boolean = false
)

data class AnswerItem(
    val questionRef: DocumentReference,
    val answer: String,
    var questionText: String = ""
)

object AppData {
    val hostelList = mutableListOf<Hostel>()
}

data class Hostel(
    val id: String = "",
    val name: String = "",
    val type: String = ""
)

// ───────────────────────────────────────────────
// USER SESSION OBJECT
// ───────────────────────────────────────────────
object UserSession {
    var userType: String = ""    // "admin" or "student"
    var userId: String = ""
    var mess: String = ""        // hostel doc id
    var name: String = ""
}

// ───────────────────────────────────────────────
// MAIN ACTIVITY
// ───────────────────────────────────────────────
class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val flag = findViewById<ImageView>(R.id.loginStatusIcons)
        flag.setOnClickListener { auth.signOut()
            Toast.makeText(this, "signedout", Toast.LENGTH_SHORT).show()
        }
        // ⭐ First load all hostels
        loadHostels {
            // ⭐ Then detect logged-in user type
            checkUserRole { success ->
                if (success) {
                    // Load correct home fragment
                    supportFragmentManager.commit {
                        if (UserSession.userType == "admin") {
                            replace(R.id.fragment_loader, AdminHomeFragment())
                        } else {
                            replace(R.id.fragment_loader, StudentHomeFragment())
                        }
                    }
                } else {
                    // No user found → open register screen
                    openRegisterFragment()
//                    openLoginFragment()
                }
            }
        }
    }

    // ───────────────────────────────────────────────
    // OPEN REGISTER FRAGMENT
    // ───────────────────────────────────────────────
    private fun openRegisterFragment() {
        supportFragmentManager.commit {
            replace(R.id.fragment_loader, RegisterFragment())
        }
    }
    private fun openLoginFragment() {
        supportFragmentManager.commit {
            replace(R.id.fragment_loader, LoginFragment())
        }
    }


    // ───────────────────────────────────────────────
    // LOAD HOSTELS LIST FROM FIRESTORE
    // ───────────────────────────────────────────────
    private fun loadHostels(callback: () -> Unit) {
        db.collection("hostels")
            .get()
            .addOnSuccessListener { result ->
                AppData.hostelList.clear()

                for (doc in result) {
                    val name = doc.getString("hostel") ?: continue
                    val type = doc.getString("type") ?: ""
                    val id = doc.id

                    AppData.hostelList.add(Hostel(id, name, type))
                }

                callback()  // proceed
            }
            .addOnFailureListener {
                callback()
            }
    }

    // ───────────────────────────────────────────────
    // CHECK IF USER IS ADMIN OR STUDENT
    // ───────────────────────────────────────────────
    private fun checkUserRole(callback: (Boolean) -> Unit) {

        val currentUser = auth.currentUser ?: return callback(false)
        val uid = currentUser.uid

        // First check admins
        db.collection("admins").document(uid)
            .get()
            .addOnSuccessListener { adminDoc ->
                if (adminDoc.exists()) {
                    // user is admin
                    fetchAdminDoc(uid) { ok ->
                        callback(ok)
                    }
                } else {
                    // Else check students
                    db.collection("students").document(uid)
                        .get()
                        .addOnSuccessListener { studentDoc ->
                            if (studentDoc.exists()) {
                                fetchStudentDoc(uid) { ok ->
                                    callback(ok)
                                }
                            } else {
                                callback(false)
                            }
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    // ───────────────────────────────────────────────
    // FETCH ADMIN FIRESTORE DOCUMENT
    // returns ok=true if user doc is valid and session populated
    // otherwise returns ok=false (and signs out)
    // ───────────────────────────────────────────────
    private fun fetchAdminDoc(uid: String, callback: (Boolean) -> Unit) {

        db.collection("admins").document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val messRef = doc.getDocumentReference("mess")
                if (messRef == null) {
                    // invalid data in firestore — sign out and force login
                    Toast.makeText(this, "Admin record missing mess reference. Please login again.", Toast.LENGTH_LONG).show()
                    auth.signOut()
                    callback(false)
                    return@addOnSuccessListener
                }

                UserSession.userType = "admin"
                UserSession.userId = uid
                UserSession.name = doc.getString("name") ?: ""
                UserSession.mess = messRef.id

                callback(true)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading admin data", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    // ───────────────────────────────────────────────
    // FETCH STUDENT FIRESTORE DOCUMENT
    // returns ok=true if user doc is valid and session populated
    // otherwise returns ok=false (and signs out)
    // ───────────────────────────────────────────────
    private fun fetchStudentDoc(uid: String, callback: (Boolean) -> Unit) {

        db.collection("students").document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val messRef = doc.getDocumentReference("mess")
                if (messRef == null) {
                    // invalid data in firestore — sign out and force login
                    Toast.makeText(this, "Student record missing mess reference. Please login again.", Toast.LENGTH_LONG).show()
                    auth.signOut()
                    callback(false)
                    return@addOnSuccessListener
                }

                UserSession.userType = "student"
                UserSession.userId = uid
                UserSession.name = doc.getString("name") ?: ""
                UserSession.mess = messRef.id

                callback(true)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading student data", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

}
