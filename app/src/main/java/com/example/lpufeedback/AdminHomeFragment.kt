package com.example.lpufeedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf

class AdminHomeFragment : Fragment() {

    private lateinit var feedbackContainer: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_home, container, false)
        feedbackContainer = view.findViewById(R.id.feedbackscontainer)

        parentFragmentManager.setFragmentResult(
            "updateTitle",
            bundleOf("title" to "Admin")
        )

        val createBtn = view?.findViewById<ConstraintLayout>(R.id.constraintLayout)
        createBtn?.setOnClickListener {
            openAddQuestionScreen()
        }

        loadAdminFeedbacks()


        return view
    }

    // ------------------------------------------------------
    // MAIN: Fetch feedbacks for this admin's mess
    // ------------------------------------------------------
    private fun loadAdminFeedbacks() {
        feedbackContainer.removeAllViews()

        val adminMess = UserSession.mess
        val hostel = AppData.hostelList.firstOrNull { it.id == adminMess }

        if (hostel == null) {
            showEmptyMessage("Hostel not found for $adminMess")
            return
        }

        val hostelId = hostel.id

        db.collection("hostels")
            .document(hostelId)
            .collection("all_feedbacks")
            .get()
            .addOnSuccessListener { result ->

                GlobalFeedbackStore.feedbackList.clear()

                for (doc in result) {
                    val studentRef = doc.getDocumentReference("student") ?: continue
                    val rating = doc.getDouble("rating")?.toFloat() ?: 0f
                    val timestamp = doc.getTimestamp("date")?.seconds ?: 0L

                    GlobalFeedbackStore.feedbackList.add(
                        FeedbackItem(
                            id = doc.id,
                            studentRef = studentRef,
                            rating = rating,
                            timestamp = timestamp
                        )
                    )
                }

                loadAllStudentDetails(hostelId)
            }
            .addOnFailureListener {
                showEmptyMessage("Failed to load feedbacks.")
            }
    }

    // ------------------------------------------------------
    // STEP 1: Load student details
    // ------------------------------------------------------
    private fun loadAllStudentDetails(hostelId: String) {
        var done = 0
        val total = GlobalFeedbackStore.feedbackList.size

        if (total == 0) {
            displayFeedbacks()
            return
        }

        for (fb in GlobalFeedbackStore.feedbackList) {
            fb.studentRef.get()
                .addOnSuccessListener { doc ->
                    fb.regno = doc.getString("registration") ?: "N/A"
                    fb.name = doc.getString("name") ?: "Unknown"

                    done++
                    if (done == total) {
                        loadAllAnswers(hostelId)
                    }
                }
        }
    }

    // ------------------------------------------------------
    // STEP 2: Load answers for each feedback
    // ------------------------------------------------------
    private fun loadAllAnswers(hostelId: String) {
        var loadedCount = 0
        val feedbacks = GlobalFeedbackStore.feedbackList

        if (feedbacks.isEmpty()) {
            displayFeedbacks()
            return
        }

        for (fb in feedbacks) {

            db.collection("hostels")
                .document(hostelId)
                .collection("all_feedbacks")
                .document(fb.id)
                .collection("answers")
                .get()
                .addOnSuccessListener { answersSnap ->

                    val tempList = mutableListOf<AnswerItem>()

                    for (ans in answersSnap) {
                        val ansText = ans.getString("answer") ?: ""
                        val qRef = ans.getDocumentReference("question") ?: continue

                        tempList.add(
                            AnswerItem(
                                questionRef = qRef,
                                answer = ansText
                            )
                        )
                    }

                    fb.answers = tempList
                    loadedCount++

                    if (loadedCount == feedbacks.size) {
                        resolveQuestions()
                    }
                }
        }
    }

    // ------------------------------------------------------
    // STEP 3: Resolve question texts
    // ------------------------------------------------------
    private fun resolveQuestions() {
        var resolved = 0
        var total = 0

        for (fb in GlobalFeedbackStore.feedbackList) {
            total += fb.answers.size
        }

        if (total == 0) {
            displayFeedbacks()
            return
        }

        for (fb in GlobalFeedbackStore.feedbackList) {
            for (ans in fb.answers) {
                ans.questionRef.get()
                    .addOnSuccessListener { qDoc ->
                        ans.questionText = qDoc.getString("question") ?: "Question"
                        resolved++

                        if (resolved == total) {
                            displayFeedbacks()
                        }
                    }
            }
        }
    }

    // ------------------------------------------------------
    // STEP 4: Build collapsed cards
    // ------------------------------------------------------
    private fun displayFeedbacks() {
        feedbackContainer.removeAllViews()

        for (fb in GlobalFeedbackStore.feedbackList) {

            val itemView = layoutInflater.inflate(
                R.layout.feedbacks_layout,
                feedbackContainer,
                false
            )

            val regTv = itemView.findViewById<TextView>(R.id.registrationnumber)
            val nameTv = itemView.findViewById<TextView>(R.id.name)
            val ratingTv = itemView.findViewById<TextView>(R.id.rating)
            val expandedHolder = itemView.findViewById<LinearLayout>(R.id.expandedHolder)
            val clickableBox = itemView.findViewById<ConstraintLayout>(R.id.feedbackBox)

            regTv.text = fb.regno
            nameTv.text = fb.name
            ratingTv.text = fb.rating.toString()

            expandedHolder.visibility = View.GONE



            clickableBox.setOnClickListener {
                fb.expanded = !fb.expanded

                if (fb.expanded) showExpandedCard(expandedHolder, fb)
                else expandedHolder.visibility = View.GONE
            }

            feedbackContainer.addView(itemView)
        }
    }
    private fun openAddQuestionScreen() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_loader, AddQuestionFragment())
            .addToBackStack(null)
            .commit()
    }


    // ------------------------------------------------------
    // STEP 5: Build expanded card (question + answer)
    // ------------------------------------------------------
    private fun showExpandedCard(holder: LinearLayout, fb: FeedbackItem) {
        holder.removeAllViews()

        val card = layoutInflater.inflate(
            R.layout.feedback_expanded_layout,
            holder,
            false
        )

        val content = card.findViewById<LinearLayout>(R.id.expandedContent)

        for (ans in fb.answers) {
            val tv = TextView(requireContext())
            tv.text = "Q: ${ans.questionText}\nA: ${ans.answer}"
            tv.textSize = 16f
            tv.setPadding(0, 6, 0, 6)

            content.addView(tv)
        }

        holder.addView(card)
        holder.visibility = View.VISIBLE
    }

    // ------------------------------------------------------
    // Empty state
    // ------------------------------------------------------
    private fun showEmptyMessage(msg: String) {
        feedbackContainer.removeAllViews()
        val tv = TextView(requireContext())
        tv.text = msg
        tv.textSize = 16f
        tv.setPadding(8, 8, 8, 8)
        feedbackContainer.addView(tv)
    }
}
