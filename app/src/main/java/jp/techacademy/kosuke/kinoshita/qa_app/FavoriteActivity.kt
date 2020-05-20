package jp.techacademy.kosuke.kinoshita.qa_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoriteActivity : AppCompatActivity() {

    private lateinit var mFavoriteRef: DatabaseReference
    private lateinit var mQuestionRef: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private lateinit var mListView: ListView


    private val favoriteEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val questionUid = dataSnapshot.key ?: "" // questionUid
            val genre = map["genre"] ?: ""

            val dataBaseReference = FirebaseDatabase.getInstance().reference
            mQuestionRef = dataBaseReference.child(ContentsPATH).child(genre).child(questionUid)

            mQuestionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = snapshot.value as Map<String, String>
                    val title = map["title"] ?: ""
                    val body = map["body"] ?: ""
                    val name = map["name"] ?: ""
                    val uid = map["uid"] ?: ""
                    val imageString = map["image"] ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }
                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            answerArrayList.add(answer)
                        }
                    }
                    val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                        genre.toInt(), bytes, answerArrayList)
                    mQuestionArrayList.add(question)
                    mAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(firebaseError: DatabaseError) {}
            })
        }
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        }
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
        }
        override fun onCancelled(databaseError: DatabaseError) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        mListView = findViewById(R.id.listView)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView.adapter = mAdapter

        val user = FirebaseAuth.getInstance().currentUser
        val dataBaseReference = FirebaseDatabase.getInstance().reference

        mFavoriteRef = dataBaseReference.child(FavoritePATH).child(user!!.uid)
        mFavoriteRef.addChildEventListener(favoriteEventListener)

    }
}
