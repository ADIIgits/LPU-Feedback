# 🏫 LPU Feedback

**LPU Feedback** is an Android application designed for **Lovely Professional University (LPU)** to streamline the collection and management of hostel/mess feedback. It features a dual-role system — students submit structured feedback, and admins review and manage it in real time.

---

## ✨ Features

- 👩‍🎓 **Student Role**
  - Pick a feedback date using a built-in date picker
  - Rate the mess/hostel experience on a seekbar (0–10 scale)
  - Answer dynamic questions loaded from Firestore for their specific hostel
  - Submit feedback in one tap — answers saved under their feedback record
  - Confirmation screen shown on successful submission

- 🔑 **Admin Role**
  - View all submitted feedback for their assigned hostel/mess
  - Expandable feedback cards showing student name, registration number, rating, and all Q&A pairs
  - Add custom questions to the hostel's question pool directly from the app

- 🔐 **Authentication** — Secure login and registration via Firebase Auth
- ☁️ **Firebase Firestore** — Cloud-synced storage for hostels, questions, students, and feedback
- 📸 **Glide** — Smooth GIF loading for animated status indicators (loading, tick states)

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Android SDK (API 24+) | Minimum Android 7.0 (Nougat) |
| Firebase Auth | User authentication |
| Firebase Firestore | Cloud database for all feedback data |
| Glide | Image & GIF loading for status icons |
| FlexboxLayout | Flexible UI layouts |
| ViewModel + LiveData | Lifecycle-aware state management |
| Material Design | UI components & theming |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- A Firebase project with **Authentication** and **Firestore** enabled

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/ADIIgits/LPU-Feedback.git
   cd LPU-Feedback
   ```

2. **Add `google-services.json`**
   Place your Firebase `google-services.json` file inside `app/`.

3. **Build and run**
   Open the project in Android Studio, sync Gradle, and run on a device or emulator.

---

## 📱 App Flow

```
Launch App
    ├── Login / Register
    └── Logged In
           ├── 👩‍🎓 Student
           │     ├── Pick date
           │     ├── Set rating (SeekBar 0–10)
           │     ├── Answer dynamic hostel questions
           │     └── Submit → Success Screen
           └── 🔑 Admin
                 ├── View all feedback (expandable cards)
                 │     └── Card → Student name, reg no, rating, Q&A
                 └── Add new questions → pushed to hostel's question pool
```

---

## 🗂️ Firestore Data Model

```
hostels/{hostelId}
    ├── questions/{questionId}
    │       └── question: "How was the food quality?"
    └── all_feedbacks/{feedbackId}
            ├── date (Timestamp)
            ├── rating (Double)
            ├── student (ref → students/{studentId})
            └── answers/{answerId}
                    ├── question (ref → questions/{questionId})
                    └── answer: "It was good"

students/{studentId}
    ├── name
    └── registration
```

---

## 📂 Project Structure

```
app/src/main/java/com/example/lpufeedback/
├── MainActivity.kt               # Host activity, fragment navigation
├── LoginFragment.kt              # Firebase login screen
├── RegisterFragment.kt           # User registration screen
├── StudentHomeFragment.kt        # Feedback form for students
├── AdminHomeFragment.kt          # Feedback viewer + admin dashboard
├── AddQuestionFragment.kt        # Admin: add new questions to hostel
├── SubmissionSuccessFragment.kt  # Confirmation screen after submission
└── UserSessionfile.kt            # Holds active session data (UserSession, AppData, etc.)
```

---

## 📄 License

This project is for personal/educational use. Feel free to fork and build upon it!
