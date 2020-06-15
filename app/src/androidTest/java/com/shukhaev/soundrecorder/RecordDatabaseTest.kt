package com.shukhaev.soundrecorder

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shukhaev.soundrecorder.dataBase.RecordDataBaseDao
import com.shukhaev.soundrecorder.dataBase.RecordDatabase
import com.shukhaev.soundrecorder.dataBase.RecordingItem
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RecordDatabaseTest{
    private  lateinit var recordDataBaseDao: RecordDataBaseDao
    private lateinit var database: RecordDatabase

    @Before
    fun createDb(){
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context,RecordDatabase::class.java)
            .allowMainThreadQueries().build()

        recordDataBaseDao = database.recordDataBaseDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb(){
        database.close()
    }

    @Test
    @Throws(IOException::class)
    fun testDatabase(){
        recordDataBaseDao.insert(RecordingItem())
        val getCount = recordDataBaseDao.getCount()
        assertEquals(getCount,1)
    }

}