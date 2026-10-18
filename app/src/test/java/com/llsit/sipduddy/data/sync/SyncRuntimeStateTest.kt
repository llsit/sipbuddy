package com.llsit.sipduddy.data.sync
import org.junit.Test
import org.junit.Assert.*
class SyncRuntimeStateTest {
    @Test fun overlappingWorkersKeepStatusRunningUntilBothFinish() {
        val runtime=SyncRuntimeState()
        runtime.start("A");runtime.start("A");runtime.start("B")
        runtime.finish("A")
        assertEquals(1,runtime.running.value["A"])
        runtime.finish("A")
        assertFalse("A" in runtime.running.value);assertTrue("B" in runtime.running.value)
    }
}
