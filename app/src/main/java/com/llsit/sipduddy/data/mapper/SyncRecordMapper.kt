package com.llsit.sipduddy.data.mapper
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import com.llsit.sipduddy.domain.model.SyncRecord
class SyncRecordMapper {
    fun toDomain(row: DrinkEntity) = SyncRecord(row.ownerId,row.remoteId,row.drink,row.amountMl,row.timestamp,
        row.deleted,row.serverVersion,row.operationId,row.localVersion,row.allowRestore)
    fun toEntity(row: SyncRecord, id: Long = 0, localVersion: Long = 0) = DrinkEntity(id=id,
        ownerId=row.ownerId,remoteId=row.remoteId,drink=row.drink,amountMl=row.amountMl,timestamp=row.timestamp,
        deleted=row.deleted,serverVersion=row.serverVersion,operationId=row.operationId,
        localVersion=localVersion,pending=false,allowRestore=false)
}
