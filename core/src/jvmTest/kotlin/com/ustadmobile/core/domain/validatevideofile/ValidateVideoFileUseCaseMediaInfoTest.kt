package com.ustadmobile.core.domain.validatevideofile

import com.ustadmobile.door.ext.toDoorUri
import com.ustadmobile.util.test.ext.newFileFromResource
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidateVideoFileUseCaseMediaInfoTest {

    @JvmField
    @Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var validatorUseCase: ValidateVideoFileUseCase

    @BeforeTest
    fun setup() {
        validatorUseCase = ValidateVideoFileUseCaseMediaInfo(
            mediaInfoPath = "/usr/bin/mediainfo",
            workingDir = temporaryFolder.newFolder(),
            json = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
        )
    }

    @Test
    fun givenValidVideo_whenInvoked_willReturnTrue() {
        val videoFile = temporaryFolder.newFileFromResource(this::class.java,
            "/com/ustadmobile/core/container/BigBuckBunny.mp4")


        runBlocking {
            assertTrue(validatorUseCase(videoFile.toDoorUri()))
        }
    }

    @Test
    fun givenFileIsNotVideo_whenInvoked_willReturnFalse() {
        val otherFile = temporaryFolder.newFileFromResource(this::class.java,
            "/com/ustadmobile/core/container/testfile1.png")
        runBlocking {
            assertFalse(validatorUseCase(otherFile.toDoorUri()))
        }
    }

    @Test
    fun givenFileDoesNotExist_whenInvoked_willReturnFalse() {
        val fileNotExisting = File("idontexist")
        runBlocking {
            assertFalse(validatorUseCase(fileNotExisting.toDoorUri()))
        }
    }

}