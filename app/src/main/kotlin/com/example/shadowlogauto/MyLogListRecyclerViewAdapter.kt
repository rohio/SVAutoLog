package com.example.shadowlogauto

import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.example.shadowlogauto.LogListFragment.OnListFragmentInteractionListener
import com.example.shadowlogauto.model.CaramDb

import kotlinx.android.synthetic.main.fragment_log.view.*

/**
 * [RecyclerView.Adapter] that can display a [CaramDb] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class MyLogListRecyclerViewAdapter(
        private val mValues: List<CaramDb>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<MyLogListRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

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
            for(i in 2..(vLinearLayout.childCount - 1)){
                if(vLinearLayout.getChildAt(i).visibility == View.GONE){
                    vLinearLayout.getChildAt(i).visibility = View.VISIBLE
                } else if(vLinearLayout.getChildAt(i).visibility == View.VISIBLE){
                    vLinearLayout.getChildAt(i).visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_log, parent, false)

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

        for ((index, cardName) in item.cardName.withIndex()){
            holder.mCardNameViewList[index].text = cardName
            holder.mMulliganViewList[index].text = item.cardMulligan[index]
        }
//        カード名、マリガン結果のViewはDBから取得した値がnullの場合、削除
        for ((index, mCardNameView) in holder.mCardNameViewList.withIndex()){
            if (TextUtils.isEmpty(mCardNameView.text)){
                (mCardNameView.parent as ViewGroup).removeView(mCardNameView)
                (holder.mMulliganViewList[index].parent as ViewGroup).removeView(holder.mMulliganViewList[index])
                continue
            }
//            SettingFragmentでカード名の表示を非表示に設定している場合、Viewを非表示
            if (logListViewVisibility){
                (mCardNameView.parent as LinearLayout).visibility = View.GONE
            }
        }

//        setImageResourceで使用するために、myClass,opponentClassを文字列からresourceのidに変換
        val classIntList = setClassImage(arrayListOf(item.myClass, item.opponentClass))

        holder.mMyClassView.setImageResource(classIntList[0])
        holder.mOpponentClassView.setImageResource(classIntList[1])

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
//        RecyclerView内の各Viewを取得
        val mResultView: TextView = mView.result
        val mTurnView: TextView = mView.turn
        val mMyClassView : ImageView = mView.my_class
        val mMyDeckView : TextView = mView.my_deck
        val mOpponentClassView : ImageView = mView.opponent_class
        val mOpponentDeckView : TextView = mView.opponent_deck
        val mCardNameViewList = mutableListOf<TextView>()
        val mMulliganViewList = mutableListOf<TextView>()
        val mTimeStamp: TextView = mView.timestamp

        init {
            mCardNameViewList.add(mView.card_name1)
            mCardNameViewList.add(mView.card_name2)
            mCardNameViewList.add(mView.card_name3)
            mMulliganViewList.add(mView.mulligan1)
            mMulliganViewList.add(mView.mulligan2)
            mMulliganViewList.add(mView.mulligan3)
        }
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
