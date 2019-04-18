package com.example.shadowlogauto

import android.support.v4.app.Fragment
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.shadowlogauto.custominterface.OnBackPressedListener
import com.example.shadowlogauto.model.CaramDb
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.lang.Exception

// logListFragmentでカード情報を表示するViewの表示or非表示を設定するパラメータ
var logListViewVisibility = false
var SVIntentVisibility = false

class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        RegistrationFragment.OnFragmentInteractionListener,
        LogSearchFragment.OnFragmentInteractionListener,
        SettingFragment.OnFragmentInteractionListener,
        LogListFragment.OnListFragmentInteractionListener,
        DeleteDataFragment.OnFragmentInteractionListener,
        LogListDeleteFragment.OnListFragmentInteractionListener{
    override fun onListFragmentInteraction(item: CaramDb?) {
    }

    override fun onFragmentInteraction(uri: Uri) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        logListViewVisibility = sharedPreferences.getBoolean("logListViewVisibility", false)
        SVIntentVisibility = sharedPreferences.getBoolean("SVIntentVisibility", false)
        if (SVIntentVisibility){
            fab.hide()
        }

        fab.setOnClickListener {
            val pm = packageManager
            val intent = pm.getLaunchIntentForPackage("jp.co.cygames.Shadowverse")
            try {
                startActivity(intent)
            } catch (e : Exception){
                Toast.makeText(this, "Shadowverseが見つかりませんでした。", Toast.LENGTH_SHORT).show()
            }
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onStop() {
        super.onStop()

/*        設定パラメータを保存 */
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editer = sharedPreferences.edit()
        editer.putBoolean("logListViewVisibility", logListViewVisibility)
        editer.putBoolean("SVIntentVisibility", SVIntentVisibility)
        editer.apply()
    }

/*    デバイスのバックボタンを押した際に実行される関数 */
    override fun onBackPressed() {
/*        NavigationWindowを開いている状態か確認 */
//        開いている場合NavigationWindowを閉じる
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else { // 開いていない場合
            /* LogListDeleteFragmentである場合、カスタムリスナーのonBackPressed()を呼ぶ */
            val found = supportFragmentManager.findFragmentByTag(LogListDeleteFragment::class.java.simpleName)
            if (found != null && found is LogListDeleteFragment){
                (found as OnBackPressedListener).onBackPressed()
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
//        BackStackに積んだFragmentを削除
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_registration -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.container, Fragment.instantiate(this, "com.example.shadowlogauto.RegistrationFragment"))
                        .commit()
            }

            R.id.nav_log_list -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.container, Fragment.instantiate(this, "com.example.shadowlogauto.LogSearchFragment"))
                        .commit()
            }

            R.id.nav_setting -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.container, Fragment.instantiate(this, "com.example.shadowlogauto.SettingFragment"))
                        .commit()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
