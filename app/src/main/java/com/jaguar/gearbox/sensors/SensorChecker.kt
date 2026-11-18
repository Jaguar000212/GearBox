package com.jaguar.gearbox.sensors

/** Fixed permission-free inventory, not a full device fingerprint or diagnostic certification. */
enum class CheckedSensor(val androidType:Int, val components:Int) {
    ACCELEROMETER(1,3), GYROSCOPE(4,3), MAGNETOMETER(2,3), LIGHT(5,1), PRESSURE(6,1),
    PROXIMITY(8,1), GRAVITY(9,3), LINEAR_ACCELERATION(10,3), ROTATION_VECTOR(11,3)
}
data class CheckedSensorMetadata(val available:Boolean, val maximumRange:Float?=null,val resolution:Float?=null)
enum class SensorCheckStatus { STOPPED, UNAVAILABLE, WAITING, READING, ERROR }
data class SensorCheckState(val status:SensorCheckStatus=SensorCheckStatus.STOPPED,val values:List<Float> = emptyList(),val accuracy:LightMeterAccuracy=LightMeterAccuracy.UNKNOWN)
fun checkedSensorValues(kind:CheckedSensor, values:List<Float>):List<Float>? {
    val count=if(kind==CheckedSensor.ROTATION_VECTOR && values.size>=4)4 else kind.components
    if(values.size<count)return null
    return values.take(count).takeIf { it.all(Float::isFinite) && (kind.components!=1 || it[0]>=0f) }
}
interface SensorCheckerSource {
    fun metadata(kind:CheckedSensor):CheckedSensorMetadata
    fun start(kind:CheckedSensor,reading:(List<Float>,Int)->Unit,accuracy:(Int)->Unit):Boolean
    fun stop()
}
class SensorCheckerController(private val source:SensorCheckerSource,private val publish:(SensorCheckState)->Unit) {
    private var epoch=0L
    private var active=false
    private var state=SensorCheckState()
    private fun update(next:SensorCheckState){state=next;publish(next)}
    fun start(kind:CheckedSensor){
        stop();active=true;val token=++epoch
        if(!source.metadata(kind).available){active=false;update(SensorCheckState(SensorCheckStatus.UNAVAILABLE));return}
        update(SensorCheckState(SensorCheckStatus.WAITING))
        val ok=try{source.start(kind,{values,accuracy->if(active && token==epoch){
            val valid=checkedSensorValues(kind,values)
            update(SensorCheckState(if(valid==null)SensorCheckStatus.WAITING else SensorCheckStatus.READING,valid?:emptyList(),lightMeterAccuracy(accuracy)))
        }},{accuracy->if(active && token==epoch)update(state.copy(accuracy=lightMeterAccuracy(accuracy)))})}catch(_:RuntimeException){false}
        if(!ok){stop();update(SensorCheckState(SensorCheckStatus.ERROR))}
    }
    fun stop(){++epoch;active=false;try{source.stop()}catch(_:RuntimeException){};update(SensorCheckState())}
}
