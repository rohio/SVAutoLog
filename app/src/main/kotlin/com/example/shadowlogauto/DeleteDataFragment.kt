package com.example.shadowlogauto

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.example.shadowlogauto.database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [DeleteDataFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [DeleteDataFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class DeleteDataFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var mContext : Context

    private lateinit var mBeginDate : Button
    private lateinit var mEndDate : Button

    private lateinit var mMyClassRadioGroup : RadioGroup
    private lateinit var mMyClassRadioButton : RadioButton
    private lateinit var mOpponentClassRadioGroup : RadioGroup
    private lateinit var mOpponentClassRadioButton : RadioButton

    private lateinit var mDbHelper: DatabaseHelper
    private lateinit var db : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        SQLiteを使ってDB管理
        mDbHelper = DatabaseHelper(mContext)
        db = mDbHelper.writableDatabase
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_delete_data, container, false)

/*        DeleteDataFragmentの各Viewを取得 */
        mBeginDate = view.findViewById(R.id.button_begin_date_delete) as Button
        mEndDate = view.findViewById(R.id.button_end_date_delete) as Button

        mMyClassRadioGroup = view.findViewById(R.id.my_class_delete_radio_group) as RadioGroup
        mOpponentClassRadioGroup = view.findViewById(R.id.opponent_class_delete_radio_group) as RadioGroup

        /*        日付を入力する時に使用するinnerクラス
                  DiarogFragmentで日付を入力できるカレンダーを表示する  */
        class DateDialogFragment(val button: Button) : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                val calendar = Calendar.getInstance()
                return DatePickerDialog(
                        mContext,
//                        Lollipop の場合は Holo テーマのダイアログにする
//                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) AlertDialog.THEME_HOLO_LIGHT else
                        theme,
/*                        DiarogFragmentで日付を選択し、OKを押したときの処理 */
                        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                            val date = Calendar.getInstance()
                            date.set(year, month, dayOfMonth)
                            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val str = df.format(date.time)
//                            DiarogFragmentを呼び出したボタンのテキストに日付をセット
                            button.text = str
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH))
                        .also {
                            //                            選択可能な年月日の上限を指定
                            it.datePicker.maxDate = calendar.timeInMillis
//                            選択可能な年月日の下限を指定
                            val minDate = Calendar.getInstance()
                            minDate.set(2019, 0, 1)
                            it.datePicker.minDate = minDate.timeInMillis
//                            タイトルが勝手に表示されるのを防ぐために空文字をセット
                            it.setTitle("")
                        }
            }

            /* DiarogFragmentでcancelを押した際の処理 */
            override fun onCancel(dialog: DialogInterface?) {
                super.onCancel(dialog)
                button.text = "指定なし"
            }
        }

        /* 削除するデータの条件が未指定の場合に表示するDialogFragment
         * すべてのデータを削除する旨を表示 */
        class DeleteCheckDialogFragment(val queryGameMap: MutableMap<String, String>, val queryDateMap: MutableMap<String, String>) : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                val builder = AlertDialog.Builder(mContext)
                builder.setMessage("条件が指定されていない場合、すべてのデータを削除しますが、本当に実行しますか？")
                        .setPositiveButton("OK") { _, _ ->
                            // 検索条件(queryMap)を用いて、deleteByQueryを実行し、削除件数を取得
                            val deleteNum = deleteByquery(db,  queryGameMap, queryDateMap)
                            // 1件以上削除した場合
                            if (deleteNum > 0){
                                showToast("指定された条件に合う $deleteNum 件のデータを削除しました。")
                            } else { // 条件に合うデータが0件の場合
                                showToast("指定された条件に合うデータが 0 件でした。条件を確認してください。")
                            }
                        }
                        .setNegativeButton("Cancel") { _, _ ->  }
                return builder.create()
            }
        }

/*        EditTextView以外をタップしたときにソフトキーボードを閉じる */
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN){
                if (v != null) {
                    val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                }
            }
            true
        }

/*        日付を指定するボタンを押したとき */
        mBeginDate.setOnClickListener {
//            DialogFragmentを生成し、表示
//            引数にButtonを渡し、そのButton.textに日付をセット
            DateDialogFragment(it as Button).show(activity?.supportFragmentManager, it::class.java.simpleName)
        }
        mEndDate.setOnClickListener {
            DateDialogFragment(it as Button).show(activity?.supportFragmentManager, it::class.java.simpleName)
        }

/*        削除ボタンを押したとき */
        val mDelete = view.findViewById(R.id.delete_search_button) as Button
        mDelete.setOnClickListener{
            //            自分のクラス・デッキ、相手のクラス・デッキの検索条件を格納するMap
//            key:DBのカラム、value:格納するフィールド
            val queryGameMap = mutableMapOf<String, String>()

//            自分のクラスをqueryGameMapに格納
            mMyClassRadioButton = view.findViewById(mMyClassRadioGroup.checkedRadioButtonId) as RadioButton
            val myClassStr = conversionString(mMyClassRadioButton.id)
//            自分のクラスの検索条件が指定されている場合、queryGameMapに追加
            if(!TextUtils.isEmpty(myClassStr)){
                queryGameMap["my_class"] = myClassStr
            }

//            自分のデッキをqueryGameMapに格納
            val myDeck = (view.findViewById(R.id.edit_my_deck_delete) as TextView).text.toString()
            if(!TextUtils.isEmpty(myDeck)){
                queryGameMap["my_deck"] = myDeck
            }

//            相手のクラスをqueryGameMapに追加
            mOpponentClassRadioButton = view.findViewById(mOpponentClassRadioGroup.checkedRadioButtonId) as RadioButton
            val opponentClassStr = conversionString(mOpponentClassRadioButton.id)
//            相手のクラスの検索条件が指定されている場合、queryGameMapに追加
            if(!TextUtils.isEmpty(opponentClassStr)){
                queryGameMap["opponent_class"] = opponentClassStr
            }

//            相手のデッキをqueryGameMapに追加
            val oppoonentDeck = (view.findViewById(R.id.edit_opponent_deck_delete) as TextView).text.toString()
            if(!TextUtils.isEmpty(oppoonentDeck)){
                queryGameMap["opponent_deck"] = oppoonentDeck
            }

//            日時範囲(開始、終了)の検索条件を格納するMap
            val queryDateMap = mutableMapOf<String, String>()

//            日時範囲(開始)をqueryDateMapに追加
            val beginDate = mBeginDate.text.toString()
            if(beginDate != "指定なし"){
//                入力値は日付のみで、DBは日時なので" 00:00"を付けたものをvalueとする
                queryDateMap["begin_date"] = "$beginDate 00:00"
            }

//            日時範囲(終了)をqueryDateMapに追加
            val endDate = mEndDate.text.toString()
            if(endDate != "指定なし"){
//                入力値は日付のみで、DBは日時なので" 23:59"を付けたものをvalueとする
                queryDateMap["end_date"] = "$endDate 23:59"
            }

            if (queryGameMap.isEmpty() && queryDateMap.isEmpty()){
                val deleteCheckDialogFragment = DeleteCheckDialogFragment(queryGameMap, queryDateMap)
                deleteCheckDialogFragment.show(fragmentManager, "DeleteCheckDialogFragment")
            } else {
//                検索条件(queryMap)を用いて、deleteByqueryを実行し、削除件数を取得
                val deleteNum = deleteByquery(db, queryGameMap, queryDateMap)

//                1件以上削除した場合
                if (deleteNum > 0){
                    showToast("指定された条件に合う $deleteNum 件のデータを削除しました。")
                } else { // 条件に合うデータが0件の場合
                    showToast("指定された条件に合うデータが 0 件でした。条件を確認してください。")
                }
            }
        }
        return view
    }

    /*    DELETEを用いたSQL文を生成し、SQL文を実行する関数 */
    private fun deleteByquery(db: SQLiteDatabase, queryGameMap: MutableMap<String, String>, queryDateMap: MutableMap<String, String>): Int {
//        SQLのwhere句を格納するString
        var caramQuery = ""
//        SQLのプレースホルダーへバインドする値を格納するList
        val caramValueList = mutableListOf<String>()

//        勝敗、先行・後攻、自分のクラス、相手のクラスのSQL文のwhere句を作成
        for(queryGame in queryGameMap){
//            caramQueryにすでに値が格納されている場合、"and"を挿入
            if(!TextUtils.isEmpty(caramQuery)){
                caramQuery += " and "
            }

//            カラム名とプレースホルダーを挿入 ex) result = ?
            caramQuery += (queryGame.key + " = ?")
//            プレースホルダーにバインドする値を格納
            caramValueList.add(queryGame.value)
        }

/*        日時範囲のSQL分のwhere句を作成 */
        for(queryDate in queryDateMap){
//            caramQueryにすでに値が格納されている場合、"and"を挿入
            if(!TextUtils.isEmpty(caramQuery)){
                caramQuery += " and "
            }

            if(queryDate.key == "begin_date"){
                caramQuery += ("time_stamp >= ?")
            } else if(queryDate.key == "end_date"){
                caramQuery += ("time_stamp <= ?")
            }
            caramValueList.add(queryDate.value)
        }

//        プレースホルダーに格納する値はArrayなのでListからArrayに変換
        val caramValueArray: Array<String>?
        if(caramValueList.size == 0){ // sizeが0の場合はnullにする
            caramValueArray = null
        } else {
            caramValueArray = caramValueList.toTypedArray()
        }

//        DELETEを用いたSQLを実行し、削除した件数を返却
        return db.delete("default_table", caramQuery, caramValueArray)
    }

    // クラスのradiobuttonのIDからStringに変換する関数
    private fun conversionString(id: Int): String {
        when(id){
            R.id.elf_my_delete, R.id.elf_opponent_delete -> {
                return mContext.getString(R.string.elf)
            }
            R.id.royal_my_delete, R.id.royal_opponent_delete-> {
                return mContext.getString(R.string.royal)
            }
            R.id.witch_my_delete, R.id.witch_opponent_delete-> {
                return mContext.getString(R.string.witch)
            }
            R.id.dragon_my_delete, R.id.dragon_opponent_delete-> {
                return mContext.getString(R.string.dragon)
            }
            R.id.necromancer_my_delete, R.id.necromancer_opponent_delete-> {
                return mContext.getString(R.string.necromancer)
            }
            R.id.vampire_my_delete, R.id.vampire_opponent_delete-> {
                return mContext.getString(R.string.vampire)
            }
            R.id.bishop_my_delete, R.id.bishop_opponent_delete-> {
                return mContext.getString(R.string.bishop)
            }
            R.id.nemesis_my_delete, R.id.nemesis_opponent_delete-> {
                return mContext.getString(R.string.nemesis)
            }
        }

        return ""
    }

    //    Toast表示のための関数
    private fun showToast(s: String) {
        val msg = Toast.makeText(mContext, s, Toast.LENGTH_LONG)
        msg.show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
            mContext = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment DeleteDataFragment.
         */
        @JvmStatic
        fun newInstance() =
                DeleteDataFragment().apply {
                    arguments = Bundle()
                }
    }
}
