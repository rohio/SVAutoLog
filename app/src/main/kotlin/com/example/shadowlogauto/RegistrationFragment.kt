package com.example.shadowlogauto

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.example.shadowlogauto.database.DatabaseHelper
import com.example.shadowlogauto.model.RegistAsyncTask
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

// 撮影する画像ファイルのパス
private var mCameraFileName: String = ""
// ログ出力のタグ
private const val TAG = "ShadowLogRegist"
// startActivityForResultのリクエストコード
private const val TAKE_PICTURE = 1
private const val CHOICE_PICTURE = 2
// onRequestPermissionsResultのリクエストコード
private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STROGE = 1

// SQLiteのDBカラムのArray
private val dbCaramArray = arrayOf("result", "my_class", "my_deck",
        "opponent_class", "opponent_deck", "turn",
        "card_name1", "card_name2", "card_name3",
        "card_mulligan1", "card_mulligan2", "card_mulligan3",
        "time_stamp")

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [RegistrationFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [RegistrationFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class RegistrationFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var mContext: Context

    private lateinit var mDbHelper: DatabaseHelper
    private lateinit var db : SQLiteDatabase

    private val cardNameCompletionList = mutableListOf<String>()

//    アクティビティ破棄時にbitmapを保存するためのプロパティ
    private var mInputBitmapName = ""
    private var mRequestCode = 0
    private lateinit var mUri : Uri

    private lateinit var mImage : ImageView

    private lateinit var mMyDeckText : TextView
    private lateinit var mOpponentDeckText : TextView

    private lateinit var mResultRadioGroup : RadioGroup
    private lateinit var mResultRadioButton : RadioButton
    private lateinit var mMyClassRadioGroup : RadioGroup
    private lateinit var mMyClassRadioButton : RadioButton
    private lateinit var mOpponentClassRadioGroup : RadioGroup
    private lateinit var mOpponentClassRadioButton : RadioButton
    private lateinit var mTurnRadioGroup : RadioGroup
    private lateinit var mTurnRadioButton : RadioButton

    private lateinit var mTakePicture : Button
    private lateinit var mChoicePicture : Button
    private lateinit var mSaveButton : Button

    private lateinit var mProgressBar : ProgressBar

    //    TODO DEBUG用 削除予定
    //    private var mCardImageList = mutableListOf<ImageView>()
    private var mCardNameList = mutableListOf<AutoCompleteTextView>()
    private var mCardMulliganList = mutableListOf<ToggleButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        ストレージにアクセスするパーミッション許可or非許可を選択するDialogの表示
        checkWriteExternalPermission()

//        SQLiteを使ってDB管理
        mDbHelper = DatabaseHelper(mContext)
        db = mDbHelper.writableDatabase

//        OpenCVライブラリのロード
        System.loadLibrary("opencv_java3")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

/*        RegistrationFragment内の各Viewを取得 */
        mTakePicture = view.findViewById(R.id.takePicture) as Button
        mChoicePicture = view.findViewById(R.id.choiceButton) as Button
        mSaveButton = view.findViewById(R.id.save_button) as Button

        mImage = view.findViewById(R.id.inputView) as ImageView
        mMyDeckText = view.findViewById(R.id.myDeckText) as TextView
        mOpponentDeckText = view.findViewById(R.id.opponentDeckText) as TextView

//        TODO DEBUG用 削除予定
//        カード画像をセットするViewをmCardImageListに格納
//        mCardImageList.add(view.findViewById(R.id.cardView1) as ImageView)
//        mCardImageList.add(view.findViewById(R.id.cardView2) as ImageView)
//        mCardImageList.add(view.findViewById(R.id.cardView3) as ImageView)

        mCardNameList.add(view.findViewById(R.id.card_name_regist1) as AutoCompleteTextView)
        mCardNameList.add(view.findViewById(R.id.card_name_regist2) as AutoCompleteTextView)
        mCardNameList.add(view.findViewById(R.id.card_name_regist3) as AutoCompleteTextView)

        mCardMulliganList.add(view.findViewById(R.id.card_mulligan_toggle1) as ToggleButton)
        mCardMulliganList.add(view.findViewById(R.id.card_mulligan_toggle2) as ToggleButton)
        mCardMulliganList.add(view.findViewById(R.id.card_mulligan_toggle3) as ToggleButton)

/*        radioGroupのオブジェクトを取得し、ラジオIDを元に選択されたラジオボタンを取得 */
        mResultRadioGroup = view.findViewById(R.id.resultButton) as RadioGroup
        mMyClassRadioGroup = view.findViewById(R.id.my_class_regist_radio_group) as RadioGroup
        mOpponentClassRadioGroup = view.findViewById(R.id.opponent_class_regist_radio_group) as RadioGroup
        mTurnRadioGroup = view.findViewById(R.id.turn_regist_radio_group) as RadioGroup

        mProgressBar = view.findViewById(R.id.progress_bar) as ProgressBar

//        Activity,Fragmentを破棄する際にsavedInstanceStateに値を保存している場合、値を読み込む
        if (savedInstanceState != null){
//            入力画像をセット
            mRequestCode = savedInstanceState.getInt("request_code")

//            入力画像をinputBitmapに格納
            var inputBitmap : Bitmap? = null
            if(mRequestCode == TAKE_PICTURE){ // カメラアプリで画像を撮影した場合
//                TODO 他にnullable回避策はある？
                mCameraFileName = savedInstanceState.getString("input_bitmap_name")!!
                inputBitmap = BitmapFactory.decodeFile(mCameraFileName)
            } else if(mRequestCode == CHOICE_PICTURE) { /* 画像を選択した場合 */
//                TODO 他にnullable回避策はある？
                mUri = savedInstanceState.getParcelable("uri")!!
                val pfDescriptor: ParcelFileDescriptor? = mContext.contentResolver?.openFileDescriptor(mUri, "r")
                if (pfDescriptor != null) {
                    val fileDescriptor = pfDescriptor.fileDescriptor
                    inputBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                    pfDescriptor.close()
                }
            }
            mImage.setImageBitmap(inputBitmap)

/*            認識結果を表示するViewの表示状態を取得し、trueの場合visibleをセット */
            if (savedInstanceState.getBoolean("visibility")){
                for (mCardName in mCardNameList){
                    mCardName.visibility = View.VISIBLE
                }
                for (mCardMulligan in mCardMulliganList){
                    mCardMulligan.visibility = View.VISIBLE
                }
                mTurnRadioGroup.visibility = View.VISIBLE
                mOpponentClassRadioGroup.visibility = View.VISIBLE
                mResultRadioGroup.visibility = View.VISIBLE
                mOpponentDeckText.visibility = View.VISIBLE
                mSaveButton.visibility = View.VISIBLE
            }
        }

//        EditTextViewで入力候補を表示するために、テキストファイルを読み込む際に使用するStream、BufferdReader
        var labelInputSrream : InputStream? = null
        var labelBufferdReader : BufferedReader? = null

/*        EditTextView以外をタップしたときにソフトキーボードを閉じる */
        val touchRegistration = view.findViewById(R.id.touch_registration) as LinearLayout
        touchRegistration.setOnClickListener {
            val tmpView = activity?.currentFocus
            if (tmpView != null) {
//                    ソフトキーボードを閉じる
                val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }

/*        写真を撮るボタンを押した場合 */
        mTakePicture.setOnClickListener{
            //            入力した自分のクラスのTensorflowのモデルファイル、ラベルを使うためにmMyClassRadioButtonを取得
            mMyClassRadioButton = view.findViewById(mMyClassRadioGroup.checkedRadioButtonId) as RadioButton

/*           カメラアプリ(写真を撮るアプリ)へ遷移するIntentを実行 */
            val intent = Intent()
            intent.action = MediaStore.ACTION_IMAGE_CAPTURE
            val date = Date()
            val df = SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.US)

            val newPicFile = df.format(date) + ".jpg"
            val outPath = File(Environment.getExternalStorageDirectory(), newPicFile).path
            val outFile = File(outPath)

            mCameraFileName = outFile.toString()

            val outuri = FileProvider.getUriForFile(mContext, mContext.applicationContext.packageName + ".provider", outFile)
            Log.i(TAG, "uri: $outuri")

            intent.putExtra(MediaStore.EXTRA_OUTPUT, outuri)
            Log.i(TAG, "Taking New Picture: $mCameraFileName")
            try { // カメラを使用し、画像を取得できた場合、startActivityForResult関数を実行し、onActivityResultを呼ぶ
                startActivityForResult(intent, TAKE_PICTURE)
            } catch (e : ActivityNotFoundException){ // カメラアプリが無い場合
                showToast("カメラアプリが見つかりませんでした。")
            }
        }

/*        写真を選ぶボタンを押した場合 */
        mChoicePicture.setOnClickListener{
//            入力した自分のクラスのTensorflowのモデルファイル、ラベルを使うためにmMyClassRadioButtonを取得
            mMyClassRadioButton = view.findViewById(mMyClassRadioGroup.checkedRadioButtonId) as RadioButton

/*           写真を選択するアプリケーションへ遷移するIntentを実行 */
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"

            Log.i(TAG, "Importing New Picture")
            try { // 画像を選択するアプリを使用し、画像を取得できた場合、startActivityForResult関数を実行し、onActivityResultを呼ぶ
                startActivityForResult(intent, CHOICE_PICTURE)
            } catch (e : ActivityNotFoundException){ // 画像を選択するアプリが無い場合
                showToast("画像を選択するアプリが見つかりませんでした。")
            }
        }

/*        保存するボタンを押した場合 */
        val mSaveData = view.findViewById(R.id.save_button) as Button
        mSaveData.setOnClickListener{
//            勝敗、自分・相手のクラス、先攻/後攻のラジオボタンのチェックを取得
            mMyClassRadioButton = view.findViewById(mMyClassRadioGroup.checkedRadioButtonId) as RadioButton
            mOpponentClassRadioButton = view.findViewById(mOpponentClassRadioGroup.checkedRadioButtonId) as RadioButton
            mTurnRadioButton = view.findViewById(mTurnRadioGroup.checkedRadioButtonId) as RadioButton
            mResultRadioButton = view.findViewById(mResultRadioGroup.checkedRadioButtonId) as RadioButton

//            SQLiteのDBに登録する処理を行う、DBへの登録が成功したかどうかをBooleanで取得
            val saveSuccess = doAddEntry(db)

//            DBに登録できた場合
            if (saveSuccess){
//                DEBUG用 削除予定
                searchByCount(db)

/*                Viewに入力された値を削除 */
                mImage.setImageDrawable(null)
                mTurnRadioButton = view.findViewById(mTurnRadioGroup.checkedRadioButtonId) as RadioButton
                for(mCardName in mCardNameList){
                    mCardName.setText("", TextView.BufferType.NORMAL)
                }
                for(mCardMulligan in mCardMulliganList){
                    mCardMulligan.isChecked = false
                }

//                認識結果を表示する各Viewを非表示
                viewVisibilityChange(View.GONE)

                showToast("保存しました。")
            }
        }

/*        EditTextViewのカード名の候補を表示する処理 */
        try {
            labelInputSrream = activity!!.assets.open("input.txt")
            labelBufferdReader = BufferedReader(InputStreamReader(labelInputSrream))
            var strTmp = labelBufferdReader.readLine()

/*            入力候補用のテキストファイルを1行ずつ読み込み、Listに格納 */
            do {
                cardNameCompletionList.add(strTmp)
                strTmp = labelBufferdReader.readLine()
            } while(strTmp != null)

            for(mCardName in mCardNameList){
//                EditTextViewに入力候補を表示するAdapterをセット
                val arrayAdapter = ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, cardNameCompletionList)
                mCardName.setAdapter(arrayAdapter)
            }
        } finally {
//            InputStream,BufferdReaderをclose
            labelInputSrream?.close()
            labelBufferdReader?.close()
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        requestCodeによって処理分岐
        mRequestCode = requestCode
        when(requestCode) {
            TAKE_PICTURE, CHOICE_PICTURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    try {
//                        入力画像をinputBitmapに格納
                        var inputBitmap : Bitmap? = null
                        if(requestCode == TAKE_PICTURE){ // カメラアプリで画像を撮影した場合
                            inputBitmap = BitmapFactory.decodeFile(mCameraFileName)
                            //    アクティビティ破棄時にbitmapを保存するためにmCameraFileNameを格納
                            mInputBitmapName = mCameraFileName
                        } else if(requestCode == CHOICE_PICTURE && data != null) { /* 画像を選択した場合 */
                            // アクティビティ破棄時にbitmapを保存するためにuriを格納
                            // Required: Uri, Found: Uri?を回避するために、nullチェック後に!!演算子を使用
                            if (data.data != null){
                                mUri = data.data!!
                            }
                            val pfDescriptor: ParcelFileDescriptor? = mContext.contentResolver?.openFileDescriptor(mUri, "r")
                            if (pfDescriptor != null) {
                                val fileDescriptor = pfDescriptor.fileDescriptor
                                inputBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                                pfDescriptor.close()
                            }
                        }

//                        先攻/後攻の画像を格納するList
                        val resourceTurnBitmapList = mutableListOf<Bitmap>()
//                        クラスのアイコンを格納するList
                        val resourceClassBitmapList = mutableListOf<Bitmap>()

/*                        drawableから画像をBitmap型で読み込み、Listに格納 */
                        val resourceTurnIdArray = arrayOf(R.drawable.first, R.drawable.second)
                        val resourceClassIdArray = arrayOf(R.drawable.elf, R.drawable.royal, R.drawable.witch, R.drawable.dragon,
                                R.drawable.necromancer, R.drawable.vampire, R.drawable.bishop, R.drawable.nemesis)
                        for (resourceTurnId in resourceTurnIdArray){
                            resourceTurnBitmapList.add(BitmapFactory.decodeResource(activity!!.resources, resourceTurnId))
                        }
                        for (resourceClassId in resourceClassIdArray){
                            resourceClassBitmapList.add(BitmapFactory.decodeResource(activity!!.resources, resourceClassId))
                        }

//                        自分のクラスをStringで取得
                        val myClassStr = conversionString(mMyClassRadioButton.id)

/*                        画像認識処理に時間を要するため、非同期処理をAsyncTaskで実行 */
//                        registAsyncTaskコンストラクタでAsyncTaskを実装したインスタンスを作成
                        val registAsyncTask = RegistAsyncTask(inputBitmap!!, resourceTurnBitmapList, resourceClassBitmapList, myClassStr, activity!!.assets)
//                        AsyncTaskでメインUIのViewを変更する際にListenerが必要なため、Listenerをセット
                        registAsyncTask.setListener(createListener())
//                        AsyncTaskによる非同期処理を実行
                        registAsyncTask.execute(0)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /*    AsyncTaskで非同期処理を実現するために、Listenerを作成する関数 */
    private fun createListener(): RegistAsyncTask.Listener {
//        object式で、onPreSuccess(),onSuccess()メソッドをoverrideしたRegistAsyncTask.Listenerオブジェクトをreturn
        return object : RegistAsyncTask.Listener {
            /*            非同期処理を実行する前に実行される関数 */
            override fun onPreSuccess() {
//                プログレスバーを表示
                mProgressBar.visibility = android.widget.ProgressBar.VISIBLE

//                入力画像を表示するImageViewにセットしている画像を削除
                mImage.setImageDrawable(null)

//                相手のデッキのViewにセットされているtextを削除
                mOpponentDeckText.text = ""
                for (mCardName in mCardNameList){
                    mCardName.setText("")
                }

//                カード名のViewにセットされているエラー表示を削除
                for (mCardName in mCardNameList){
                    mCardName.error = null
                }

//                画像認識結果を表示する各Viewを非表示
                viewVisibilityChange(View.GONE)

/*                写真を取るボタン、写真を選ぶボタンを非活性化し、ボタンの色を灰色にする */
                mTakePicture.isEnabled = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mTakePicture.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.cool_gray))
                }
                mChoicePicture.isEnabled = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mChoicePicture.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.cool_gray))
                }
            }

            /*            非同期処理が完了した後に実行される関数 */
            override fun onSuccess(bitmap: Bitmap, firstTurn: Boolean, opponentClassChildId: Int, cardNameList: MutableList<String>, cardMulliganList: MutableList<Boolean>) {
//                選択された画像を表示
                mImage.setImageBitmap(bitmap)

/*                ターン(先攻、後攻)を判断し、ラジオボタンにチェック */
                if(firstTurn){
                    (mTurnRadioGroup.getChildAt(0) as RadioButton).isChecked = true
                } else {
                    (mTurnRadioGroup.getChildAt(1) as RadioButton).isChecked = true
                }

//                相手のクラスのラジオボタンをチェック
                (mOpponentClassRadioGroup.getChildAt(opponentClassChildId) as RadioButton).isChecked = true

/*                カード名をセット */
                for ((index, cardNameCompletion) in cardNameList.withIndex()){
//                    TODO DEBUG用削除予定
//                    mCardImageList[index].setImageBitmap(card)
                    mCardNameList[index].setText(cardNameCompletion)
                    Log.i(TAG, cardNameCompletion)
                }

//                カードのマリガン結果をセット
                for ((index, cardMulligan) in cardMulliganList.withIndex()) {
                    mCardMulliganList[index].isChecked = cardMulligan
                }

//                プログレスバーを非表示
                mProgressBar.visibility = android.widget.ProgressBar.GONE

//                認識結果を表示する各Viewを表示
                viewVisibilityChange(View.VISIBLE)

/*                写真を取るボタン、写真を選ぶボタンを活性化し、ボタンの色を明るくする */
                mTakePicture.isEnabled = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mTakePicture.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.chocolate))
                }
                mChoicePicture.isEnabled = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mChoicePicture.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.chocolate))
                }
            }
        }
    }

    /*    認識結果を格納するViewの表示を変更する関数 */
    private fun viewVisibilityChange(visibility: Int) {
        for (mCardName in mCardNameList){
            mCardName.visibility = visibility
        }
        for (mCardMulligan in mCardMulliganList){
            mCardMulligan.visibility = visibility
        }
        mTurnRadioGroup.visibility = visibility
        mOpponentClassRadioGroup.visibility = visibility
        mResultRadioGroup.visibility = visibility
        mOpponentDeckText.visibility = visibility
        mSaveButton.visibility = visibility
    }

    /*    入力情報を元にSQLiteのDBにデータを追加する関数 */
    private fun doAddEntry(db : SQLiteDatabase): Boolean {
/*        存在しないカード名が指定されているか確認し、存在しないカード名が指定されている場合、EditTextにエラー表示をする */
//        カード名が存在するものであるかを格納するList
        val cardCorrectCheckList = mutableListOf<Boolean>()

/*        入力されたカード名と存在するカード名を比較し、合致すればtrue
          または、カード名が入力されていない場合、true */
        for ((index, mCardName) in mCardNameList.withIndex()){
//            入力されたカード名を取得
            val mCardNameText = mCardName.text.toString()

//            入力されたカード名が空の場合、trueとし、continue
            cardCorrectCheckList.add(TextUtils.isEmpty(mCardNameText))
            if (TextUtils.isEmpty(mCardNameText)) { continue }

/*             入力されたカード名と存在するカード名を比較し、合致すればtrueとする */
            for (cardNameCompletion in cardNameCompletionList){
                if (mCardName.text.toString() == cardNameCompletion){
                    cardCorrectCheckList[index] = true
//                    入力されたカード名と存在するカード名が合致した場合、break
                    break
                }
            }
        }

/*        入力されたカード名に誤りがない場合、DBに入力データを格納 */
        if (cardCorrectCheckList[0] && cardCorrectCheckList[1] && cardCorrectCheckList[2]){
//            挿入するデータをContentValuesに格納
            val value = ContentValues()

/*            カラム名とフィールド値を格納 */
            value.put("result", mResultRadioButton.text.toString())
            value.put("my_deck", mMyDeckText.text.toString())
            value.put("opponent_deck" , mOpponentDeckText.text.toString())

/*            カード名、マリガン結果を格納
              カード名の入力がない場合は、カード名、マリガン結果ともにDBに格納しない */
//            カード名とマリガン結果をPairでListに格納
            val inputCardList = mutableListOf<Pair<String, String>>()
            for ((index, mCardName) in mCardNameList.withIndex()){
//                カード名の入力がない場合は、カード名、マリガン結果共にListに格納しない
                if (!TextUtils.isEmpty(mCardName.text.toString())){
                    inputCardList.add(Pair(mCardName.text.toString(), mCardMulliganList[index].text.toString()))
                }
            }
            val caramCardNameArray = arrayOf("card_name1" to "card_mulligan1",
                    "card_name2" to "card_mulligan2", "card_name3" to "card_mulligan3")
            for ((index, inputCard) in inputCardList.withIndex()){
                value.put(caramCardNameArray[index].first, inputCard.first)
                value.put(caramCardNameArray[index].second, inputCard.second)
            }

/*            ラジオボタンのIDから文字列に変換し、カラム名とフィールド値を格納 */
            value.put("turn" , conversionString(mTurnRadioButton.id))
            value.put("my_class", conversionString(mMyClassRadioButton.id))
            value.put("opponent_class" , conversionString(mOpponentClassRadioButton.id))

//            現在日時を格納
            value.put("time_stamp" , DateFormat.format("yyyy-MM-dd kk:mm", Date()).toString())

//            default_tableテーブルに1件追加、返り値は追加した行の値
//            DBへ登録できなかった場合、返り値が -1 となる
            val insertRow = db.insert("battles_logs", null, value)

            return if(insertRow.toInt() != -1){
    //                DBに登録できた場合、trueを返却
                true
            } else {
                showToast("何らかのエラーで保存できませんでした。")
    //                DBに登録できなかった場合、falseを返却
                false
            }
        } else {  // 入力されたカード名3枚中1枚以上で存在しないカード名が指定されている場合
            for ((index, cardCorrectCheck) in cardCorrectCheckList.withIndex()){
                if (!cardCorrectCheck){
//                    存在しないカード名であることを、EditTextViewにerrorで表示
                    mCardNameList[index].error = mCardNameList[index].text.toString() + "は、存在しないカード名です."
                }
            }
//            DBに登録できなかった場合、falseを返却
            return false
        }
    }

    /*    クラスのradiobuttonのIDからString, 先攻/後攻のradiobuttonのIDからStringに変換する関数 */
    private fun conversionString(id: Int): String {
        when(id){
            R.id.elf_my_regist, R.id.elf_opponent_regist -> {
                return mContext.getString(R.string.elf)
            }
            R.id.royal_my_regist, R.id.royal_opponent_regist-> {
                return mContext.getString(R.string.royal)
            }
            R.id.witch_my_regist, R.id.witch_opponent_regist-> {
                return mContext.getString(R.string.witch)
            }
            R.id.dragon_my_regist, R.id.dragon_opponent_regist-> {
                return mContext.getString(R.string.dragon)
            }
            R.id.necromancer_my_regist, R.id.necromancer_opponent_regist-> {
                return mContext.getString(R.string.necromancer)
            }
            R.id.vampire_my_regist, R.id.vampire_opponent_regist-> {
                return mContext.getString(R.string.vampire)
            }
            R.id.bishop_my_regist, R.id.bishop_opponent_regist-> {
                return mContext.getString(R.string.bishop)
            }
            R.id.nemesis_my_regist, R.id.nemesis_opponent_regist-> {
                return mContext.getString(R.string.nemesis)
            }
            R.id.first_regist -> {
                return mContext.getString(R.string.first_turn)
            }
            R.id.second_regist -> {
                return mContext.getString(R.string.second_turn)
            }
        }

        return ""
    }

//    DEBUG用関数、削除予定
    /** データを検索  */
    private fun searchByCount(db: SQLiteDatabase): String {
//        Cursorを確実にcloseするために、try{}～finally{}にする
        var cursor : Cursor? = null
        try {
            // name_book_tableからnameとageのセットを検索する
            // ageが指定の値であるものを検索
            cursor = db.query("battles_logs", dbCaramArray,
//                    TODO DEBUG用、削除予定
                    "result = ?", arrayOf("" + "Win"), null, null, null)

            // 検索結果をcursorから読み込んで返す
            return readCursor(cursor!!)
        } finally {
            // Cursorを忘れずにcloseする
            cursor?.close()
        }
    }

    // DEBUG用関数、削除予定
    //    /** 検索結果の読み込み  */
    private fun readCursor(cursor: Cursor): String {
        val result = ""

        // まず、Cursorからnameカラムとageカラムを
        // 取り出すためのインデクス値を確認しておく
        val indexList = mutableListOf<Int>()

        for(dbCaram in dbCaramArray){
            indexList.add(cursor.getColumnIndex(dbCaram))
        }

        // ↓のようにすると、検索結果の件数分だけ繰り返される
        while (cursor.moveToNext()) {
            var tmp = ""
            // 検索結果をCursorから取り出す
            for (indexDb in indexList){
                tmp += cursor.getString(indexDb) + " "
            }
            Log.i(TAG, tmp)
        }
        return result
    }

    /*    外部ストレージにアクセスする権限の許可の設定を表示する関数 */
    private fun checkWriteExternalPermission() {
        if(ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                AlertDialog.Builder(mContext)
                        .setTitle("許可が必要です")
                        .setMessage("画像ファイルを使用するために、WRITE_EXTERNAL_STORAGEを許可してください")
                        .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                            requestWriteExternalStorage()
                        }
                        .setNegativeButton("Cancel") { _: DialogInterface, _: Int ->
                            showToast("画像ファイルの使用が許可されなかったので、画像ファイルを使用できません")
                        }
                        .show()
            } else {
                requestWriteExternalStorage()
            }
        }
    }

    private fun requestWriteExternalStorage() {
        ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STROGE)
    }

//    requestPermissionsの実行後に呼び刺される関数
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STROGE -> {
//                ユーザが許可したとき
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    showToast("画像を使用できます。")
                } else {
//                    ユーザが許可しなかったとき
//                    許可されなかったため機能が実行できないことを表示する
                    showToast("画像ファイルの使用が許可されなかったので、画像ファイルを使用できません。")
                }
            }
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

    /*    Activity,Fragmentを破棄する前に保存を行う関数 */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
//        入力画像を保存
        if (mImage.drawable != null){
            outState.putInt("request_code", mRequestCode)
            if (mRequestCode == TAKE_PICTURE){
                outState.putString("input_bitmap_name", mInputBitmapName)
            } else if(mRequestCode == CHOICE_PICTURE){
                outState.putParcelable("uri", mUri)
            }
        }
//        認識結果を表示するViewの表示状態を保存
        if(mCardNameList[0].visibility == View.VISIBLE){
            outState.putBoolean("visibility", true)
        }
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

    //    Toast表示のための関数
    private fun showToast(s: String) {
        val msg = Toast.makeText(mContext, s, Toast.LENGTH_LONG)
        msg.show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MysizeFragment.
         */
        @JvmStatic
        fun newInstance() =
                RegistrationFragment().apply {
                    arguments = Bundle()
                }
    }
}
