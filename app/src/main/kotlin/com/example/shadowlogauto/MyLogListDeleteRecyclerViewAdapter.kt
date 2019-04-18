package com.example.shadowlogauto

import android.content.res.ColorStateList
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import com.example.shadowlogauto.LogListDeleteFragment.OnListFragmentInteractionListener
import com.example.shadowlogauto.model.CaramDb

import kotlinx.android.synthetic.main.fragment_loglistdelete.view.*
import java.util.*

//    カード枚数の定数
const val CARD_NUM = 3

/**
 * [RecyclerView.Adapter] that can display a [CaramDb] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class MyLogListDeleteRecyclerViewAdapter(
        private val mValues: List<CaramDb>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<MyLogListDeleteRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    var mDeletePosition = sortedMapOf<Int, Boolean>()
    private lateinit var mParentView : LinearLayout

    init {
//        RecyclerViewの要素の1つをタップした際に呼ばれる関数
//        タップされた際に、カード情報の表示/非表示を切り替える処理
         mOnClickListener = View.OnClickListener { v ->
             val item = v.tag as CaramDb
             // Notify the active callbacks interface (the activity, if the fragment is attached to
             // one) that an item has been selected.
             mListener?.onListFragmentInteraction(item)

//            タップしたRecyclerViewの要素の1つをLinearLayout型で取得
             val vLinearLayout = v as LinearLayout

//            タップされた際に、カード情報の表示/非表示を切り替え
//            ChildAt(0):自分/相手クラス,勝敗,先攻/後攻を表示しているView
//            ChildAt(2～[childCount - 1]):カード情報を表示しているView なので、2～[childCount - 1]でループ処理
             for (i in 2..(vLinearLayout.childCount - 1)) {
                 if (vLinearLayout.getChildAt(i).visibility == View.GONE) {
                     vLinearLayout.getChildAt(i).visibility = View.VISIBLE
                 } else if (vLinearLayout.getChildAt(i).visibility == View.VISIBLE) {
                     vLinearLayout.getChildAt(i).visibility = View.GONE
                 }
             }
         }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_loglistdelete, parent, false)

        return ViewHolder(view)
    }

//    resources.getColorがdeprecatedだが、minAPIを16とし、採用しているため、@Suppress
    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        RecyclerViewの要素の1つをitemに格納
        val item = mValues[position]

//        RecyclerView内の各Viewに値をセット
        holder.mResultView.text = item.result
        if (item.result == "Win"){
            holder.mResultView.setTextColor(holder.mView.resources.getColor(R.color.colorAccent))
        } else {
            holder.mResultView.setTextColor(holder.mView.resources.getColor(R.color.silver))
        }

        holder.mTurnView.text = item.turn
        if (item.turn == "先攻"){
            holder.mTurnView.setTextColor(holder.mView.resources.getColor(R.color.cornsilk))
        } else {
            holder.mTurnView.setTextColor(holder.mView.resources.getColor(R.color.palegoldenrod))
        }

        holder.mMyDeckView.text = item.myDeck
        holder.mOpponentDeckView.text = item.opponentDeck
        holder.mTimeStamp.text = item.timeStamp

        for (index in 0 until CARD_NUM){
            if (index < item.cardName.size){
                holder.mCardNameViewList[index].text = item.cardName[index]
                holder.mMulliganViewList[index].text = item.cardMulligan[index]
            } else {
                holder.mCardNameViewList[index].text = ""
                holder.mMulliganViewList[index].text = ""
            }
        }

//        カード名、マリガン結果のViewはDBから取得した値がnullの場合、削除
        for ((index, mCardNameView) in holder.mCardNameViewList.withIndex()) {
            if (TextUtils.isEmpty(mCardNameView.text)) {
                holder.mCMLinearLayoutList[index].removeAllViews()
                continue
            } else if (!TextUtils.isEmpty(mCardNameView.text) && holder.mCMLinearLayoutList[index].childCount == 0){
                holder.mCMLinearLayoutList[index].addView(holder.mMulliganViewList[index])
                holder.mCMLinearLayoutList[index].addView(mCardNameView)
            }
//            SettingFragmentでカード名の表示を非表示に設定している場合、Viewを非表示
            if (logListViewVisibility) {
                (mCardNameView.parent as LinearLayout).visibility = View.GONE
            }
        }

//        setImageResourceで使用するために、myClass,opponentClassを文字列からresourceのidに変換
        val classIntList = setClassImage(arrayListOf(item.myClass, item.opponentClass))

        holder.mMyClassView.setImageResource(classIntList[0])
        holder.mOpponentClassView.setImageResource(classIntList[1])
//        チェックを外す
        holder.mCheckBox.isChecked = false

        /* チェック状態が変化した時の処理 */
        holder.mCheckBox.setOnClickListener {
//            チェックしたpositionをmDeletePositionに保持
            if ((it as CheckBox).isChecked){
                mDeletePosition[position] = true
            } else {
                mDeletePosition.remove(position)
            }
/*            チェックが1つもされていない場合、Deleteボタンを非活性化 */
            val mDeleteChoiceButton = mParentView.getChildAt(1) as Button
            mDeleteChoiceButton.isEnabled = mDeletePosition.size != 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mDeletePosition.size != 0){
                    mDeleteChoiceButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.mView.context, R.color.chocolate))
                } else {
                    mDeleteChoiceButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.mView.context, R.color.cool_gray))
                }
            }
        }

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
//        RecyclerView内の各Viewを取得
        val mResultView: TextView = mView.result_ld
        val mTurnView: TextView = mView.turn_ld
        val mMyClassView : ImageView = mView.my_class_ld
        val mMyDeckView : TextView = mView.my_deck_ld
        val mOpponentClassView : ImageView = mView.opponent_class_ld
        val mOpponentDeckView : TextView = mView.opponent_deck_ld
        val mCardNameViewList = mutableListOf<TextView>()
        val mMulliganViewList = mutableListOf<TextView>()
        val mTimeStamp : TextView = mView.timestamp_ld
        val mCheckBox : CheckBox = mView.delete_check
        val mCMLinearLayoutList = mutableListOf<LinearLayout>()

        init {
            mCardNameViewList.add(mView.card_name1_ld)
            mCardNameViewList.add(mView.card_name2_ld)
            mCardNameViewList.add(mView.card_name3_ld)
            mMulliganViewList.add(mView.mulligan1_ld)
            mMulliganViewList.add(mView.mulligan2_ld)
            mMulliganViewList.add(mView.mulligan3_ld)
            mCMLinearLayoutList.add(mView.cm_linear_layout1)
            mCMLinearLayoutList.add(mView.cm_linear_layout2)
            mCMLinearLayoutList.add(mView.cm_linear_layout3)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

//        DELETEボタンの活性状態を操作する際に、親View配下のメソッドを使用するため、parentをプロパティに保存
        mParentView = recyclerView.parent as LinearLayout
    }

    //    クラスのStringからリソースのIDに変換する関数
    private fun setClassImage(classStrList: ArrayList<String>): MutableList<Int> {
        val classIntList = mutableListOf<Int>()

        for(classStr in classStrList) {
            when (classStr) {
                "E" -> {
                    classIntList.add(R.drawable.elf_leader)
                }
                "R" -> {
                    classIntList.add(R.drawable.royal_leader)
                }
                "W" -> {
                    classIntList.add(R.drawable.witch_leader)
                }
                "D" -> {
                    classIntList.add(R.drawable.dragon_leader)
                }
                "Nc" -> {
                    classIntList.add(R.drawable.necromancer_leader)
                }
                "V" -> {
                    classIntList.add(R.drawable.vampire_leader)
                }
                "B" -> {
                    classIntList.add(R.drawable.bishop_leader)
                }
                "Nm" -> {
                    classIntList.add(R.drawable.nemesis_leader)
                }
            }
        }

        return classIntList
    }
}
