package com.example.lpufeedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.text.Editable
import android.text.TextWatcher
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide


class RegisterFragment : Fragment() {

    // -----------------------------
    // UI elements
    // -----------------------------
    private lateinit var btnStudent: Button
    private lateinit var btnAdmin: Button
    private lateinit var btnRegister: Button

    private lateinit var usernameBlock: View
    private lateinit var registrationBlock: View

    private lateinit var usernameField: EditText
    private lateinit var registrationField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var nameField: EditText

    private lateinit var btnBoys: Button
    private lateinit var btnGirls: Button

    private lateinit var messContainer: ViewGroup

    // -----------------------------
    // State variables
    // -----------------------------
    private var selectedRole = "student"
    private var selectedGender = "boys"
    private var selectedMess: String? = null // stores hostel doc ID

    private lateinit var checkUsernameBtn: ImageView
    private var isUsernameAvailable = false
    private lateinit var checkRegNoBtn: ImageView
    private var isRegNoAvailable = false
    private lateinit var registerStatusIcon: ImageView





    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupClicks()

        selectStudent() // default
    }


    private fun setupViews(view: View) {

        btnStudent = view.findViewById(R.id.studentbutton)
        btnAdmin = view.findViewById(R.id.adminbutton)
        btnRegister = view.findViewById(R.id.registerbutton)

        usernameBlock = view.findViewById(R.id.usernameandfield)
        registrationBlock = view.findViewById(R.id.registrationandfield)

        usernameField = view.findViewById(R.id.usernamefield)
        registrationField = view.findViewById(R.id.registrationfield)
        emailField = view.findViewById(R.id.emailfield)
        passwordField = view.findViewById(R.id.passwordfield)
        nameField = view.findViewById(R.id.namefield)

        btnBoys = view.findViewById(R.id.boysbutton)
        btnGirls = view.findViewById(R.id.girlsbutton)

        messContainer = view.findViewById(R.id.allmesscontainer)

        checkUsernameBtn = view.findViewById(R.id.checkUsernameBtn)
        checkRegNoBtn = view.findViewById(R.id.checkRegNoBtn)
        registerStatusIcon = view.findViewById(R.id.registerStatusIcon)



    }


    private fun setupClicks() {

        fun resetRegisterIcon() {
            registerStatusIcon.setImageResource(R.drawable.tick)
        }

        emailField.addTextChangedListener { resetRegisterIcon() }
        passwordField.addTextChangedListener { resetRegisterIcon() }
        nameField.addTextChangedListener { resetRegisterIcon() }
        registrationField.addTextChangedListener { resetRegisterIcon() }
        usernameField.addTextChangedListener { resetRegisterIcon() }



        usernameField.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                isUsernameAvailable = false
                checkUsernameBtn.setImageResource(R.drawable.tick) // reset icon
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        registrationField.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                isRegNoAvailable = false
                checkRegNoBtn.setImageResource(R.drawable.tick) // reset icon
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        btnStudent.setOnClickListener { selectStudent() }
        btnAdmin.setOnClickListener { selectAdmin() }

        btnBoys.setOnClickListener {
            selectedGender = "boys"
            highlightButton(btnBoys, btnGirls)
            renderMessButtons()
            Toast.makeText(requireContext(), "Showing boys hostels", Toast.LENGTH_SHORT).show()
        }

        btnGirls.setOnClickListener {
            selectedGender = "girls"
            highlightButton(btnGirls, btnBoys)
            renderMessButtons()
            Toast.makeText(requireContext(), "Showing girls hostels", Toast.LENGTH_SHORT).show()
        }

        renderMessButtons()

        btnRegister.setOnClickListener {
            loadGifInto(registerStatusIcon, R.drawable.loading)
            onRegisterClick()
        }
        checkUsernameBtn.setOnClickListener { checkUsernameAvailability() }
        checkRegNoBtn.setOnClickListener { checkRegNoAvailability() }


    }
    private fun checkRegNoAvailability() {
        val regNo = registrationField.text.toString().trim()

        if (regNo.isEmpty()) {
            Toast.makeText(requireContext(), "Enter Registration No first", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("students")
            .whereEqualTo("registration", regNo)
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    isRegNoAvailable = true
                    checkRegNoBtn.setImageResource(R.drawable.tickfilled)
                    Toast.makeText(requireContext(), "Registration number available!", Toast.LENGTH_SHORT).show()
                } else {
                    isRegNoAvailable = false
                    checkRegNoBtn.setImageResource(R.drawable.tick)
                    Toast.makeText(requireContext(), "Registration number already exists", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUsernameAvailability() {
        val username = usernameField.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Enter username first", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        // Query admins collection for same username
        db.collection("admins")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    // USERNAME AVAILABLE
                    isUsernameAvailable = true
                    checkUsernameBtn.setImageResource(R.drawable.tickfilled)
                    Toast.makeText(requireContext(), "Username available!", Toast.LENGTH_SHORT).show()
                } else {
                    // USERNAME TAKEN
                    isUsernameAvailable = false
                    checkUsernameBtn.setImageResource(R.drawable.tick)
                    Toast.makeText(requireContext(), "Username already exists", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun selectStudent() {
        selectedRole = "student"
        updateVisibility()
        highlightButton(btnStudent, btnAdmin)
    }

    private fun selectAdmin() {
        selectedRole = "admin"
        updateVisibility()
        highlightButton(btnAdmin, btnStudent)
    }

    private fun updateVisibility() {
        if (selectedRole == "student") {
            registrationBlock.visibility = View.VISIBLE
            usernameBlock.visibility = View.GONE
        } else {
            registrationBlock.visibility = View.GONE
            usernameBlock.visibility = View.VISIBLE
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


    // -----------------------------
    // MESS BUTTONS (hostels)
    // -----------------------------
    private fun renderMessButtons() {
        messContainer.removeAllViews()

        val filteredList = AppData.hostelList.filter { it.type.equals(selectedGender, true) }

        if (filteredList.isEmpty()) {
            Toast.makeText(requireContext(), "No hostels found for $selectedGender", Toast.LENGTH_SHORT).show()
        }

        for (hostel in filteredList) {
            val btn = Button(requireContext()).apply {
                text = hostel.name
                setPadding(32, 12, 32, 12)
                background = requireContext().getDrawable(R.drawable.roundedcorner)
                backgroundTintList = requireContext().getColorStateList(R.color.black10)
                textSize = 18f
            }

            // apply selected style if matches
            if (hostel.id == selectedMess) {
                btn.backgroundTintList = requireContext().getColorStateList(R.color.selectedblue)
                btn.setTextColor(requireContext().getColor(R.color.black))
            }

            btn.setOnClickListener {
                selectedMess = hostel.id
                highlightSelectedMessButton(btn)
                Toast.makeText(requireContext(), "Selected: ${hostel.name}", Toast.LENGTH_SHORT).show()
            }

            messContainer.addView(btn)
        }

    }

    private fun highlightSelectedMessButton(selectedBtn: Button) {
        for (i in 0 until messContainer.childCount) {
            val child = messContainer.getChildAt(i)
            if (child is Button) {
                if (child == selectedBtn) {
                    child.backgroundTintList =
                        requireContext().getColorStateList(R.color.selectedblue)
                    child.setTextColor(requireContext().getColor(R.color.black))
                } else {
                    child.backgroundTintList =
                        requireContext().getColorStateList(R.color.black10)
                    child.setTextColor(requireContext().getColor(R.color.black80))
                }
            }
        }
    }


    // -----------------------------
    // REGISTER CLICK
    // -----------------------------
    private fun onRegisterClick() {

        loadGifInto(registerStatusIcon, R.drawable.loading)


        val name = nameField.text.toString().trim()
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        if (selectedRole == "student") {
            val regNo = registrationField.text.toString().trim()

            if (regNo.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Registration No", Toast.LENGTH_SHORT).show()
                return
            }

            handleStudentRegistration(regNo, name, selectedGender, email, password, selectedMess)

        } else {
            val username = usernameField.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Username", Toast.LENGTH_SHORT).show()
                return
            }

            handleAdminRegistration(username, name, selectedGender, email, password, selectedMess)
        }
    }


    // -----------------------------
    // ADMIN REGISTRATION WITH AUTH
    // -----------------------------
    private fun handleAdminRegistration(
        username: String,
        name: String,
        gender: String,
        email: String,
        password: String,
        mess: String?
    ) {
        // require username verification
        if (!isUsernameAvailable) {
            Toast.makeText(requireContext(), "Please verify username availability (press the tick).", Toast.LENGTH_SHORT).show()
            return
        }

        val error = validateAdminInput(username, name, gender, email, password, mess)
        if (error != null) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // 1️⃣ Create user in Firebase Authentication
        btnRegister.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->

                val uid = authResult.user!!.uid
                val adminRef = db.collection("admins").document(uid)
                val hostelRef = db.collection("hostels").document(mess!!)

                val adminData = hashMapOf(
                    "uid" to uid,
                    "username" to username,
                    "name" to name,
                    "sex" to gender,
                    "useremail" to email,
                    "mess" to hostelRef  // reference
                )

                // 2️⃣ Save admin profile in Firestore
                adminRef.set(adminData)
                    .addOnSuccessListener {

                        // 3️⃣ Add admin reference inside hostel -> all_admins
                        hostelRef.update(
                            "all_admins",
                            com.google.firebase.firestore.FieldValue.arrayUnion(adminRef)
                        ).addOnSuccessListener {
                            Toast.makeText(requireContext(), "Admin linked to hostel!", Toast.LENGTH_SHORT).show()
                            navigateToAdminDashboard()
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to update hostel: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
                        registerStatusIcon.setImageResource(R.drawable.tick)

                    }
                btnRegister.isEnabled = true
                registerStatusIcon.setImageResource(R.drawable.tickfilled)
            }
            .addOnFailureListener { e ->
                btnRegister.isEnabled = true
                Toast.makeText(requireContext(), "Auth failed: ${e.message}", Toast.LENGTH_LONG).show()
                registerStatusIcon.setImageResource(R.drawable.tick)

            }
    }



    private fun handleStudentRegistration(
        regNo: String,
        name: String,
        gender: String,
        email: String,
        password: String,
        mess: String?
    ) {
        if (!isRegNoAvailable) {
            Toast.makeText(requireContext(), "Verify Registration No first", Toast.LENGTH_SHORT).show()
            return
        }

        val error = validateStudentInput(regNo, name, email, password, mess)
        if (error != null) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->

                val uid = authResult.user!!.uid
                val studentRef = db.collection("students").document(uid)
                val hostelRef = db.collection("hostels").document(mess!!)

                val studentData = hashMapOf(
                    "uid" to uid,
                    "registration" to regNo,
                    "name" to name,
                    "sex" to gender,
                    "useremail" to email,
                    "mess" to hostelRef
                )
                studentRef.set(studentData)
                    .addOnSuccessListener {

                        hostelRef.update(
                            "all_students",
                            com.google.firebase.firestore.FieldValue.arrayUnion(studentRef)
                        ).addOnSuccessListener {
                            Toast.makeText(requireContext(), "Student registered!", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to update hostel: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
                        registerStatusIcon.setImageResource(R.drawable.tick)

                    }
                registerStatusIcon.setImageResource(R.drawable.tickfilled)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Auth failed: ${e.message}", Toast.LENGTH_LONG).show()
                registerStatusIcon.setImageResource(R.drawable.tick)

            }
    }
    private fun validateStudentInput(
        regNo: String,
        name: String,
        email: String,
        password: String,
        mess: String?
    ): String? {

        if (regNo.isBlank()) return "Registration number required"
        if (name.isBlank()) return "Name required"
        if (email.isBlank()) return "Email required"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Invalid email"

        if (password.length < 8) return "Password must be 8+ chars"
        if (!password.any { it.isUpperCase() }) return "Add uppercase letter"
        if (!password.any { it.isLowerCase() }) return "Add lowercase letter"
        if (!password.any { it.isDigit() }) return "Add a number"

        if (mess.isNullOrBlank()) return "Select a mess"

        return null
    }



    // -----------------------------
    // VALIDATION
    // -----------------------------
    private fun validateAdminInput(
        username: String,
        name: String,
        gender: String,
        email: String,
        password: String,
        mess: String?
    ): String? {

        if (username.isBlank()) return "Username required"
        if (name.isBlank()) return "Name required"
        if (email.isBlank()) return "Email required"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Invalid email"

        if (password.length < 8) return "Password must be 8+ chars"
        if (!password.any { it.isUpperCase() }) return "Add uppercase letter"
        if (!password.any { it.isLowerCase() }) return "Add lowercase letter"
        if (!password.any { it.isDigit() }) return "Add a number"

        if (mess.isNullOrBlank()) return "Select a mess"

        return null
    }
    private fun loadGifInto(imageView: ImageView, gifRes: Int) {
        Glide.with(this)
            .asGif()
            .load(gifRes)
            .into(imageView)
    }



    private fun navigateToAdminDashboard() {
        Toast.makeText(requireContext(), "Navigating to dashboard...", Toast.LENGTH_SHORT).show()
    }
}
