package info.covid.state

import info.covid.customview.DataPoint
import info.covid.models.DistrictState
import info.covid.network.RetrofitClient

class StateWiseRepository {

    private val apiService = RetrofitClient.get().create(StateWiseAPIService::class.java)

    suspend fun <T> getDistrictInfo(
        state: String,
        success: (DistrictState?) -> T,
        error: (String) -> T
    ) {
        try {
            val resp = apiService.getStateDistrictWise()
            if (resp.isSuccessful) {
                val states = resp.body() ?: emptyList()
                val state = states.find { it.state == state }
                success(state)
            } else error("Oops, Something went wrong")
        } catch (e: Exception) {
            error("Oops, Something went wrong.")
        }
    }

    suspend fun <T> getStateDaily(
        state: String,
        success: (HashMap<String, ArrayList<DataPoint>>) -> T,
        error: (String) -> T
    ) {
        try {
            val resp = apiService.getStateDaily()
            if (resp.isSuccessful) {
                val states = resp.body()?.dailyStats ?: emptyList()
                val mapData = HashMap<String, ArrayList<DataPoint>>()

                states.groupBy { it.status }.forEach {
                    mapData[it.key ?: ""] = arrayListOf()
                    it.value.forEach { stateDailyItem ->
                        mapData[it.key]?.add(DataPoint(stateDailyItem.getCount(state).toFloat()))
                    }
                }


                mapData["Active"] = arrayListOf<DataPoint>().apply {
                    mapData["Confirmed"]?.forEachIndexed { index, dataPoint ->
                        val recovered = mapData["Recovered"]?.get(index)?.amount ?: 0f
                        val deceased = mapData["Deceased"]?.get(index)?.amount ?: 0f
                        add(DataPoint(dataPoint.amount.minus((recovered.plus(deceased)))))
                    }
                }

                success(mapData)
            } else error("Oops, Something went wrong")
        } catch (e: Exception) {
            error("Oops, Something went wrong.")
        }
    }
}