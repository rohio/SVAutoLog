package com.example.shadowlogauto

import android.content.Context
import android.content.res.ColorStateList
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.example.shadowlogauto.custominterface.OnBackPressedListener
import com.example.shadowlogauto.database.DatabaseHelper

import com.example.shadowlogauto.model.CaramDb

// logListFragmentのレイアウト内でのrecyclerViewのindex
private const val RECYCLER_VIEW_INDEX = 0

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [LogListDeleteFragment.OnListFragmentInteractionListener] interface.
 */
class LogListDeleteFragment : Fragment(), OnBackPressedListener {
//    RecyclerViewのlist表示の列数
    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var mContext : Context

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mDeleteChoiceButton : Button

    private lateinit var mDbHelper: DatabaseHelper
    private lateinit var db : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //        SQLiteを使ってDB管理
        mDbHelper = DatabaseHelper(mContext)
        db = mDbHelper.writableDatabase

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_loglistdelete_list, container, false)

//        searchLogFragmentから渡された検索結果を取得し、MutableList<CaramDb>にキャスト
//        Suppressアノテーションでas Array<CaramDb>のキャストのWARNINGを回避。現状Suppressアノテーションでの回避策しかない。
        @Suppress("UNCHECKED_CAST")
        val searchResultList = (arguments!!.get("SEARCH_RESULT") as Array<CaramDb>).toMutableList()

//        LogListFragmentからrecyclerViewを取得
        mRecyclerView = (view as LinearLayout).getChildAt(RECYCLER_VIEW_INDEX) as RecyclerView

//        RecyclerViewの1つ1つの要素の間に区切り線を描画
        val itemDecoration = DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL)
        mRecyclerView.addItemDecoration(itemDecoration)

        // Set the adapter
        with(mRecyclerView) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(mContext)
                else -> GridLayoutManager(mContext, columnCount)
            }
            adapter = MyLogListDeleteRecyclerViewAdapter(searchResultList, listener)
        }

//        Deleteボタンを押したとき
        mDeleteChoiceButton = view.findViewById(R.id.delete_choice_button) as Button
        mDeleteChoiceButton.setOnClickListener {
//            チェックされたチェックボックスのrecyclerViewのindexを取得
            val deletePositionMap = (mRecyclerView.adapter as MyLogListDeleteRecyclerViewAdapter).mDeletePosition

//            mDeletePositionを初期化
            (mRecyclerView.adapter as MyLogListDeleteRecyclerViewAdapter).mDeletePosition = sortedMapOf()

/*            削除するデータをListに格納 */
            val deleteSearchDataList = mutableListOf<CaramDb>()
            for (deletePosition in deletePositionMap){
                deleteSearchDataList.add(searchResultList[deletePosition.key])
            }

/*            recyclerViewから削除したデータのItemViewを削除するためにsearchResultListから削除 */
            val iterator = searchResultList.listIterator()
            var iteratorIndex = iterator.nextIndex() -1
            loop@ for (deletePosition in deletePositionMap){
                while(iterator.hasNext()){
                    iterator.next()
                    iteratorIndex++
                    if (deletePosition.key == iteratorIndex){
                        iterator.remove()
                        continue@loop
                    }
                }
            }

/*            DBから削除 */
            var deleteNum = 0
            if (deleteSearchDataList.size > 0){
                deleteSearchDataList.forEach {
                    deleteNum += db.delete("default_table", "_id = ?", arrayOf(it.id.toString()))
                }
            }

            showToast("$deleteNum 件削除しました。")

/*            Deleteボタンを非活性にする */
            mDeleteChoiceButton.isEnabled = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mDeleteChoiceButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.cool_gray))
            }

//            recyclerViewの変化を適用
            (mRecyclerView.adapter as MyLogListDeleteRecyclerViewAdapter).notifyDataSetChanged()
        }

        return view
    }

/*    デバイスのバックボタンを押した際の処理
*     LogListFragmentを飛ばして、LogSearchFragmentに戻る */
    override fun onBackPressed() {
//        supportFragmentManagerを取得
        val fragmentManager = activity!!.supportFragmentManager

        val found = fragmentManager.findFragmentByTag(LogListDeleteFragment::class.java.simpleName)
        fragmentManager.beginTransaction().remove(found!!).commit()
        fragmentManager.popBackStack()
    }

    //    Toast表示のための関数
    private fun showToast(s: String) {
        val msg = Toast.makeText(mContext, s, Toast.LENGTH_LONG)
        msg.show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
            mContext = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
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
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: CaramDb?)
    }

    companion object {
        const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance() =
                LogListDeleteFragment().apply {
                    arguments = Bundle()
                }
    }
}
