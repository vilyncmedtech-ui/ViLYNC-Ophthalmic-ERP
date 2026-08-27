package com.vilync.ophthalmicerp.feature.login.model

/**
 * Structured failures for the application bootstrap pipeline.
 * Inherits from RuntimeException to support throwing in the facts provider.
 */
sealed class StartupFailure(
    val code: String, 
    override val message: String, 
    override val cause: Throwable? = null
) : RuntimeException(message, cause) {
    
    class DatabaseOpenFailure(exception: Throwable) : 
        StartupFailure("ERR_DB_OPEN", "Database could not be opened.", exception)
        
    class MigrationFailure(val diskVersion: Int, val expectedVersion: Int) : 
        StartupFailure("ERR_DB_MIGRATION", "Migration mismatch. On disk: $diskVersion, Expected: $expectedVersion")
        
    class SchemaMismatch(message: String) : 
        StartupFailure("ERR_DB_SCHEMA", "Database schema is invalid: $message")
        
    class MissingTable(val tableName: String) : 
        StartupFailure("ERR_DB_MISSING_TABLE", "Critical system table missing: $tableName")
        
    class DaoFailure(val daoName: String, exception: Throwable) : 
        StartupFailure("ERR_DAO_QUERY", "Failed to query system state via $daoName.", exception)
        
    class InfrastructureFailure(message: String) : 
        StartupFailure("ERR_INFRASTRUCTURE", "System infrastructure error: $message")
        
    class OverwriteProtectionError : 
        StartupFailure("ERR_OVERWRITE_PROTECTED", "Business data detected without administrator account. Setup blocked for safety.")
        
    class UnknownFailure(exception: Throwable) : 
        StartupFailure("ERR_UNKNOWN", "An unexpected bootstrap error occurred.", exception)
}
