package com.androar.buyhatke

import android.R
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory


class MainActivity : AppCompatActivity() {

    lateinit var progressdialog : ProgressDialog
    private val webViewMyntra by lazy { findViewById<WebView>(com.androar.buyhatke.R.id.webviewMyntra) }
    private val fabDiscount by lazy { findViewById<FloatingActionButton>(com.androar.buyhatke.R.id.fabDiscount) }

    /**
     * It is known that the progress dialog is deprecated, however I couldn't find a suitable alternative for the
     * the given requirements since I had to show it on runtime.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.androar.buyhatke.R.layout.activity_main)
        progressdialog = ProgressDialog(this)

        setupWebView()

        fabDiscount.setOnClickListener {
            getDiscounts() }

    }

    /**
     * The setupwebview function is used to display the Myntra website in the home layout
     * This function also used to show the FAB button only the specified URL and not throughout the app.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webViewMyntra.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (webViewMyntra.url.toString() == getString(com.androar.buyhatke.R.string.myntra_checkout_url)) {
                    fabDiscount.visibility = View.VISIBLE
                }
                else {
                    fabDiscount.visibility = View.INVISIBLE
                }
                super.onPageFinished(view, url)
            }
        }
        webViewMyntra.settings.javaScriptEnabled = true
        webViewMyntra.settings.domStorageEnabled = true;
        webViewMyntra.settings.userAgentString = getString(com.androar.buyhatke.R.string.user_agent_string)
        webViewMyntra.loadUrl(getString(com.androar.buyhatke.R.string.myntra_url))
    }


    /**
     * The get discounts function is used to fetch discounts from the API using retrofit library it also calls the
     * apply discounts function.
     */
    private fun getDiscounts()  {
        progressdialog.setCancelable(false);
        progressdialog.setMessage("Finding Discounts")
        progressdialog.show()

        val retrofit = Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl("http://coupons.buyhatke.com/PickCoupon/FreshCoupon/")
            .build()
        val api = retrofit.create(API::class.java)
        val call = api.getDiscounts()
        call!!.enqueue(object : Callback<String?> {
            override fun onResponse(
                call: Call<String?>?,
                response: Response<String?>
            ) {
                if (response.isSuccessful()) {
                    val responseString: String? = response.body()
                    var discounts: List<String> = responseString!!.split("~")
                    discounts = discounts.subList(0, discounts.size - 1) //Last parameter is empty in URL
                    applyDiscounts(discounts)
                }
            }

            override fun onFailure(call: Call<String?>?, t: Throwable?) {
                Log.d("error",t!!.message.toString());

                Toast.makeText(this@MainActivity, "Congratulations! You broke my code", Toast.LENGTH_LONG).show()
            }
        })

    }

    /**
     * Apply Discounts function acts as the core function of the app, where it runs a for loop applying all discounts of the list,
     * adds them to an array to find the max element
     * and then further applies it one last time for the coupon which gives the max discount.
     * It also shows a dialog box of the max element.
     */
    private fun applyDiscounts(discounts : List<String>) {
        val savings : ArrayList<String> = arrayListOf()
        val maxDiscount = 0
        var maxElement = 0

        for (i in discounts.indices) {
            val discount = discounts.get(i)
            kotlin.run { progressdialog.setMessage("Applying $discount")}
            Log.d("applying", discount[i].toString())

            //Function to click the apply discount text from cart page.
            webViewMyntra.loadUrl("javascript:(function(){document.getElementsByClassName('coupons-base-title')[0].click();})()")
            webViewMyntra.clearCache(true)

            //Function to enter discount from list to HTML input
            webViewMyntra.loadUrl("javascript:(function(){document.getElementById('coupon-input-field').value = '$discount';})()")
            webViewMyntra.clearCache(true)

            //Function to click on the pink color thin apply button to see the Max savings
            webViewMyntra.loadUrl("javascript:(function(){document.getElementsByClassName('couponsForm-base-applyButton')[0].click();})()")
            webViewMyntra.clearCache(true)


            //Function to copy the Maxsavings text.
            webViewMyntra.evaluateJavascript("(function(){var node = document.getElementsByClassName('couponsForm-base-price')[0].innerText; return node;})()"
            ) { p0 ->
                val curDiscount = p0.replace("\"", "")
                if (Integer.parseInt(curDiscount) > maxDiscount) {
                    maxElement = i
                }
                savings.add(p0)
                Log.d("saving", p0) }
            webViewMyntra.clearCache(true)
        }

        /**
         * Now we have the max element stored in the maxDiscountedElement variable after applying all coupons in the API.
         */

        val maxDiscountedElement = discounts[maxElement]
        kotlin.run { progressdialog.setMessage("Applying $maxDiscountedElement")}

        //Function to enter discount from maxDiscount to HTML input
        webViewMyntra.loadUrl("javascript:(function(){document.getElementById('coupon-input-field').value = '$maxDiscountedElement';})()")
        webViewMyntra.clearCache(true)

        //Function to click on the pink color thin apply button.
        webViewMyntra.loadUrl("javascript:(function(){document.getElementsByClassName('couponsForm-base-applyButton')[0].click();})()")
        webViewMyntra.clearCache(true)

        //Function to click the big Apply button to go back to cart page.
        webViewMyntra.loadUrl("javascript:(function(){document.getElementById('applyCoupon').click() ;})()")
        webViewMyntra.clearCache(true)

        progressdialog.dismiss()
        val alertDialog: AlertDialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("The following coupons were applied:")
            .setMessage("$discounts\n\n" + "Max coupon was $maxDiscountedElement")
            .setPositiveButton("Okay",
                DialogInterface.OnClickListener { dialogInterface, i ->
                   dialogInterface.dismiss()
                })

            .show()
    }

}
