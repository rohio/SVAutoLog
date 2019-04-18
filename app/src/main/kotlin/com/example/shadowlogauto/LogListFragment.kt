package com.example.shadowlogauto

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

import com.example.shadowlogauto.model.CaramDb
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.data.*

//    logListFragmentのレイアウト内でのrecyclerViewのindex
private const val RECYCLER_VIEW_INDEX = 1

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [LogListFragment.OnListFragmentInteractionListener] interface.
 */
class LogListFragment : Fragment() {
//    RecyclerViewのlist表示の列数
    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var mContext : Context

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_log_list, container, false)

//        searchLogFragmentから渡された検索結果を取得し、List<CaramDb>にキャスト
//        Suppressアノテーションでas Array<CaramDb>のキャストのWARNINGを回避。現状Suppressアノテーションでの回避策しかない。
        @Suppress("UNCHECKED_CAST")
        val searchResultList = (arguments!!.get("SEARCH_RESULT") as Array<CaramDb>).toMutableList()

//        自分のクラスの割合を示す円グラフのViewを取得
        val mMyClassPieChart = view.findViewById(R.id.my_class_pie_chart) as PieChart
/*        自分のクラスを格納したリストを作成 */
        val myClassList = mutableListOf<String>()
        for(searchResult in searchResultList){
            myClassList.add(searchResult.myClass)
        }
//        setupPieChartViewで自分のクラスの割合を示す円グラフを描画
        setupPieChartView(mMyClassPieChart, myClassList, "自分のクラス")

//        相手のクラスの割合を示す円グラフのViewを取得
        val mOpponentClassPieChart = view.findViewById(R.id.opponent_class_pie_chart) as PieChart
        val opponentClassList = mutableListOf<String>()
        for(searchResult in searchResultList){
            opponentClassList.add(searchResult.opponentClass)
        }
//        setupPieChartViewで相手のクラスの割合を示す円グラフを描画
        setupPieChartView(mOpponentClassPieChart, opponentClassList, "相手のクラス")

//        勝率を示すViewを取得
        val mWinRatioTextView = view.findViewById(R.id.win_rate_num) as TextView
/*        勝利数を計算 */
        var winNum = 0f
        for(searchResult in searchResultList){
            if(searchResult.result == "Win"){
                winNum++
            }
        }
//        勝率をセット
        mWinRatioTextView.text = String.format("%.1f %%" , (winNum / searchResultList.size) * 100)

//        先行率を示すViewを取得
        val mTurnRatioTextView = view.findViewById(R.id.turn_rate_num) as TextView
/*        先行数を計算 */
        var firstTurnNum = 0f
        for(searchResult in searchResultList){
            if(searchResult.turn == "先攻"){
                firstTurnNum++
            }
        }
//        先攻率をセット
        mTurnRatioTextView.text = String.format("%.1f %%" , (firstTurnNum / searchResultList.size) * 100)

//        LogListFragmentからrecyclerViewを取得
        mRecyclerView = (view as LinearLayout).getChildAt(RECYCLER_VIEW_INDEX) as RecyclerView

//        RecyclerViewの1つ1つの要素の間に区切り線を描画
        val itemDecoration = DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL)
        mRecyclerView.addItemDecoration(itemDecoration)

/*        adapterを利用して、検索結果をrecyclerViewをセット */
        with(mRecyclerView) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(mContext)
                else -> GridLayoutManager(mContext, columnCount)
            }
//                検索結果をrecyclerViewにセット
            adapter = MyLogListRecyclerViewAdapter(searchResultList, listener)
        }

        val mGoDeleteListButton = view.findViewById(R.id.card_inf_open_toggle) as Button
        mGoDeleteListButton.setOnClickListener {
//            fragmentに遷移するためにfragmentManagerを取得
            val fragmentManager = activity?.supportFragmentManager

//            LogListFragmentにデータを渡すためのBundleを生成
            val bundle = Bundle()

/*            検索結果を遷移先のFragmentであるLogListFragmentに渡す */
//            intentにputExtraする際にListを扱えないため、Arrayを渡す
            bundle.putParcelableArray("SEARCH_RESULT", searchResultList.toTypedArray())

//                RecyclerViewのlist表示の列数を指定
//                TODO 2以上を指定した際に自然に表示されるようにする
//                TODO SettingFragmentで指定する形がベスト
            bundle.putInt("column-count", 1)

            if(fragmentManager != null){
                val fragmentTransaction = fragmentManager.beginTransaction()

//                BackStackには積まない(遷移先のFragmentで戻るボタンを押すと、遷移元のFragmentに戻らないようにする)

//                遷移先のFragmentにLogListFragmentを指定
                val fragmentLogListDelete = LogListDeleteFragment.newInstance()

//                fragmentに検索結果を格納したbundleをセット
                fragmentLogListDelete.arguments = bundle

//                LogListFragmentに遷移
//                LogListDeleteFragmentでカスタムリスナーのonBackPressed()を使用するために,第3引数にTAGを指定
                fragmentTransaction.replace(R.id.container, fragmentLogListDelete, LogListDeleteFragment::class.java.simpleName).commit()
            }
        }
        return view
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

    /*    自分/相手のクラスの割合を示す円グラフを描画する関数 */
    private fun setupPieChartView(mPieChart: PieChart, graphValueList: List<String>, titleChart: String) {
        mPieChart.setUsePercentValues(true)
//        mPieChart.setBackgroundColor(Color.LTGRAY)
        mPieChart.setHoleColor(Color.LTGRAY)
//        グラフの説明部を設定
        val description = Description()
        description.textColor = Color.WHITE
        description.text = titleChart
        description.textSize = 10f
        mPieChart.description = description

//        凡例
        mPieChart.legend.isEnabled = false

//        calculateClass関数を使用し、円グラフに描画するクラスと割合を格納したList<Pair<String, Float>>を取得
        val classRatioList = calculateClass(graphValueList)

//        円グラフに表示する割合を格納するListを生成
        val entries = mutableListOf<PieEntry>()
//        円グラフに表示する割合をListに格納
        for (classRatio in classRatioList){
            entries.add(PieEntry(classRatio.second, classRatio.first))
        }

//        PieDataSetを使用し、円グラフとグラフのラベルを描画
        val dataSet = PieDataSet(entries, "")
//        グラフの要素の色を指定
        dataSet.setColors(*ColorTemplate.COLORFUL_COLORS)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f
        dataSet.setDrawValues(true)

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter())
        pieData.setValueTextSize(12f)
        pieData.setValueTextColor(Color.WHITE)

        // 表示アニメーション
        mPieChart.animateXY(1000, 1000)

//        Viewに円グラフをセット
        mPieChart.data = pieData
    }

    /*    円グラフに描画するクラスと割合を格納したList<Pair<String, Float>>を返却する関数 */
    private fun calculateClass(graphValueList: List<String>): MutableList<Pair<String, Float>> {
//        各クラスの個数を格納するList
        val classNumList = mutableListOf(Pair(mContext.getString(R.string.elf), 0f),
                Pair(mContext.getString(R.string.royal), 0f),
                Pair(mContext.getString(R.string.witch), 0f),
                Pair(mContext.getString(R.string.dragon), 0f),
                Pair(mContext.getString(R.string.necromancer), 0f),
                Pair(mContext.getString(R.string.vampire), 0f),
                Pair(mContext.getString(R.string.bishop), 0f),
                Pair(mContext.getString(R.string.nemesis), 0f))

//        各クラスの個数を格納
        for(graphValue in graphValueList) { // 引数に渡された各検索結果(caramDb型)でループ
            for ((index, classNum) in classNumList.withIndex()){ // 各クラスの割合を格納するListでループ
                if(graphValue == classNum.first){
                    classNumList[index] = Pair(classNum.first, classNum.second + 1f)
                    break
                }
            }
        }

//        割合を計算するために合計の値を定義
        var sum = 0f
//        各クラスの割合を格納するList
        val classRatioList = mutableListOf<Pair<String, Float>>()
//        classNumListからクラスの個数が0のクラスを省いたものをclassRatioListに格納
        for (classNum in classNumList){
            if(classNum.second != 0f){
                classRatioList.add(classNum)
            }
//            合計値にクラスの個数を加算
            sum += classNum.second
        }

//        各クラスの個数を格納したListから各クラスの割合を格納したListに変換
        for ((index, classRatio) in classRatioList.withIndex()){
            classRatioList[index] = Pair(classRatio.first, classRatio.second / sum)
        }

//        各クラスの割合を格納したListを返却
        return classRatioList
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
                LogListFragment().apply {
                    arguments = Bundle()
                }
    }
}
