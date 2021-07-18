package com.example.whatsappclone

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.example.whatsappclone.databinding.ActivityOtpBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

const val PHONE_NUMBER = "phoneNumber"

class OtpActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityOtpBinding

    lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    var phoneNumber: String? = null
    var mVerificationId: String? = null
    var mResendToken: PhoneAuthProvider.ForceResendingToken? = null

    private lateinit var progressDialog: ProgressDialog
    private var mCounterDown: CountDownTimer? = null
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOtpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initViews()
        startVerify()// begin verification process. Make sure to call this function after you've
        // set the callbacks. i.e. after initViews()
    }

    private fun startVerify() {

        val options = PhoneAuthOptions.newBuilder()
                .setPhoneNumber(phoneNumber!!)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

        showTimer(60000)// Initialise countdown time with 60 seconds
//        progressDialog = createProgressDialog("Sending a verification code", false)
//        progressDialog.show()
    }

    private fun showTimer(milliSecInFuture: Long) {
        binding.resendBtn.isEnabled = false
        mCounterDown = object :
                CountDownTimer(milliSecInFuture, 1000) { // second parameter is Interval = 1second
            override fun onTick(millisUntilFinished: Long) {
                // Called after every second
                binding.counterTv.isVisible = true
                binding.counterTv.text = getString(R.string.seconds_remaining, millisUntilFinished / 1000)
                //here you can have your logic to set text to edittext
            }

            override fun onFinish() {
                // Called when count down timer reaches 0
                binding.resendBtn.isEnabled = true
                binding.counterTv.isVisible = false
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCounterDown != null) {
            mCounterDown!!.cancel()
        }
    }

    private fun initViews() {
        // Fetch the phone number for intent parameter
        phoneNumber = intent.getStringExtra(PHONE_NUMBER)
        binding.verifyTv.text = getString(
                R.string.verify_number,
                phoneNumber
        )// This will automatically generate Verify +919650212345
        setSpannableString()

        binding.verificationBtn.setOnClickListener(this)
        binding.resendBtn.setOnClickListener(this)

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }

                val smsCode = credential.smsCode
                if (!smsCode.isNullOrBlank())
                    binding.sentcodeEt.setText(smsCode)

                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.

                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Log.e("Exception:", "FirebaseTooManyRequestsException", e)
                }
                Log.e("ERROR_FIREBASE", e.localizedMessage)

                // Show a message and update the UI
                notifyUserAndRetry("Your Phone Number might be wrong or connection error. Try again!")
            }

            override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }
                binding.counterTv.isVisible = false

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                mResendToken = token
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {

        mAuth = FirebaseAuth.getInstance()
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        // You can also make a dialog here so that user does not do anything else
                        // while this is happening
                        // Move onto next Activity
                        if (::progressDialog.isInitialized) {
                            progressDialog.dismiss()
                        }
                        startActivity(Intent(this, SignUpActivity::class.java))
                        finish()
                    } else {
                        if (::progressDialog.isInitialized) {
                            progressDialog.dismiss()
                        }
                        notifyUserAndRetry("Your Phone Number verification failed. Try again!")
                    }
                }

    }

    private fun notifyUserAndRetry(message: String) {
        MaterialAlertDialogBuilder(this).apply {
            setMessage(message)
            setPositiveButton("Ok") { _, _ ->
                showLoginActivity()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            setCancelable(false)
            create()
            show()
        }
    }

    private fun setSpannableString() {
        val span = SpannableString(getString(R.string.waiting_text, phoneNumber))
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // send back to previous activity
                showLoginActivity()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // remove underline of clickable text
                ds.color = ds.linkColor // set the color of clickable text to default link color
            }
        }
        // Set the span: the area which is clickable
        span.setSpan(clickableSpan, span.length - 13, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // last flag integer is to make start and end index to be exclusive

        binding.waitingTv.movementMethod = LinkMovementMethod.getInstance()// for handling click
        binding.waitingTv.text = span
    }

    // to disable back button
    override fun onBackPressed() {
    }

    private fun showLoginActivity() {
        startActivity(
                Intent(this, LoginActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)// These flags handle
                // the backstack so that user cannot move back to previous activity
        )
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.verificationBtn -> {

                val code = binding.sentcodeEt.text.toString()
                if (code.isNotEmpty() && !mVerificationId.isNullOrBlank()) {
                    progressDialog = createProgressDialog("Please wait....", false)
                    progressDialog.show()
                    val credential = PhoneAuthProvider.getCredential(mVerificationId!!, code)
                    signInWithPhoneAuthCredential(credential)
                }
            }

            binding.resendBtn -> {
                if (mResendToken != null) {
                    showTimer(60000)
                    progressDialog = createProgressDialog("Sending a verification code", false)
                    progressDialog.show()

                    val options = PhoneAuthOptions.newBuilder()
                            .setPhoneNumber(phoneNumber!!)       // Phone number to verify
                            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                            .setActivity(this)                 // Activity (for callback binding)
                            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                            .setForceResendingToken(mResendToken!!)
                            .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                }
            }

        }
    }

}

fun Context.createProgressDialog(message: String, isCancelable: Boolean): ProgressDialog {
    return ProgressDialog(this).apply {
        setCancelable(false)
        setMessage(message)
        setCanceledOnTouchOutside(false)
    }
}