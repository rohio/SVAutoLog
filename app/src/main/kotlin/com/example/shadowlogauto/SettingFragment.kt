package com.example.shadowlogauto

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import kotlinx.android.synthetic.main.app_bar_main.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SettingFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SettingFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SettingFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var mContext : Context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

/*        logListFragmentでカード情報を表示するViewの表示or非表示を設定するパラメータを設定 */
        val mLogListVisibilityButton = view.findViewById(R.id.log_list_card_visibility) as ToggleButton
//        現在の表示設定にボタンのチェック状態を合わせる
        mLogListVisibilityButton.isChecked = logListViewVisibility
        mLogListVisibilityButton.setOnClickListener {
            //            表示設定変更ボタンを押した際に、logListViewVisibilityを変更
            logListViewVisibility = mLogListVisibilityButton.isChecked
        }

/*        shadowverseを起動するFloatingActionButtonの表示or非表示を設定 */
        val mSVIntentVisibilityButton = view.findViewById(R.id.shadowverse_intent) as ToggleButton
//        現在の表示設定にボタンのチェック状態を合わせる
        mSVIntentVisibilityButton.isChecked = SVIntentVisibility
        val fabButton = activity?.fab as FloatingActionButton
        mSVIntentVisibilityButton.setOnClickListener {
            SVIntentVisibility = mSVIntentVisibilityButton.isChecked
            if (SVIntentVisibility){ // shadowverseを起動するボタンを非表示
                fabButton.hide()
            } else { // shadowverseを起動するボタンを表示
                fabButton.show()
            }
        }

/*        データ削除画面へ遷移 */
        val mDeleteDataFragmentButton = view.findViewById(R.id.delete_data_fragment_move) as Button
        mDeleteDataFragmentButton.setOnClickListener {
            //            fragmentに遷移するためにfragmentManager,fragmentTransactionを取得
            val fragmentManager = activity?.supportFragmentManager
            val fragmentTransaction = fragmentManager?.beginTransaction()

//            BackStackに積む(遷移先のFragmentでデバイスの戻るボタンを押すと、遷移元のFragmentに戻るようにする)
            fragmentTransaction?.addToBackStack(null)

//            遷移先のFragmentにDeleteDataFragmentを指定
            val fragmentDeleteData = Fragment.instantiate(mContext, "com.example.shadowlogauto.DeleteDataFragment")

//            DeleteDataFragmentに遷移
            fragmentTransaction?.replace(R.id.container, fragmentDeleteData)?.commit()

        }

        return view
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
         * @return A new instance of fragment MemorialFragment.
         */
        @JvmStatic
        fun newInstance() =
                SettingFragment().apply {
                    arguments = Bundle()
                }
    }
}
