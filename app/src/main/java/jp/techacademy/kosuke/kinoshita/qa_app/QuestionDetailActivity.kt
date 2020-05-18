package jp.techacademy.kosuke.kinoshita.qa_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ListView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_detail.*

import java.util.HashMap
import kotlin.properties.Delegates
import com.google.firebase.database.ValueEventListener
import android.support.v4.app.SupportActivity
import android.support.v4.app.SupportActivity.ExtraData
import android.support.v4.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class QuestionDetailActivity : AppCompatActivity(), View.OnClickListener {

    // プロパティ
    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference

    private var favFrag by Delegates.notNull<Boolean>()


    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
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
//    private val mFavoriteListener = object : ChildEventListener {
//        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
//            val favoriteQuestionUid = dataSnapshot.key ?: ""
//            if(favoriteQuestionUid == mQuestion.questionUid){
//                favFrag = true
//            }
//        }
//
//        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
//
//        }
//
//        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
//            val favoriteQuestionUid = dataSnapshot.key ?: ""
//            if(favoriteQuestionUid == mQuestion.questionUid){
//                favFrag = false
//            }
//        }
//
//        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
//
//        }
//
//        override fun onCancelled(databaseError: DatabaseError) {
//
//        }
//    }
    private var postListener: ValueEventListener = object : ValueEventListener {
        @SuppressLint("RestrictedApi")
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if(dataSnapshot.value != null) {
                favFrag = true
                favorite.visibility = View.VISIBLE
                nofavorite.visibility = View.GONE

            }else{
                favFrag = false
                favorite.visibility = View.GONE
                nofavorite.visibility = View.VISIBLE

            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser
        mFavoriteRef = dataBaseReference.child(FavoritePATH).child(user!!.uid)

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // --- ここまで ---
            }
        }
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
  //      mFavoriteRef.addChildEventListener(mFavoriteListener)
        mFavoriteRef.child(mQuestion.questionUid).addValueEventListener(postListener)

    }

    @SuppressLint("RestrictedApi")
    override fun onClick(v: View) {
        val user = FirebaseAuth.getInstance().currentUser
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mFavoriteRef = dataBaseReference.child(FavoritePATH).child(user!!.uid)
        val data = HashMap<String, String>()

        // mFavoriteListneがtrue
        if(favFrag){
            // お気に入り登録がされていればお気に入りから削除
            mFavoriteRef.child(mQuestion.questionUid).removeValue()
            nofavorite.visibility = View.VISIBLE
            favorite.visibility = View.GONE

        }else{
            // お気に入り登録がされていなければお気に入りへ登録
            data["genre"] = mQuestion.genre.toString()
            mFavoriteRef.child(mQuestion.questionUid).setValue(data)
            favorite.visibility = View.VISIBLE
            nofavorite.visibility = View.GONE

        }
    }

    @SuppressLint("RestrictedApi")
    override fun onResume() {
        super.onResume()
        val user = FirebaseAuth.getInstance().currentUser

        // ログインしていなければお気に入りボタンを非表示
        if (user == null) {
            favorite.visibility = View.GONE
            nofavorite.visibility = View.GONE
        }else{
            favorite.setOnClickListener(this)
            nofavorite.setOnClickListener(this)
        }
    }
}