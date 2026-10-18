package com.llsit.sipduddy.data.mapper
import com.llsit.sipduddy.domain.model.SyncRecord
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Test
import org.junit.Assert.*
class RemoteDrinkMapperTest {
    @Test fun unknownLegacyDrinkDoesNotPoisonUploadQueueAndTimestampStaysExact() {
        val record=SyncRecord("owner","entry","FUTURE_TYPE",250,1771234567890L,false,0,"operation")
        val json=RemoteDrinkMapper().encode(record)
        assertEquals("OTHER",json.getValue("p_drink").jsonPrimitive.content)
        assertEquals(record.timestamp,json.getValue("p_consumed").jsonPrimitive.long)
    }
}
