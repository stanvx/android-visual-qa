package com.androidvisualqa.app

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.Display
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * Minimal fake Android [Context] for unit testing.
 *
 * Implements every abstract method in API 36 (compileSdk 36).
 * The handful needed by [WorkManagerTestInitHelper] and [androidx.room.Room]
 * return real values; everything else is a stub.
 *
 * ponytail: stub class; add real implementations only when a test needs them.
 */
internal class FakeContext(private val rootDir: File) : Context() {

    private val _cacheDir = File(rootDir, "cache").also { it.mkdirs() }
    private val _filesDir = File(rootDir, "files").also { it.mkdirs() }
    private val _prefs = mutableMapOf<String, FakeSharedPreferences>()

    override fun getAssets(): AssetManager = no()
    override fun getResources(): Resources = no()
    override fun getPackageManager(): PackageManager = no()
    override fun getContentResolver(): ContentResolver = FakeContentResolverImpl(this)
    override fun getMainLooper(): Looper = Looper.getMainLooper()
    override fun getApplicationContext(): Context = this
    override fun getCacheDir(): File = _cacheDir
    override fun getFilesDir(): File = _filesDir
    override fun getDir(name: String, mode: Int): File = File(rootDir, name).also { it.mkdirs() }
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = _prefs.getOrPut(name) { FakeSharedPreferences() }
    override fun openFileInput(name: String): FileInputStream = FileInputStream(File(_filesDir, name))
    override fun openFileOutput(name: String, mode: Int): FileOutputStream = FileOutputStream(File(_filesDir, name))
    override fun fileList(): Array<String> = _filesDir.list() ?: emptyArray()
    override fun deleteFile(name: String): Boolean = File(_filesDir, name).delete()
    override fun getDatabasePath(name: String): File = File(rootDir, "databases/$name").also { it.parentFile?.mkdirs() }
    override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase = no()
    override fun openOrCreateDatabase(n: String, m: Int, f: SQLiteDatabase.CursorFactory?, e: DatabaseErrorHandler?): SQLiteDatabase = no()
    override fun deleteDatabase(name: String): Boolean = getDatabasePath(name).delete()
    override fun databaseList(): Array<String> = File(rootDir, "databases").let { if (it.isDirectory) it.list() ?: emptyArray() else emptyArray() }
    override fun getPackageName(): String = "com.androidvisualqa.app.test"
    override fun getPackageCodePath(): String = ""
    override fun getPackageResourcePath(): String = ""
    override fun getApplicationInfo(): ApplicationInfo = no()

    // Activity / broadcast
    override fun startActivity(intent: Intent) {}
    override fun startActivity(intent: Intent, options: Bundle?) {}
    override fun startActivities(intents: Array<out Intent>) {}
    override fun startActivities(intents: Array<out Intent>, options: Bundle?) {}
    override fun startIntentSender(intent: IntentSender, fillInIntent: Intent?, flags: Int, start: Int, requestCode: Int) {}
    override fun startIntentSender(intent: IntentSender, fillInIntent: Intent?, flags: Int, start: Int, requestCode: Int, options: Bundle?) {}
    override fun startService(service: Intent): ComponentName? = null
    override fun stopService(name: Intent): Boolean = false
    override fun startForegroundService(service: Intent): ComponentName? = null
    override fun startInstrumentation(n: ComponentName, p: String?, a: Bundle?): Boolean = false

    override fun sendBroadcast(intent: Intent) {}
    override fun sendBroadcast(intent: Intent, receiverPermission: String?) {}
    override fun sendOrderedBroadcast(intent: Intent, receiverPermission: String?) {}
    override fun sendOrderedBroadcast(intent: Intent, receiverPermission: String?, resultReceiver: BroadcastReceiver?, scheduler: Handler?, initialCode: Int, initialData: String?, initialExtras: Bundle?) {}
    override fun sendBroadcastAsUser(intent: Intent, user: UserHandle) {}
    override fun sendBroadcastAsUser(intent: Intent, user: UserHandle, receiverPermission: String?) {}
    override fun sendOrderedBroadcastAsUser(intent: Intent, user: UserHandle, receiverPermission: String?, resultReceiver: BroadcastReceiver?, scheduler: Handler?, initialCode: Int, initialData: String?, initialExtras: Bundle?) {}
    override fun sendStickyBroadcast(intent: Intent) {}
    override fun sendStickyBroadcastAsUser(intent: Intent, user: UserHandle) {}
    override fun sendStickyOrderedBroadcast(intent: Intent, resultReceiver: BroadcastReceiver?, scheduler: Handler?, initialCode: Int, initialData: String?, initialExtras: Bundle?) {}
    override fun sendStickyOrderedBroadcastAsUser(intent: Intent, user: UserHandle, resultReceiver: BroadcastReceiver?, scheduler: Handler?, initialCode: Int, initialData: String?, initialExtras: Bundle?) {}
    override fun removeStickyBroadcast(intent: Intent) {}
    override fun removeStickyBroadcastAsUser(intent: Intent, user: UserHandle) {}

    override fun registerReceiver(r: BroadcastReceiver?, f: IntentFilter?): Intent? = null
    override fun registerReceiver(r: BroadcastReceiver?, f: IntentFilter?, flags: Int): Intent? = null
    override fun registerReceiver(r: BroadcastReceiver?, f: IntentFilter?, bp: String?, s: Handler?): Intent? = null
    override fun registerReceiver(r: BroadcastReceiver?, f: IntentFilter?, bp: String?, s: Handler?, flags: Int): Intent? = null
    override fun unregisterReceiver(receiver: BroadcastReceiver?) {}

    override fun bindService(s: Intent, c: ServiceConnection, flags: Int): Boolean = false
    override fun unbindService(conn: ServiceConnection) {}

    // Permissions
    override fun checkSelfPermission(permission: String): Int = PackageManager.PERMISSION_GRANTED
    override fun checkCallingPermission(permission: String): Int = PackageManager.PERMISSION_GRANTED
    override fun checkCallingOrSelfPermission(permission: String): Int = PackageManager.PERMISSION_GRANTED
    override fun checkCallingUriPermission(uri: Uri?, modeFlags: Int): Int = PackageManager.PERMISSION_GRANTED
    override fun checkCallingOrSelfUriPermission(uri: Uri?, modeFlags: Int): Int = PackageManager.PERMISSION_GRANTED
    override fun checkPermission(permission: String, pid: Int, uid: Int): Int = PackageManager.PERMISSION_GRANTED
    override fun checkUriPermission(uri: Uri, pid: Int, uid: Int, modeFlags: Int): Int = PackageManager.PERMISSION_GRANTED
    override fun checkUriPermission(uri: Uri?, rp: String?, wp: String?, pid: Int, uid: Int, modeFlags: Int): Int = PackageManager.PERMISSION_GRANTED
    override fun enforcePermission(p: String, pid: Int, uid: Int, m: String?) {}
    override fun enforceCallingPermission(p: String, m: String?) {}
    override fun enforceCallingOrSelfPermission(p: String, m: String?) {}
    override fun enforceCallingUriPermission(uri: Uri?, modeFlags: Int, m: String?) {}
    override fun enforceCallingOrSelfUriPermission(uri: Uri?, modeFlags: Int, m: String?) {}
    override fun enforceUriPermission(uri: Uri?, pid: Int, uid: Int, modeFlags: Int, m: String?) {}
    override fun enforceUriPermission(uri: Uri?, rp: String?, wp: String?, pid: Int, uid: Int, modeFlags: Int, m: String?) {}
    override fun grantUriPermission(toPkg: String, uri: Uri, flags: Int) {}
    override fun revokeUriPermission(uri: Uri, modeFlags: Int) {}
    override fun revokeUriPermission(toPkg: String, uri: Uri, modeFlags: Int) {}

    // System
    override fun getSystemService(name: String): Any? = null
    override fun getSystemServiceName(serviceClass: Class<*>): String? = null

    // Context creation
    override fun createContextForSplit(splitName: String): Context = this
    override fun createConfigurationContext(config: Configuration): Context = this
    override fun createDeviceProtectedStorageContext(): Context = this
    override fun createDisplayContext(display: Display): Context = this
    override fun createPackageContext(packageName: String, flags: Int): Context = this
    override fun isDeviceProtectedStorage(): Boolean = false

    // Storage dirs
    override fun getObbDir(): File? = null
    override fun getObbDirs(): Array<File> = emptyArray()
    override fun getExternalCacheDir(): File? = _cacheDir
    override fun getExternalCacheDirs(): Array<File> = arrayOf(_cacheDir)
    override fun getExternalFilesDir(type: String?): File? = _filesDir
    override fun getExternalFilesDirs(type: String?): Array<File> = arrayOf(_filesDir)
    override fun getExternalMediaDirs(): Array<File> = emptyArray()
    override fun getNoBackupFilesDir(): File = File(rootDir, "no_backup").also { it.mkdirs() }
    override fun getCodeCacheDir(): File = File(rootDir, "code_cache").also { it.mkdirs() }
    override fun getDataDir(): File = rootDir
    override fun getFileStreamPath(name: String): File = File(_filesDir, name)

    // Wallpaper
    override fun getWallpaper(): Drawable? = null
    override fun peekWallpaper(): Drawable? = null
    override fun getWallpaperDesiredMinimumWidth(): Int = 0
    override fun getWallpaperDesiredMinimumHeight(): Int = 0
    override fun setWallpaper(bitmap: Bitmap) {}
    override fun setWallpaper(stream: InputStream) {}
    override fun clearWallpaper() {}

    // Misc
    override fun getClassLoader(): ClassLoader = this::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
    override fun getTheme(): Resources.Theme = no()
    override fun setTheme(resId: Int) {}
    override fun getMainExecutor(): java.util.concurrent.Executor = Executors.newSingleThreadExecutor()
    override fun moveDatabaseFrom(src: Context, name: String): Boolean = false
    override fun moveSharedPreferencesFrom(src: Context, name: String): Boolean = false
    override fun deleteSharedPreferences(name: String): Boolean = _prefs.remove(name) != null

    private fun no(): Nothing = throw NotImplementedError()
}

internal class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()
    override fun getAll(): MutableMap<String, *> = map.toMutableMap()
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValue: MutableSet<String>?): MutableSet<String>? = map[key] as? MutableSet<String> ?: defValue
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = key in map
    override fun edit(): SharedPreferences.Editor = FakeEditor()
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {}

    private inner class FakeEditor : SharedPreferences.Editor {
        private val p = mutableMapOf<String, Any?>()
        private val r = mutableSetOf<String>()
        override fun putString(k: String, v: String?): SharedPreferences.Editor { p[k] = v; return this }
        override fun putStringSet(k: String, v: MutableSet<String>?): SharedPreferences.Editor { p[k] = v; return this }
        override fun putInt(k: String, v: Int): SharedPreferences.Editor { p[k] = v; return this }
        override fun putLong(k: String, v: Long): SharedPreferences.Editor { p[k] = v; return this }
        override fun putFloat(k: String, v: Float): SharedPreferences.Editor { p[k] = v; return this }
        override fun putBoolean(k: String, v: Boolean): SharedPreferences.Editor { p[k] = v; return this }
        override fun remove(k: String): SharedPreferences.Editor { r.add(k); return this }
        override fun clear(): SharedPreferences.Editor { p.clear(); r.clear(); map.clear(); return this }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() { for (k in r) map.remove(k); map.putAll(p); r.clear(); p.clear() }
    }
}

/**
 * Stub [ContentResolver] that returns null for all queries.
 * Needed to prevent [NotImplementedError] when ExportBridge
 * tries to use MediaStore on API 29+.
 *
 * ContentResolver has no abstract methods in API 36 (all are
 * concrete/final). Default implementations return null/false,
 * so no overrides are needed.
 */
internal class FakeContentResolverImpl(context: Context?) : ContentResolver(context)
