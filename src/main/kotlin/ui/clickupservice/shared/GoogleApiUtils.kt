package ui.clickupservice.shared

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import org.springframework.stereotype.Component
import ui.clickupservice.shared.config.ConfigProperties
import java.io.File
import java.io.StringReader

@Component
class GoogleApiUtils(val configProperties: ConfigProperties) {

    companion object {
        private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
        private const val TOKENS_DIRECTORY_PATH: String = "./config/tokens"

    }

    fun getSheetService(): Sheets {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

        return Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
            .setApplicationName("Sheet API")
            .build()
    }

    fun getDriveService(): Drive {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

        return Drive.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
            .setApplicationName("Drive API")
            .build()
    }


    private fun getCredentials(transport: NetHttpTransport): Credential {

        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, StringReader(configProperties.googleCredentials))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(
            transport,
            JSON_FACTORY,
            clientSecrets,
            listOf(DriveScopes.DRIVE_METADATA_READONLY, DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS)
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }


}