package org.sjhstudio.integritychecker

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.system.Os
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.sjhstudio.integritychecker.databinding.ActivityMainBinding
import org.sjhstudio.integritychecker.integrity.IntegrityState
import org.sjhstudio.integritychecker.service.RootingCheckerService

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // RootingCheck
    private var rootingCheckerServiceBinder: IRootingCheckerService? = null
    private val rootingCheckerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("sjh", "RootingCheckerService bound")
            rootingCheckerServiceBinder = IRootingCheckerService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("sjh", "RootingCheckerService unbound")
            rootingCheckerServiceBinder = null
        }
    }

    override fun onStart() {
        super.onStart()
        bindRootingCheckerService()
    }

    override fun onStop() {
        super.onStop()
        unbindRootingCheckerService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        observeData()
    }

    private fun initViews() {
        with(binding) {
            btnCheck.setOnClickListener {
                setProgress(true)
                viewModel.initIntegrityState()
                viewModel.getToken(this@MainActivity)
            }
            btnCheck2.setOnClickListener {
                var isMagisk: Boolean
                try {
                    Log.d("sjh", "UID:" + Os.getuid())
                    rootingCheckerServiceBinder?.let { binder ->
                        isMagisk = binder.isMagiskPresent
                        if (isMagisk) {
                            Toast.makeText(applicationContext, "Magisk Found", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(applicationContext, "Magisk Not Found", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deviceIntegrityState.collectLatest { setIcon(binding.ivDeviceIntegrity, it) }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.basicIntegrityState.collectLatest { setIcon(binding.ivBasicIntegrity, it) }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.strongIntegrityState.collectLatest { setIcon(binding.ivStrongIntegrity, it) }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { showErrorDialog(it) }
            }
        }
    }

    private fun setProgress(isLoading: Boolean) {
        binding.progress.isVisible = isLoading
        binding.btnCheck.isEnabled = isLoading.not()
    }

    private fun setIcon(icon: ImageView, state: IntegrityState) {
        icon.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                when (state) {
                    IntegrityState.UnKnown -> {
                        R.drawable.ic_unknown
                    }

                    IntegrityState.Pass -> {
                        setProgress(false)
                        R.drawable.ic_pass
                    }

                    IntegrityState.Fail -> {
                        setProgress(false)
                        R.drawable.ic_fail
                    }
                }
            )
        )
    }

    private fun showErrorDialog(message: String) {
        setProgress(false)
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton("확인") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
            .show()
    }

    private fun bindRootingCheckerService() {
        Intent(applicationContext, RootingCheckerService::class.java).run {
            applicationContext.bindService(this, rootingCheckerServiceConnection, Service.BIND_AUTO_CREATE)
        }
    }

    private fun unbindRootingCheckerService() {
        unbindService(rootingCheckerServiceConnection)
    }
}