/**
 * 文件说明： MainActivity.kt
 * 作用： 定义应用主入口 Activity，负责协调顶层导航切换。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.zhengyang.redbook.broadcast.MyBroadcastReceiver
import com.zhengyang.redbook.databinding.ActivityMainBinding
import com.zhengyang.redbook.databinding.LayoutPublishOptionsSheetBinding
import com.zhengyang.redbook.ui.home.HomeFragment
import com.zhengyang.redbook.ui.message.MessageFragment
import com.zhengyang.redbook.ui.my.MyFragment
import com.zhengyang.redbook.ui.placeholder.SimplePageFragment
import com.zhengyang.redbook.ui.publish.PublishTextActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPage: Page? = null
    private var pendingPublishMode: PublishTextActivity.Mode? = null

    private lateinit var receiver: MyBroadcastReceiver
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            val mode = pendingPublishMode
            pendingPublishMode = null
            if (granted && mode != null) {
                openPublishPage(mode)
            } else if (mode != null) {
                Toast.makeText(this, R.string.permission_required_message, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.apply {
            Page.entries.forEach { page ->
                addTab(newTab().setText(page.titleResId).setTag(page))
            }

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val page = tab.tag as? Page ?: return
                    if (page == Page.Add) {
                        showPublishOptionsSheet()
                        binding.bottomNavigation.getTabAt((currentPage ?: Page.Home).ordinal)?.select()
                        return
                    }
                    showPage(page)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            })
        }

        if (savedInstanceState == null) {
            showPage(Page.Home)
        }
        binding.bottomNavigation.getTabAt(Page.Home.ordinal)?.select()
        binding.centerAddButton.setOnClickListener {
            showPublishOptionsSheet()
        }

        receiver = MyBroadcastReceiver()
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(MyBroadcastReceiver.ACTION_CUSTOM_BROADCAST),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun showPage(page: Page) {
        if (page == currentPage) {
            return
        }

        currentPage = page
        supportFragmentManager.beginTransaction()
            .replace(R.id.pageContainer, page.createFragment(), page.name)
            .commit()
    }

    private fun showPublishOptionsSheet() {
        val sheetBinding = LayoutPublishOptionsSheetBinding.inflate(LayoutInflater.from(this))
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.optionAlbum.setOnClickListener {
            dialog.dismiss()
            openPublishEntryWithPermission(PublishTextActivity.Mode.ALBUM)
        }
        sheetBinding.optionCamera.setOnClickListener {
            dialog.dismiss()
            openPublishEntryWithPermission(PublishTextActivity.Mode.CAMERA)
        }
        sheetBinding.optionText.setOnClickListener {
            dialog.dismiss()
            openPublishPage(PublishTextActivity.Mode.TEXT)
        }
        sheetBinding.optionCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    private fun openPublishEntryWithPermission(mode: PublishTextActivity.Mode) {
        val permissions = requiredPermissionsFor(mode)
        if (permissions.isEmpty() || permissions.all(::hasPermission)) {
            openPublishPage(mode)
            return
        }

        pendingPublishMode = mode
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun openPublishPage(mode: PublishTextActivity.Mode) {
        startActivity(PublishTextActivity.createIntent(this, initialMode = mode))
    }

    private fun requiredPermissionsFor(mode: PublishTextActivity.Mode): List<String> = when (mode) {
        PublishTextActivity.Mode.ALBUM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        PublishTextActivity.Mode.CAMERA -> listOf(Manifest.permission.CAMERA)
        PublishTextActivity.Mode.TEXT -> emptyList()
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private enum class Page(
        val titleResId: Int
    ) {
        Home(R.string.nav_home),
        Market(R.string.nav_market),
        Add(R.string.nav_add_placeholder),
        Message(R.string.nav_message),
        Me(R.string.nav_me);

        fun createFragment(): Fragment = when (this) {
            Home -> HomeFragment()
            Market -> SimplePageFragment.newInstance("市集")
            Add -> SimplePageFragment.newInstance("发布")
            Message -> MessageFragment()
            Me -> MyFragment()
        }
    }
}
