package com.ustadmobile.lib.rest.ext

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.PersonAuthDao
import com.ustadmobile.core.util.UMIOUtils
import com.ustadmobile.core.util.ext.insertPersonAndGroup
import com.ustadmobile.lib.contentscrapers.ScraperConstants
import com.ustadmobile.lib.db.entities.Language
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.PersonAuth
import com.ustadmobile.lib.db.entities.WorkSpace
import com.ustadmobile.lib.util.encryptPassword
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.ArrayList

fun UmAppDatabase.ktorInitDbWithRepo(repo: UmAppDatabase, passwordFilePath: String) {
    if(workSpaceDao.getWorkSpace() == null) {
        repo.workSpaceDao.insert(WorkSpace().apply {
            uid = 1L
            name = "UstadmobileWorkspace"
            guestLogin = false
            registrationAllowed = false
        })
    }

    if(languageDao.totalLanguageCount() < 1) {
        //insert all languages
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val langListStr = BufferedReader(InputStreamReader(
                this::class.java.getResourceAsStream("/languagedata/iso_639_3.json"))).use {
                    it.readText()
        }

        val langList = gson.fromJson<ArrayList<Language>>(langListStr,
                object : TypeToken<List<Language>>() {}.type)

        repo.languageDao.insertList(langList)
    }


    val adminuser = personDao.findByUsername("admin")

    if (adminuser == null) {
        val adminPerson = Person("admin", "Admin", "User")
        adminPerson.admin = true
        adminPerson.personUid = runBlocking { repo.insertPersonAndGroup(adminPerson).personUid }

        //Remove lower case l, upper case I, and the number 1
        val adminPass = RandomStringUtils.random(10, "abcdefghijkmnpqrstuvxwyzABCDEFGHJKLMNPQRSTUVWXYZ23456789")

        personAuthDao.insert(PersonAuth(adminPerson.personUid,
                PersonAuthDao.ENCRYPTED_PASS_PREFIX + encryptPassword(adminPass)))


        val adminPassFile = File(passwordFilePath, "admin.txt")
        if (!adminPassFile.parentFile.isDirectory) {
            adminPassFile.parentFile.mkdirs()
        }

        adminPassFile.writeText(adminPass)
        println("Saved admin password to ${adminPassFile.absolutePath}")
    }

    runBlocking {
        repo.roleDao.insertDefaultRolesIfRequired()
    }
}