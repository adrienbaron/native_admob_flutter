package com.bruno.native_admob_flutter.banner

import android.content.Context
import android.view.View
import com.bruno.native_admob_flutter.RequestFactory
import com.bruno.native_admob_flutter.encodeError
import com.google.android.gms.ads.*
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class BannerAdViewFactory : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context?, id: Int, args: Any?): PlatformView {
        val creationParams = args as Map<String?, Any?>?
        return BannerAdView(context, creationParams)
    }
}

class BannerAdView(context: Context?, data: Map<String?, Any?>?) : PlatformView {

    private var controller: BannerAdController = BannerAdControllerManager.getController(data!!["controllerId"] as String)!!
    private var adSize: AdSize
    private var nonPersonalizedAds: Boolean
    private var keywords: List<String>

    private fun getAdSize(context: Context, width: Float): AdSize {
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width.toInt())
    }

    init {
        adSize = getAdSize(controller.context, (data!!["size_width"] as Double).toFloat())
        nonPersonalizedAds = data["nonPersonalizedAds"] as Boolean
        keywords = data["keywords"] as List<String>
        generateAdView(context, data)
        controller.loadRequested = { load(it) }
        load(null)
    }

    private fun load(result: MethodChannel.Result?) {
        controller.adView!!.loadAd(RequestFactory.createAdRequest(nonPersonalizedAds, keywords))
        controller.adView!!.adListener = object : AdListener() {
            override fun onAdImpression() {
                super.onAdImpression()
                controller.channel.invokeMethod("onAdImpression", null)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                controller.channel.invokeMethod("onAdClicked", null)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                controller.channel.invokeMethod("onAdFailedToLoad", encodeError(error))
                result?.success(false)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                controller.channel.invokeMethod("onAdLoaded", controller.adView!!.adSize?.height)
                result?.success(true)
            }
        }
    }

    private fun generateAdView(context: Context?, data: Map<String?, Any?>?) {
        controller.adView = context?.let { AdView(it) }
        val width: Int = (data!!["size_width"] as Double).toInt()
        val height: Int = (data["size_height"] as Double).toInt()
        if (height != -1) controller.adView?.setAdSize(AdSize(width, height))
        else controller.adView?.setAdSize(adSize)
        controller.adView!!.adUnitId = data["unitId"] as String
    }

    override fun getView(): View {
        return controller.adView!!
    }

    override fun dispose() {}
}