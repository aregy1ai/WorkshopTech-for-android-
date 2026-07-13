package com.workshoptech

import android.content.Context
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.repository.WorkshopRepository

/**
 * Manual dependency injection container.
 * Holds singletons and provides them to ViewModelFactory.
 * Replace with Hilt for large-scale apps.
 */
class AppContainer(context: Context) {

    val database: AppDatabase = AppDatabase.getInstance(context)

    val repository: WorkshopRepository = WorkshopRepository(
        caseDao           = database.caseDao(),
        customerDao       = database.customerDao(),
        photoDao          = database.photoDao(),
        inspectionDao     = database.inspectionDao(),
        workflowTaskDao   = database.workflowTaskDao(),
        technicianDao     = database.technicianDao(),
        inventoryDao      = database.inventoryDao(),
        damageFindingDao  = database.damageFindingDao(),
        analysisResultDao = database.analysisResultDao(),
        videoDao          = database.videoDao(),
        motionDataDao     = database.motionDataDao()
    )
}
