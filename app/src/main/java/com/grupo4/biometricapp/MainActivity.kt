package com.grupo4.biometricapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.grupo4.biometricapp.databinding.ActivityMainBinding
import com.grupo4.biometricapp.viewmodels.MainViewModel
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var binding: ActivityMainBinding

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val mainViewModel : MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        initListener()
        initObservables()
        autenticationVariables()
        mainViewModel.checkBiometric(this)

    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.etxtUser.visibility = View.GONE
            binding.etxtPassword.visibility = View.GONE

            binding.imgFinger.visibility = View.VISIBLE
            binding.txtInfo.text = getString(R.string.biometric_success)

        }else{
            binding.imgFinger.visibility = View.GONE
            binding.txtInfo.text = getString(R.string.no_user)
        }
    }

    private fun initListener(){
        binding.imgFinger.setOnClickListener{
            biometricPrompt.authenticate(promptInfo)
        }
        binding.btnSaveUser.setOnClickListener{
            createNewUsers(binding.etxtUser.text.toString(), binding.etxtPassword.text.toString())
        }
        binding.btnSingUser.setOnClickListener{
            signInUsers(
                binding.etxtUser.text.toString(),
                binding.etxtPassword.text.toString()
            )
        }
    }

    private fun autenticationVariables(){
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startActivity(Intent(this@MainActivity, MainActivity2::class.java))
                }

            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()


    }

    private fun initObservables(){
        mainViewModel.resultCheckBiometric.observe(this){code ->
            when(code){
                BiometricManager.BIOMETRIC_SUCCESS -> {


                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    binding.txtInfo.text = getString(R.string.biometric_no_hardware)

                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                    }
                    startActivityForResult(enrollIntent, 100)
                }
            }

        }
    }

    private fun createNewUsers(user:String, password:String){
        auth.createUserWithEmailAndPassword(user, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "createUserWithEmail:success")
                    val user = auth.currentUser
                    Snackbar.make(this,
                        binding.etxtUser,
                        "createUserWithEmail:success",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.etxtUser.text.clear()
                    binding.etxtPassword.text.clear()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("TAG", "createUserWithEmail:failure", task.exception)
                    Snackbar.make(
                        this,
                        binding.etxtUser,
                        task.exception!!.message.toString(),
                        Snackbar.LENGTH_LONG
                    ).show()

                    Log.d("TAG", task.exception!!.stackTraceToString())
                }
            }
    }

    private fun signInUsers(email:String, password: String){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    startActivity(Intent(this, MainActivity2::class.java))
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("TAG", "signInWithEmail:failure", task.exception)
                    Snackbar.make(
                        this,
                        binding.etxtUser,
                        "signInWithEmail:failure",
                        Snackbar.LENGTH_LONG
                    ).show()

                }
            }
    }


}