package com.yourgpt.sdk.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yourgpt.sdk.YourGPTNotificationClient
import com.yourgpt.sdk.YourGPTSDK
import kotlinx.coroutines.launch

class HomeScreenActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        setupUI()
        initializeSDK()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }

    private fun setupUI() {
        viewPager = findViewById(R.id.viewPager)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val fragments = listOf(
            HomeFragment(),
            ExchangeFragment(),
            OrdersFragment(),
            SupportFragment()
        )

        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { viewPager.currentItem = 0; true }
                R.id.navigation_exchange -> { viewPager.currentItem = 1; true }
                R.id.navigation_orders -> { viewPager.currentItem = 2; true }
                R.id.navigation_support -> { viewPager.currentItem = 3; true }
                else -> false
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> bottomNavigationView.selectedItemId = R.id.navigation_home
                    1 -> bottomNavigationView.selectedItemId = R.id.navigation_exchange
                    2 -> bottomNavigationView.selectedItemId = R.id.navigation_orders
                    3 -> bottomNavigationView.selectedItemId = R.id.navigation_support
                }
            }
        })

        viewPager.setCurrentItem(3, false)
        bottomNavigationView.selectedItemId = R.id.navigation_support
    }

    private fun initializeSDK() {
        lifecycleScope.launch {
            YourGPTSDK.quickInitialize(this@HomeScreenActivity, MainActivity.WIDGET_UID)
        }
    }

    private fun handleNotificationIntent(intent: Intent) {
        if (YourGPTNotificationClient.handleNotificationClick(this, intent)) return

        if (intent.action == "com.yourgpt.sdk.OPEN_WIDGET") {
            viewPager.setCurrentItem(3, true)
            bottomNavigationView.selectedItemId = R.id.navigation_support

            val sessionUid = intent.getStringExtra("conversation_id")
            viewPager.postDelayed({
                if (sessionUid != null) {
                    YourGPTSDK.openSession(this, sessionUid)
                } else {
                    YourGPTSDK.show(this)
                }
            }, 300)
        }
    }

    private inner class ViewPagerAdapter(
        activity: AppCompatActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}