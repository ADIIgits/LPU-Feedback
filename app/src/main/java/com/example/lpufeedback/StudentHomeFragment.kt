package com.example.lpufeedback

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StudentHomeFragment : Fragment() {

    private lateinit var datePickerText: TextView
    private lateinit var questionsContainer: LinearLayout
    private lateinit var ratingSeekbar: SeekBar
    private lateinit var ratingDescription: TextView
    private lateinit var submitBtn: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_student_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentFragmentManager.setFragmentResult(
            "updateTitle",
            bundleOf("title" to "Student")
        )


        setupViews(view)
        setupDatePicker()
        setupRatingSeekBar()

        // Load questions then show them
        loadQuestions {
            displayQuestions()
        }
    }

    // ----------------------------------------------------------
    // VIEW SETUP
    // ----------------------------------------------------------
    private fun setupViews(view: View) {
        datePickerText = view.findViewById(R.id.datePickerText)
        questionsContainer = view.findViewById(R.id.allquestionscontainer)
        ratingSeekbar = view.findViewById(R.id.ratingSeekbar)
        ratingDescription = view.findViewById(R.id.ratingDescription)
        submitBtn = view.findViewById(R.id.submitBtn)

        submitBtn.setOnClickListener {
            handleSubmit()
        }

    }
    private fun handleSubmit() {

        val hostelId = UserSession.mess
        val studentId = UserSession.userId

        if (hostelId.isEmpty() || studentId.isEmpty()) {
            Toast.makeText(requireContext(), "Session error", Toast.LENGTH_SHORT).show()
            return
        }

        // ---- 1) Parse DATE to Timestamp ----
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val date = sdf.parse(datePickerText.text.toString())
        val timestamp = com.google.firebase.Timestamp(date!!)

        // ---- 2) Rating ----
        val ratingValue = ratingSeekbar.progress

        // ---- 3) Student reference ----
        val studentRef = db.collection("students").document(studentId)

        // ---- 4) Create feedback map ----
        val feedbackData = hashMapOf(
            "date" to timestamp,
            "rating" to ratingValue.toDouble(),
            "student" to studentRef
        )

        // ---- 5) Add feedback to all_feedbacks ----
        val feedbackRef = db.collection("hostels")
            .document(hostelId)
            .collection("all_feedbacks")
            .document()       // auto ID

        feedbackRef.set(feedbackData)
            .addOnSuccessListener {
                uploadAnswers(feedbackRef)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to submit feedback", Toast.LENGTH_SHORT).show()
            }
    }
    private fun uploadAnswers(feedbackRef: com.google.firebase.firestore.DocumentReference) {

        val hostelId = UserSession.mess
        val answersColl = feedbackRef.collection("answers")

        val total = questionsContainer.childCount
        var done = 0

        for (i in 0 until total) {

            val view = questionsContainer.getChildAt(i)
            val qId = view.tag as String   // question doc id
            val answerField = view.findViewById<EditText>(R.id.answerfield)
            val userAnswer = answerField.text.toString()

            if (userAnswer.isEmpty()) {
                done++
                if (done == total) afterSubmit()
                continue
            }

            val qRef = db.collection("hostels")
                .document(hostelId)
                .collection("questions")
                .document(qId)

            val answerMap = hashMapOf(
                "question" to qRef,
                "answer" to userAnswer
            )

            answersColl.add(answerMap)
                .addOnSuccessListener {
                    done++
                    if (done == total) afterSubmit()
                }
                .addOnFailureListener {
                    done++
                    if (done == total) afterSubmit()
                }
        }
    }
    private fun afterSubmit() {
        Toast.makeText(requireContext(), "Feedback submitted!", Toast.LENGTH_LONG).show()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_loader, SubmissionSuccessFragment())
            .addToBackStack(null)
            .commit()
    }


    // ----------------------------------------------------------
    // DATE PICKER LOGIC
    // ----------------------------------------------------------
    private fun setupDatePicker() {
        datePickerText.setOnClickListener {
            val calendar = Calendar.getInstance()

            val dialog = DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val format = SimpleDateFormat("dd MMM yyyy", Locale.US)
                    val selected = Calendar.getInstance()
                    selected.set(y, m, d)

                    datePickerText.text = format.format(selected.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            dialog.show()
        }
    }

    // ----------------------------------------------------------
    // RATING SEEKBAR LISTENER
    // ----------------------------------------------------------
    private fun setupRatingSeekBar() {
        ratingSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                ratingDescription.text = "Selected: $value"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    // ----------------------------------------------------------
    // LOAD QUESTIONS (TEMP STATIC — FIRESTORE LATER)
    // ----------------------------------------------------------
    private fun loadQuestions(callback: () -> Unit) {

        val hostelId = UserSession.mess      // already stored as hostel doc id
        if (hostelId.isEmpty()) {
            GlobalQuestionsStore.questions.clear()
            callback()
            return
        }

        db.collection("hostels")
            .document(hostelId)
            .collection("questions")
            .get()
            .addOnSuccessListener { snap ->

                GlobalQuestionsStore.questions.clear()

                for (doc in snap) {
                    val qText = doc.getString("question") ?: continue
                    GlobalQuestionsStore.questions.add(
                        QuestionItem(
                            id = doc.id,
                            text = qText
                        )
                    )
                }

                callback()
            }
            .addOnFailureListener {
                GlobalQuestionsStore.questions.clear()
                callback()
            }
    }


    // ----------------------------------------------------------
    // DYNAMICALLY ADD QUESTIONS
    // ----------------------------------------------------------
    private fun displayQuestions() {
        questionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (q in GlobalQuestionsStore.questions) {

            val itemView = inflater.inflate(
                R.layout.question_layout,
                questionsContainer,
                false
            )

            val qText = itemView.findViewById<TextView>(R.id.questiontext)
            qText.text = "Q. ${q.text}"
            itemView.tag = q.id

            questionsContainer.addView(itemView)
        }
    }
}
