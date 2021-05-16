package de.koch.soundcontrol.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST

interface NADControlApi {


    @GET("/power")
    fun getPower(): Call<NADResponse>

    @POST("/power/on")
    fun powerOn(): Call<NADResponse>

    @POST("/power/off")
    fun powerOff(): Call<NADResponse>

    @GET("/output/speaker/a")
    fun getStatusSpeakerA(): Call<NADResponse>

    @GET("/output/speaker/b")
    fun getStatusSpeakerB(): Call<NADResponse>

    @POST("/output/speaker/a/on")
    fun enableSpeakerA(): Call<NADResponse>

    @POST("/output/speaker/a/off")
    fun disableSpeakerA(): Call<NADResponse>

    @POST("/output/speaker/b/on")
    fun enableSpeakerB(): Call<NADResponse>

    @POST("/output/speaker/b/off")
    fun disableSpeakerB(): Call<NADResponse>

    @GET("/volume/mute")
    fun getMute(): Call<NADResponse>

    @POST("/volume/mute/on")
    fun muteOn(): Call<NADResponse>

    @POST("/volume/mute/off")
    fun muteOff(): Call<NADResponse>

    @POST("/volume/up")
    fun volumeUp(): Call<NADResponse>

    @POST("/volume/down")
    fun volumeDown(): Call<NADResponse>

    @GET("/source")
    fun getSource(): Call<NADResponse>

    @POST("/source/aux")
    fun setSourceAux(): Call<NADResponse>

    @POST("/source/cd")
    fun setSourceCd(): Call<NADResponse>

    @POST("/source/mp")
    fun setSourceMp(): Call<NADResponse>

    @POST("/source/phono")
    fun setSourcePhono(): Call<NADResponse>

    @POST("/source/tape2")
    fun setSourceTape2(): Call<NADResponse>

    @POST("/source/tuner")
    fun setSourceTuner(): Call<NADResponse>
}