package com.example.lpufeedback

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.google.firebase.firestore.FirebaseFirestore

object AppData {
    val hostelList = mutableListOf<Hostel>()
}

data class Hostel(
    val id: String = "",
    val name: String = "",
    val type: String = ""
)


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ⭐ Load hostels first, THEN load fragment
        loadHostels {
            supportFragmentManager.commit {
                replace(R.id.fragment_loader, AdminHomeFragment())
            }
        }

    }

    private fun openRegisterFragment() {
        supportFragmentManager.commit {
            replace(R.id.fragment_loader, RegisterFragment())
        }
    }

    private fun loadHostels(callback: () -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("hostels")
            .get()
            .addOnSuccessListener { result ->
                AppData.hostelList.clear()

                for (doc in result) {
                    val name = doc.getString("hostel") ?: continue
                    val type = doc.getString("type") ?: ""
                    val id = doc.id  // <--- new

                    AppData.hostelList.add(Hostel(id, name, type))
                }


                callback()  // 🔥 Now notify that loading is done
            }
            .addOnFailureListener {
                callback()  // Still continue, maybe show error toast
            }
    }

}
