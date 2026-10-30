package com.example.xmedic_v100.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdsManager {

    companion object {
        private const val TAG = "XMEDIC_ADS"

        // ID oficial de anuncios recompensados de prueba
        private const val TEST_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/5224354917"
    }

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun load(
        context: Context,
        onLoaded: (Boolean) -> Unit = {}
    ) {
        if (isLoading) return

        isLoading = true

        RewardedAd.load(
            context,
            TEST_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false

                    Log.d(TAG, "Anuncio de prueba cargado")
                    onLoaded(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false

                    Log.e(TAG, "Error al cargar: ${error.message}")
                    onLoaded(false)
                }
            }
        )
    }

    fun show(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onClosed: () -> Unit = {}
    ) {
        val ad = rewardedAd

        if (ad == null) {
            Log.w(TAG, "No hay un anuncio preparado")
            return
        }

        rewardedAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Anuncio cerrado")
                    load(activity)
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(
                    error: AdError
                ) {
                    Log.e(TAG, "Error al mostrar: ${error.message}")
                    load(activity)
                    onClosed()
                }
            }

        ad.show(activity) { reward ->
            Log.d(
                TAG,
                "Recompensa de prueba obtenida: " +
                        "${reward.amount} ${reward.type}"
            )

            // Solo notificación para pruebas.
            // NO acreditar tokens permanentes aquí.
            onRewardEarned()
        }
    }
}

