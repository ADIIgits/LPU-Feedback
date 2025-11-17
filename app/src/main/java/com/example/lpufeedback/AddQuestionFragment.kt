package com.example.lpufeedback

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore


class AddQuestionFragment : Fragment() {

    private lateinit var questionField: EditText
    private lateinit var addButton: Button
    private lateinit var statusIcon: ImageView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_question, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        questionField = view.findViewById(R.id.questionInput)
        addButton = view.findViewById(R.id.addQuestionBtn)
        statusIcon = view.findViewById(R.id.tick)


        addButton.setOnClickListener {
            val text = questionField.text.toString().trim()

            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Firestore upload later
            handleNewQuestion(text)

        }
    }

    private fun handleNewQuestion(question: String) {

        val hostelId = UserSession.mess

        if (hostelId.isEmpty()) {
            Toast.makeText(requireContext(), "Hostel not found", Toast.LENGTH_SHORT).show()
            showTick()
            return
        }

        // 1. Show loader
        showLoading()

        val data = hashMapOf(
            "question" to question
        )

        FirebaseFirestore.getInstance()
            .collection("hostels")
            .document(hostelId)
            .collection("questions")
            .add(data)
            .addOnSuccessListener {
                // 2. Show success icon
                showTickFilled()

                Toast.makeText(requireContext(), "Question added!", Toast.LENGTH_SHORT).show()

                // 3. Delay + navigate back
                statusIcon.postDelayed({
                    requireActivity()
                        .supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_loader, AdminHomeFragment())
                        .commit()
                }, 700)  // Smooth 0.7 sec delay
            }
            .addOnFailureListener {
                showTick()
                Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.loading)
            .into(statusIcon)     // your ImageView beside Add button
    }

    private fun showTick() {
        statusIcon.setImageResource(R.drawable.tick)
    }

    private fun showTickFilled() {
        statusIcon.setImageResource(R.drawable.tickfilled)
    }

}
