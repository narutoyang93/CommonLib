package com.naruto.commonlib.example

import android.widget.Button
import com.naruto.lib.common.NormalText
import com.naruto.lib.common.base.BaseActivity
import com.naruto.lib.common.utils.DialogFactory

class MainActivity : BaseActivity() {

    override fun init() {

        findViewById<Button>(R.id.btn_normal).setOnClickListener {
            DialogFactory.showHintDialog(this, NormalText("这是普通弹窗"), NormalText("标题"))
        }

        findViewById<Button>(R.id.btn_alert).setOnClickListener {
            DialogFactory.showNativeDialog(this, "标题", "这是原生弹窗", onConfirm = {})
        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_main
}