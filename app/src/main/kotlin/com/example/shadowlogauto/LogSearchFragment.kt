package com.example.shadowlogauto

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.example.shadowlogauto.database.DatabaseHelper
import com.example.shadowlogauto.model.CaramDb
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LogSearchFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LogSearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class LogSearchFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var mContext : Context

    private lateinit var mEditCardName1 : AutoCompleteTextView
    private lateinit var mEditCardName2 : AutoCompleteTextView
    private lateinit var mEditCardName3 : AutoCompleteTextView
    private lateinit var mKeepRadio1 : RadioButton
    private lateinit var mChangeRadio1 : RadioButton
    private lateinit var mUnspecifiedRadio1 : RadioButton
    private lateinit var mKeepRadio2 : RadioButton
    private lateinit var mChangeRadio2 : RadioButton
    private lateinit var mUnspecifiedRadio2 : RadioButton
    private lateinit var mKeepRadio3 : RadioButton
    private lateinit var mChangeRadio3 : RadioButton
    private lateinit var mUnspecifiedRadio3 : RadioButton
    private lateinit var mMyClassRadioGroup : RadioGroup
    private lateinit var mMyClassRadioButton : RadioButton
    private lateinit var mOpponentClassRadioGroup : RadioGroup
    private lateinit var mOpponentClassRadioButton : RadioButton
    private lateinit var mBeginDate : Button
    private lateinit var mEndDate : Button
    
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
        val view = inflater.inflate(R.layout.fragment_search, container, false)

/*        LogSearchFragment内の各Viewを取得 */
        mEditCardName1 = view.findViewById(R.id.edit_card_name_search1) as AutoCompleteTextView
        mEditCardName2 = view.findViewById(R.id.edit_card_name_search2) as AutoCompleteTextView
        mEditCardName3 = view.findViewById(R.id.edit_card_name_search3) as AutoCompleteTextView

        mMyClassRadioGroup = view.findViewById(R.id.my_class_search_radio_group) as RadioGroup
        mOpponentClassRadioGroup = view.findViewById(R.id.opponent_class_search_radio_group) as RadioGroup

        mKeepRadio1 = view.findViewById(R.id.keep_search1) as RadioButton
        mChangeRadio1 = view.findViewById(R.id.change_search1) as RadioButton
        mUnspecifiedRadio1 = view.findViewById(R.id.mulligan_unspecified_search1) as RadioButton
        mKeepRadio2 = view.findViewById(R.id.keep_search2) as RadioButton
        mChangeRadio2 = view.findViewById(R.id.change_search2) as RadioButton
        mUnspecifiedRadio2 = view.findViewById(R.id.mulligan_unspecified_search2) as RadioButton
        mKeepRadio3 = view.findViewById(R.id.keep_search3) as RadioButton
        mChangeRadio3 = view.findViewById(R.id.change_search3) as RadioButton
        mUnspecifiedRadio3 = view.findViewById(R.id.mulligan_unspecified_search3) as RadioButton

        mBeginDate = view.findViewById(R.id.button_begin_date_search) as Button
        mEndDate = view.findViewById(R.id.button_end_date_search) as Button

//        EditTextViewで入力候補を表示するために、テキストファイルを読み込む際に使用するStream、BufferdReader
        var labelInputStream : InputStream? = null
        var labelBufferedReader : BufferedReader? = null

        /*        日付を入力する時に使用するinnerクラス
                  DialogFragmentで日付を入力できるカレンダーを表示する  */
        class DateDialogFragment(val button: Button) : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                val calendar = Calendar.getInstance()
                return DatePickerDialog(
                        mContext,
//                        Lollipop の場合は Holo テーマのダイアログにする
//                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) AlertDialog.THEME_HOLO_LIGHT else 
                        theme,
/*                        DialogFragmentで日付を選択し、OKを押したときの処理 */
                        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                            val inputDate = Calendar.getInstance()
                            inputDate.set(year, month, dayOfMonth)
                            val dfInputeDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val strInputDate = dfInputeDate.format(inputDate.time)
//                            DialogFragmentを呼び出したボタンのテキストに日付をセット
                            button.text = strInputDate
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH))
                        .also {
                            /* 選択可能な年月日の上限を指定
                               終了日付が指定されている場合、開始日付は終了日付を上限にする */
                            if (button === mBeginDate && mEndDate.text != "指定なし"){
                                val maxDate = Calendar.getInstance()
                                val dfMaxDate = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
                                val endInputDate = dfMaxDate.parse(mEndDate.text.toString())
                                maxDate.time = endInputDate
                                it.datePicker.maxDate = maxDate.timeInMillis
                            } else {
//                                終了日付が指定されていない場合、現在日付を上限にする
                                it.datePicker.maxDate = calendar.timeInMillis
                            }

                            /*  選択可能な年月日の下限を指定 */
                            val minDate = Calendar.getInstance()
                            if (button === mEndDate && mBeginDate.text != "指定なし"){
                            /* 開始日付が指定されている場合、終了日付は開始日付を下限にする */
                                val dfMinDate = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
                                val beginInputDate = dfMinDate.parse(mBeginDate.text.toString())
                                minDate.time = beginInputDate
                                it.datePicker.minDate = minDate.timeInMillis
                            } else {
//                                開始日付が指定されていない場合、終了日付は2019/1/1を下限にする
                                minDate.set(2019, 0, 1)
                                it.datePicker.minDate = minDate.timeInMillis
                            }
//                            タイトルが勝手に表示されるのを防ぐために空文字をセット
                            it.setTitle("")
                        }
            }

            /* DialogFragmentでcancelを押した際の処理 */
            override fun onCancel(dialog: DialogInterface?) {
                super.onCancel(dialog)
                button.text = "指定なし"
            }
        }


        /*    EditTextの入力を監視するためのinnerクラス
              searchFragmentに複数のEditTextがあるため、Viewをコールバックに渡し、Viewを判別 */
        class GenericTextWatcher(val editTextView : View) : TextWatcher {
            //        文字列を入力した後に呼ばれる関数(英字入力だと1文字入力しただけで呼ばれる)
            override fun afterTextChanged(s: Editable?) {
//            入力された文字列を取得
                val inputStr = s.toString()
//            文字列が1文字以上入力されているかどうかで入力制御するため、真偽値を取得
                val boolActive = !TextUtils.isEmpty(inputStr)

                when(editTextView.id){
//                カード名1のEditTextに変化があったとき
                    R.id.edit_card_name_search1 -> {
//                    カード名1のマリガンを入力するラジオボタンを活性化/非活性化
                        mKeepRadio1.isClickable = boolActive
                        mChangeRadio1.isClickable = boolActive
                        mUnspecifiedRadio1.isClickable = boolActive
//                    カード名2を入力するEditTextを活性化/非活性化
                        mEditCardName2.isFocusable = boolActive
                        mEditCardName2.isFocusableInTouchMode = boolActive

//                    カード名2,3のプレースホルダーの文字列を変更
                        if(boolActive){
                            mEditCardName2.hint = ""
                            mEditCardName3.hint = "カード名2から入力してください"
                        } else {
                            mEditCardName2.hint = "カード名1から入力してください"
                            mEditCardName3.hint = "カード名1から入力してください"
                            mEditCardName2.setText("")
//                        カード名1のマリガンのラジオボタンのチェックを外す
                            mUnspecifiedRadio1.isChecked = true
                        }
                    }

                    R.id.edit_card_name_search2 -> {
                        mKeepRadio2.isClickable = boolActive
                        mChangeRadio2.isClickable = boolActive
                        mUnspecifiedRadio2.isClickable = boolActive
                        mEditCardName3.isFocusable = boolActive
                        mEditCardName3.isFocusableInTouchMode = boolActive

                        if(boolActive){
                            mEditCardName3.hint = ""
                        } else {
                            if(!TextUtils.isEmpty(mEditCardName1.text)){
                                mEditCardName3.hint = "カード名2から入力してください"
                            }
                            mEditCardName3.setText("")
                            mUnspecifiedRadio2.isChecked = true
                        }
                    }

                    R.id.edit_card_name_search3 -> {
                        mKeepRadio3.isClickable = boolActive
                        mChangeRadio3.isClickable = boolActive
                        mUnspecifiedRadio3.isClickable = boolActive

                        if (boolActive){
                        } else {
                            mUnspecifiedRadio3.isChecked = true
                        }
                    }
                }
            }

            //        文字を入力する前に呼ばれる関数
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            //        1文字を入力した後に呼ばれる関数
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        }

//        EditTextView以外をタップしたときにソフトキーボードを閉じる
        val touchLinearLayout = view.findViewById(R.id.touch_search) as LinearLayout
        touchLinearLayout.setOnClickListener {
            val tmpView = activity?.currentFocus
            if (tmpView != null) {
                val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }


/*        EditTextの入力を監視し、入力を制御
          引数をGenericTextWatcherとすることで、EditTextのViewを渡す */
        mEditCardName1.addTextChangedListener(GenericTextWatcher(mEditCardName1))
        mEditCardName2.addTextChangedListener(GenericTextWatcher(mEditCardName2))
        mEditCardName3.addTextChangedListener(GenericTextWatcher(mEditCardName3))

/*        日付を指定するボタンを押したとき */
        mBeginDate.setOnClickListener {
//            DialogFragmentを生成し、表示
//            引数にButtonを渡し、そのButton.textに日付をセット
            DateDialogFragment(mBeginDate).show(activity?.supportFragmentManager, mBeginDate::class.java.simpleName)
        }
        mEndDate.setOnClickListener {
            DateDialogFragment(mEndDate).show(activity?.supportFragmentManager, mEndDate::class.java.simpleName)
        }

//        検索ボタンを押したとき
        val mSearch = view.findViewById(R.id.search_button) as Button
        mSearch.setOnClickListener{

            //            勝敗、先行・後攻、自分のクラス・デッキ、相手のクラス・デッキの検索条件を格納するMap
            val queryGameMap = mutableMapOf<String, String>()

//            勝敗をqueryGameMapに格納するために、選択されたラジオボタンのIDを取得
            val resultRadioId = (view.findViewById(R.id.result_radio_search) as RadioGroup).checkedRadioButtonId
//            勝敗の検索条件が指定されている場合、queryGameMapに追加
            if (resultRadioId != R.id.result_unspecified_search) {
                val result = (view.findViewById(resultRadioId) as RadioButton).text.toString()
                queryGameMap["result"] = result
            }

//            自分のクラスをqueryGameMapに格納
            mMyClassRadioButton = view.findViewById(mMyClassRadioGroup.checkedRadioButtonId) as RadioButton
            val myClassStr = conversionString(mMyClassRadioButton.id)
//            自分のクラスの検索条件が指定されている場合、queryGameMapに追加
            if(!TextUtils.isEmpty(myClassStr)){
                queryGameMap["my_class"] = myClassStr
            }

//            自分のデッキをqueryGameMapに格納
            val myDeck = (view.findViewById(R.id.edit_my_deck_search) as TextView).text.toString()
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
            val oppoonentDeck = (view.findViewById(R.id.edit_opponent_deck_search) as TextView).text.toString()
            if(!TextUtils.isEmpty(oppoonentDeck)){
                queryGameMap["opponent_deck"] = oppoonentDeck
            }

//            先行/後攻をqueryGameMapに追加
            val turnRadioId = (view.findViewById(R.id.turn_radio_search) as RadioGroup).checkedRadioButtonId
            if (turnRadioId != R.id.turn_unspecified_search) {
                val turn = (view.findViewById(turnRadioId) as RadioButton).text.toString()
                if (!TextUtils.isEmpty(turn)) {
                    queryGameMap["turn"] = turn
                }
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

//            カードの名前、マリガンを格納するList
            val queryCardList = mutableListOf<Pair<String, String>>()

//            カードの名前、マリガンを格納する際に、viewから値を取得するために利用するArray
            val editCardRArray = arrayOf(Triple(R.id.edit_card_name_search1, R.id.card_mulligan_search1, R.id.mulligan_unspecified_search1),
                    Triple(R.id.edit_card_name_search2, R.id.card_mulligan_search2, R.id.mulligan_unspecified_search2),
                    Triple(R.id.edit_card_name_search3, R.id.card_mulligan_search3, R.id.mulligan_unspecified_search3))

//            カードの名前、マリガンをqueryCardListに格納
            for(editCard in editCardRArray){
//                カード名の検索条件が指定されている場合、queryCardMapに追加
                val cardName = (view.findViewById(editCard.first) as TextView).text.toString()
                if(!TextUtils.isEmpty(cardName)){
                    val cardMulliganRadioId = (view.findViewById(editCard.second) as RadioGroup).checkedRadioButtonId
                    if(cardMulliganRadioId != editCard.third) {
                        val cardMulligan = (view.findViewById(cardMulliganRadioId) as RadioButton).text.toString()
                        queryCardList.add(Pair(cardName, cardMulligan))
                    } else {
//                        検索条件がカード名のみで、マリガンが指定されていない場合はPair.secondに空文字を格納
                        queryCardList.add(Pair(cardName, ""))
                    }
                } else {
                    break
                }
            }

//            検索条件(queryMap,cardMap)を用いて、searchByqueryを実行し、検索結果を取得
            val searchResultList = searchByquery(db, queryGameMap, queryDateMap, queryCardList)

//            検索結果が1件以上の場合
            if (searchResultList.size > 0){
//                fragmentに遷移するためにfragmentManagerを取得
                val fragmentManager = activity?.supportFragmentManager

//                LogListFragmentにデータを渡すためのBundleを生成
                val bundle = Bundle()

/*                検索結果を遷移先のFragmentであるLogListFragmentに渡す */
//                intentにputExtraする際にListを扱えないため、Arrayを渡す
                bundle.putParcelableArray("SEARCH_RESULT", searchResultList.toTypedArray())

//                RecyclerViewのlist表示の列数を指定
//                TODO 2以上を指定した際に自然に表示されるようにする
//                TODO SettingFragmentで指定する形がベスト
                bundle.putInt("column-count", 1)

                if(fragmentManager != null){
                    val fragmentTransaction = fragmentManager.beginTransaction()

//                    BackStackに積む(遷移先のFragmentで戻るボタンを押すと、遷移元のFragmentに戻るようにする)
                    fragmentTransaction.addToBackStack(null)

//                    遷移先のFragmentにLogListFragmentを指定
                    val fragmentLogList = LogListFragment.newInstance()

//                    fragmentに検索結果を格納したbundleをセット
                    fragmentLogList.arguments = bundle

//                    LogListFragmentに遷移
                    fragmentTransaction.replace(R.id.container, fragmentLogList).commit()
                }
            } else { // 検索結果が0件の場合
                showToast("検索結果が0件でした。検索条件を確認してください。")
            }
        }

        try {
            labelInputStream = activity!!.assets.open("input.txt")
            labelBufferedReader = BufferedReader(InputStreamReader(labelInputStream))
            val cardNameAutoCompleteList = mutableListOf<String>()
            var strTmp = labelBufferedReader.readLine()

//            入力候補用のテキストファイルを1行ずつ読み込み、Listに格納
            do {
                cardNameAutoCompleteList.add(strTmp)
                strTmp = labelBufferedReader.readLine()
            } while(strTmp != null)

//            EditTextViewに入力候補を表示するAdapterをセット
            val arrayAdapter = ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, cardNameAutoCompleteList)
            mEditCardName1.setAdapter(arrayAdapter)
            mEditCardName2.setAdapter(arrayAdapter)
            mEditCardName3.setAdapter(arrayAdapter)
        } finally {
            labelInputStream?.close()
            labelBufferedReader?.close()
        }

        return view
    }

//    SQL文を生成し、readCursor(SQL文を実行する)関数を呼ぶ関数
    private fun searchByquery(db: SQLiteDatabase, queryGameMap: MutableMap<String, String>, queryDateMap: MutableMap<String, String>, queryCardList: MutableList<Pair<String, String>>): MutableList<CaramDb> {
//        Cursorを確実にcloseするために、try{}～finally{}にする
        var cursor : Cursor? = null
        try {
//            SQLのwhere句を格納するString
            var caramQuery = ""
//            SQLのプレースホルダーへバインドする値を格納するList
            val caramValueList = mutableListOf<String>()

//            勝敗、先行・後攻、自分のクラス、相手のクラスのSQL文のwhere句を作成
            for(queryGame in queryGameMap){
//                caramQueryにすでに値が格納されている場合、"and"を挿入
                if(!TextUtils.isEmpty(caramQuery)){
                    caramQuery += " and "
                }

//                カラム名とプレースホルダーを挿入 ex) result = ?
                caramQuery += (queryGame.key + " = ?")
//                プレースホルダーにバインドする値を格納
                caramValueList.add(queryGame.value)
            }

//            日時範囲のSQL分のwhere句を作成
            for(queryDate in queryDateMap){
//                caramQueryにすでに値が格納されている場合、"and"を挿入
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

            if (queryCardList.size > 0){
  //            カード名、マリガン結果のwhere句を格納するString
                var cardQuery = ""

/*              勝敗、先行・後攻、自分のクラス、相手のクラスのSQL文のwhere句を作成
                検索条件に指定されたカードの枚数によって分岐 */
//              検索条件として必要なカラムのList
                var caramCardList : List<List<Pair<String, String>>> = listOf()
                when(queryCardList.size){
                    1 -> {
                        caramCardList = listOf(listOf("card_name1" to "card_mulligan1"),
                                listOf("card_name2" to "card_mulligan2"),
                                listOf("card_name3" to "card_mulligan3"))
                    }
                    2 -> {
                        caramCardList = listOf(listOf("card_name1" to "card_mulligan1", "card_name2" to "card_mulligan2"),
                                listOf("card_name2" to "card_mulligan2", "card_name1" to "card_mulligan1"),
                                listOf("card_name1" to "card_mulligan1", "card_name3" to "card_mulligan3"),
                                listOf("card_name3" to "card_mulligan3", "card_name1" to "card_mulligan1"),
                                listOf("card_name2" to "card_mulligan2", "card_name3" to "card_mulligan3"),
                                listOf("card_name3" to "card_mulligan3", "card_name2" to "card_mulligan2"))
                    }
                    3 -> {
                        caramCardList = listOf(listOf("card_name1" to "card_mulligan1", "card_name2" to "card_mulligan2", "card_name3" to "card_mulligan3"),
                                listOf("card_name3" to "card_mulligan3", "card_name1" to "card_mulligan1", "card_name2" to "card_mulligan2"),
                                listOf("card_name2" to "card_mulligan2", "card_name3" to "card_mulligan3", "card_name1" to "card_mulligan1"),
                                listOf("card_name2" to "card_mulligan2", "card_name1" to "card_mulligan1", "card_name3" to "card_mulligan3"),
                                listOf("card_name3" to "card_mulligan3", "card_name2" to "card_mulligan2", "card_name1" to "card_mulligan1"),
                                listOf("card_name1" to "card_mulligan1", "card_name3" to "card_mulligan3", "card_name2" to "card_mulligan2"))
                    }
                }

                if(!TextUtils.isEmpty(caramQuery)){
                    caramQuery += " and "
                }

                cardQuery += "("

//                検索条件として必要なカラムをループ
                for((index, caramCard) in caramCardList.withIndex()){
//                    cardQueryにすでに値が格納されている場合、"or"を挿入
//                    ex) (card_name1 = ? and card_mulligan1 = ?) or (card_name2 = ? and ....
                    if(index >= 1){
                        cardQuery += " or "
                    }

                    cardQuery += "("
//                    検索条件に指定されたカードの枚数分ループ
                    for((indexCardNum, queryCard) in queryCardList.withIndex()){
                        if(indexCardNum >= 1){
                            cardQuery += " and "
                        }
//                        カード名のカラム名とプレースホルダーを挿入
                        cardQuery += "(" + caramCard[indexCardNum].first +  " = ?"
//                        カード名のプレースホルダーにバインドする値を格納
                        caramValueList.add(queryCard.first)
//                        検索条件でカード名、マリガンの双方を指定している場合
                        if(!TextUtils.isEmpty(queryCard.second)) {
//                            マリガンのカラム名とプレースホルダーを挿入
                            cardQuery += " and " + caramCard[indexCardNum].second + " = ?"
//                            マリガンのプレースホルダーにバインドする値を格納
                            caramValueList.add(queryCard.second)
                        }
                        cardQuery += ")"
                    }

                    cardQuery += ")"
                }
                caramQuery += "$cardQuery)"
            }

//            プレースホルダーに格納する値はArrayでなければいけないのでListからArrayに変換
            val caramValueArray: Array<String>?
            if(caramValueList.size == 0){
                caramValueArray = null
            } else {
                caramValueArray = caramValueList.toTypedArray()
            }

//            SQLをを実行し、検索結果をcursorに格納
            cursor = db.query("battles_logs",
                    arrayOf("_id", "result", "my_class", "my_deck", "opponent_class", "opponent_deck", "turn", "card_name1", "card_name2", "card_name3",
                            "card_mulligan1", "card_mulligan2", "card_mulligan3", "time_stamp"),
                    caramQuery, caramValueArray, null, null, null)

//            検索結果をcursorから1レコードずつ読み込んで返す
            return readCursor(cursor!!)
        } finally {
            // Cursorを忘れずにcloseする
            cursor?.close()
        }
    }

    /*   検索結果が格納されたcursorから、1レコードずつ読み込み、検索結果のListを返す関数 */
    private fun readCursor(cursor: Cursor): MutableList<CaramDb> {
//        検索結果を格納するList
        val resultSearch = mutableListOf<CaramDb>()

//        カラムのindexを格納するList
        val indexList = mutableListOf<Int>()

//        SQLiteのDBカラムのArray、各カラムのindexを格納する際に使用
        val dbCaramArray = arrayOf("_id", "result", "my_class", "my_deck", "opponent_class", "opponent_deck", "turn", "card_name1", "card_name2", "card_name3", "card_mulligan1", "card_mulligan2", "card_mulligan3", "time_stamp")

//        各カラムのindexをindexListに格納
        for(dbCaram in dbCaramArray){
            indexList.add(cursor.getColumnIndex(dbCaram))
        }

//        検索結果をresultSearchに格納するループ、cursor.moveToNextとすると検索結果の個数分ループする
        while (cursor.moveToNext()) {
//            idを格納するInt
            var primaryKeyId = 0
//            勝敗、先行・後攻、自分のクラス、相手のクラスの検索結果を一時的に格納するList
            val gameResultList = arrayListOf<String>()
//            カード名3枚を格納するList
            val cardNameList = arrayListOf<String>()
//            3枚のマリガンを格納するList
            val cardMulliganList = arrayListOf<String>()
//            検索結果をcursorから取り出す
            for ((index, indexDb) in indexList.withIndex()){
                when(index){
//                    idを格納
                    0 -> {
                        primaryKeyId = cursor.getInt(indexDb)
                    }
//                    勝敗、先行・後攻、自分のクラス、相手のクラス、タイムスタンプを格納
                    1, 2, 3, 4, 5, 6, 13 -> {
                        gameResultList.add(cursor.getString(indexDb))
                    }
//                    カード名を格納
                    7, 8, 9 -> {
//                        カード名がnullである場合、cardNameListにaddしない
                        if (cursor.getString(indexDb) != null){
                            cardNameList.add(cursor.getString(indexDb))
                        }
                    }
//                    マリガンを格納
                    10, 11, 12 -> {
//                        マリガン結果がnullである場合、cardMulliganListにaddしない
                        if (cursor.getString(indexDb) != null) {
                            cardMulliganList.add(cursor.getString(indexDb))
                        }
                    }

                }
            }
//            検索結果をcaramDbオブジェクトに格納
            val caramDbResult = CaramDb(primaryKeyId, gameResultList[0], gameResultList[1], gameResultList[2], gameResultList[3], gameResultList[4], gameResultList[5], cardNameList, cardMulliganList, gameResultList[6])
//            caramDbオブジェクトをListに格納
            resultSearch.add(caramDbResult)
        }
//        検索結果のListを返却
        return resultSearch
    }

    //    クラスのradiobuttonのIDからStringに変換する関数
    private fun conversionString(id: Int): String {
        when(id){
            R.id.elf_my_search, R.id.elf_opponent_search -> {
                return mContext.getString(R.string.elf)
            }
            R.id.royal_my_search, R.id.royal_opponent_search-> {
                return mContext.getString(R.string.royal)
            }
            R.id.witch_my_search, R.id.witch_opponent_search-> {
                return mContext.getString(R.string.witch)
            }
            R.id.dragon_my_search, R.id.dragon_opponent_search-> {
                return mContext.getString(R.string.dragon)
            }
            R.id.necromancer_my_search, R.id.necromancer_opponent_search-> {
                return mContext.getString(R.string.necromancer)
            }
            R.id.vampire_my_search, R.id.vampire_opponent_search-> {
                return mContext.getString(R.string.vampire)
            }
            R.id.bishop_my_search, R.id.bishop_opponent_search-> {
                return mContext.getString(R.string.bishop)
            }
            R.id.nemesis_my_search, R.id.nemesis_opponent_search-> {
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

    override fun onResume() {
        super.onResume()
        val prefs = this.activity!!.getSharedPreferences("property", Context.MODE_PRIVATE)
        val wifeBirth = prefs.getInt("birthday", 0)
//        val edWifeBirth = view!!.findViewById(R.id.edit_birthday) as EditText
        if(wifeBirth != 0){
//        editMyClassLog.setText(Integer.toString(wifeBirth))
        }
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
         * @return A new instance of fragment PropertyFragment.
         */
        @JvmStatic
        fun newInstance() =
                LogSearchFragment().apply {
                    arguments = Bundle()
                }
    }
}
