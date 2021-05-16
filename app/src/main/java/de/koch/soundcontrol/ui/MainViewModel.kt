package de.koch.soundcontrol.ui

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.koch.soundcontrol.api.*
import de.koch.soundcontrol.api.APIClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {

    private lateinit var client: NADControlApi

    val error = MutableLiveData<String>()
    val power = MutableLiveData(false)
    val mute = MutableLiveData<Boolean>()
    val speakerA = MutableLiveData<Boolean>()
    val speakerB = MutableLiveData<Boolean>()
    val source = MutableLiveData<Source>()
    val vibrate = MutableLiveData<Boolean>()
    val loading = MutableLiveData<Boolean>()

    fun initialize() {
        loading.value = true
        getPowerStatus()
    }

    fun initNetwork(ip: String, port: String) {
        createRetrofitClient(ip, port)
    }

    fun powerOn() {
        callPost(client.powerOn(), ifSuccess = { power.value = true })
    }

    fun powerOff() {
        callPost(client.powerOff(), ifSuccess = {
            power.value = false
            mute.value = false
            speakerA.value = false
            speakerB.value = false
            source.value = null
        })
    }

    fun enableSpeakerA() {
        callPost(client.enableSpeakerA(), ifSuccess = { speakerA.value = true })
    }

    fun enableSpeakerB() {
        callPost(client.enableSpeakerB(), ifSuccess = { speakerB.value = true })
    }

    fun disableSpeakerA() {
        callPost(client.disableSpeakerA(), ifSuccess = { speakerA.value = false })
    }

    fun disableSpeakerB() {
        callPost(client.disableSpeakerB(), ifSuccess = { speakerB.value = false })
    }

    private fun muteOn() {
        callPost(client.muteOn(), ifSuccess = { mute.value = true })
    }

    private fun muteOff() {
        callPost(client.muteOff(), ifSuccess = { mute.value = false })
    }

    fun volumeUp() {
        callPost(client.volumeUp(), ifSuccess = {})
    }

    fun volumeDown() {
        callPost(client.volumeDown(), ifSuccess = {})
    }

    fun setSource(selectedSource: Source) {
        when (selectedSource) {
            Source.CD -> callPost(client.setSourceCd(), ifSuccess = { source.value = Source.CD })
            Source.AUX -> callPost(client.setSourceAux(), ifSuccess = { source.value = Source.AUX })
            Source.MP -> callPost(client.setSourceMp(), ifSuccess = { source.value = Source.MP })
            Source.PHONO -> callPost(client.setSourcePhono(), ifSuccess = { source.value = Source.PHONO })
            Source.TAPE2 -> callPost(client.setSourceTape2(), ifSuccess = { source.value = Source.TAPE2 })
            Source.TUNER -> callPost(client.setSourceTuner(), ifSuccess = { source.value = Source.TUNER })
        }
    }

    fun getSource() {
        callGet(client.getSource(), target = source)
    }

    private fun getPowerStatus() {
        callGet(client.getPower(), target = power)
    }

    fun getMuteStatus() {
        callGet(client.getMute(), target = mute)
    }

    fun getStatusSpeakerA() {
        callGet(client.getStatusSpeakerA(), target = speakerA)
    }

    fun getStatusSpeakerB() {
        callGet(client.getStatusSpeakerB(), target = speakerB)
    }

    fun toggleMute() {
        if (mute.value == true) {
            muteOff()
        } else {
            muteOn()
        }
    }

    fun createRetrofitClient(ip: String, port: String): Boolean {
        return if (ip.isNotEmpty() && port.isDigitsOnly() && port.toInt() >= 0 && port.toInt() < 65535) {
            val url = "http://$ip:$port/"
            client = APIClient(url).client!!.create(NADControlApi::class.java)
            true
        } else {
            error.value = "Invalid IP $ip or Port $port"
            false
        }
    }

    private fun callPost(function: Call<NADResponse>, ifSuccess: () -> Unit) {
        if (::client.isInitialized) {
            loading.value = true
            function.enqueue(object : Callback<NADResponse> {
                override fun onResponse(
                    call: Call<NADResponse>,
                    response: Response<NADResponse>
                ) {
                    if (response.isSuccessful) {
                        ifSuccess()
                    } else {
                        error.value = "${response.raw().request().url()} ${response.errorBody()?.string()}"
                    }
                    loading.value = false
                }

                override fun onFailure(call: Call<NADResponse>, t: Throwable) {
                    error.value = t.message.toString()
                    loading.value = false
                }
            })
        } else {
            error.value = "IP Config not set"
        }
    }

    private fun callGet(function: Call<NADResponse>, target: MutableLiveData<*>) {
        if (::client.isInitialized) {
            loading.value = true
            function.enqueue(object : Callback<NADResponse> {
                override fun onResponse(
                    call: Call<NADResponse>,
                    NADResponse: Response<NADResponse>
                ) {
                    if (NADResponse.isSuccessful) {
                        val message = NADResponse.body()?.message
                        if (message == "true" || message == "false") {
                            target.value = NADResponse.body()?.message.toBoolean()
                        } else {
                            when (NADResponse.body()?.message) {
                                "CD" -> source.value = Source.CD
                                "AUX" -> source.value = Source.AUX
                                "MP" -> source.value = Source.MP
                                "DISC/MDC" -> source.value = Source.PHONO
                                "TAPE2" -> source.value = Source.TAPE2
                                "TUNER" -> source.value = Source.TUNER
                            }
                        }
                    } else {
                        error.value = "${NADResponse.raw().request().url()} Error: ${NADResponse.errorBody()?.string()}"
                    }
                    loading.value = false
                }

                override fun onFailure(call: Call<NADResponse>, t: Throwable) {
                    error.value = t.message.toString()
                    loading.value = false
                }
            })
        } else {
            error.value = "IP Config not set"
        }
    }

    fun setVibration(vibration: Boolean) {
        vibrate.value = vibration
    }
}