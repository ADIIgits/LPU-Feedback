package com.example.lpufeedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var studentBtn: Button
    private lateinit var adminBtn: Button

    private lateinit var usernameBlock: View
    private lateinit var registrationBlock: View

    private lateinit var usernameField: EditText
    private lateinit var registrationField: EditText
    private lateinit var passwordField: EditText

    private lateinit var loginButton: Button
    private lateinit var loginStatusIcon: ImageView

    private var selectedRole = "student" // default

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupClicks()
        selectStudent()
    }

    private fun setupViews(view: View) {

        studentBtn = view.findViewById(R.id.studentloginbtn)
        adminBtn = view.findViewById(R.id.adminloginbtn)

        usernameBlock = view.findViewById(R.id.usernamecontainer)
        registrationBlock = view.findViewById(R.id.registrationcontainer)

        usernameField = view.findViewById(R.id.usernamefield)
        registrationField = view.findViewById(R.id.registrationfield)
        passwordField = view.findViewById(R.id.passwordfield)

        loginButton = view.findViewById(R.id.loginbutton)
        loginStatusIcon = view.findViewById(R.id.loginStatusIcon)
    }

    private fun setupClicks() {

        // Reset status icon on typing — use static tick
        fun resetIcon() = showTick()

        usernameField.addTextChangedListener { resetIcon() }
        registrationField.addTextChangedListener { resetIcon() }
        passwordField.addTextChangedListener { resetIcon() }

        studentBtn.setOnClickListener { selectStudent() }
        adminBtn.setOnClickListener { selectAdmin() }

        loginButton.setOnClickListener {
            showLoading()
            handleLogin()
        }
        loginStatusIcon.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Signed out (testing mode)", Toast.LENGTH_SHORT).show()
            showTick() // reset icon after signout
        }

    }

    // ---------- ICON HELPERS ----------

    private fun showLoading() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.loading) // your GIF
            .into(loginStatusIcon)
    }

    private fun showTick() {
        loginStatusIcon.setImageResource(R.drawable.tick)
    }

    private fun showTickFilled() {
        loginStatusIcon.setImageResource(R.drawable.tickfilled)
    }

    // ---------- ROLE SELECTION ----------

    private fun selectStudent() {
        selectedRole = "student"
        updateVisibility()
        highlightButton(studentBtn, adminBtn)
    }

    private fun selectAdmin() {
        selectedRole = "admin"
        updateVisibility()
        highlightButton(adminBtn, studentBtn)
    }

    private fun updateVisibility() {
        if (selectedRole == "student") {
            registrationBlock.visibility = View.VISIBLE
            usernameBlock.visibility = View.GONE
        } else {
            usernameBlock.visibility = View.VISIBLE
            registrationBlock.visibility = View.GONE
        }
    }

    private fun highlightButton(active: Button, inactive: Button) {
        active.backgroundTintList =
            requireContext().getColorStateList(R.color.selectedblue)
        active.setTextColor(requireContext().getColor(R.color.black))

        inactive.backgroundTintList =
            requireContext().getColorStateList(R.color.black10)
        inactive.setTextColor(requireContext().getColor(R.color.black50))
    }

    // ---------- LOGIN PROCESS ----------

    private fun handleLogin() {
        val password = passwordField.text.toString().trim()

        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Enter password", Toast.LENGTH_SHORT).show()
            showTick()
            return
        }

        if (selectedRole == "student") {
            val regNo = registrationField.text.toString().trim()

            if (regNo.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Registration No", Toast.LENGTH_SHORT).show()
                showTick()
                return
            }

            loginStudent(regNo, password)

        } else {
            val username = usernameField.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(requireContext(), "Enter username", Toast.LENGTH_SHORT).show()
                showTick()
                return
            }

            loginAdmin(username, password)
        }
    }

    //---------------------------------------
    // STUDENT LOGIN
    //---------------------------------------
    private fun loginStudent(regNo: String, password: String) {

        val db = FirebaseFirestore.getInstance()

        db.collection("students")
            .whereEqualTo("registration", regNo)
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "Student not found", Toast.LENGTH_SHORT).show()
                    showTick()
                    return@addOnSuccessListener
                }

                val doc = result.documents[0]
                val email = doc.getString("useremail") ?: ""

                loginWithEmail(email, password)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                showTick()
            }
    }

    //---------------------------------------
    // ADMIN LOGIN
    //---------------------------------------
    private fun loginAdmin(username: String, password: String) {

        val db = FirebaseFirestore.getInstance()

        db.collection("admins")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "Admin not found", Toast.LENGTH_SHORT).show()
                    showTick()
                    return@addOnSuccessListener
                }

                val doc = result.documents[0]
                val email = doc.getString("useremail") ?: ""

                loginWithEmail(email, password)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                showTick()
            }
    }

    //---------------------------------------
    // FIREBASE AUTH LOGIN (COMMON)
    //---------------------------------------
    private fun loginWithEmail(email: String, password: String) {

        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                showTickFilled()
                Toast.makeText(requireContext(), "Logged in!", Toast.LENGTH_SHORT).show()

                // TODO: Navigate to dashboard
            }
            .addOnFailureListener {
                showTick()
                Toast.makeText(requireContext(), "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
