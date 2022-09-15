package com.naruto.commonlib.example

import android.Manifest
import android.util.Pair
import android.util.SparseArray
import android.widget.Button
import android.widget.Toast
import com.naruto.lib.common.Extension.putIfAbsent
import com.naruto.lib.common.base.BaseActivity
import com.naruto.lib.common.helper.PermissionHelper

class MainActivity : BaseActivity() {
    private val LOCATION_PERMISSIONS =
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )


    override fun init() {
        findViewById<Button>(R.id.btn_test).setOnClickListener {
            doWithPermission(object : PermissionHelper.RequestPermissionsCallback(
                Pair("你TM同意授权就行了，管那么多呢你！", LOCATION_PERMISSIONS)
            ) {
                override fun onGranted() {
                    Toast.makeText(this@MainActivity, "ok", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_main
}