package com.palmastro.data.entities

import androidx.room.*

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val dominantHand: String = "right",
    val birthdayEpochDay: Long,
    val hasBirthTime: Boolean = false,
    val birthTimeMinutes: Int? = null,
    val hasBirthPlace: Boolean = false,
    val birthPlaceLat: Double? = null,
    val birthPlaceLon: Double? = null,
    val birthPlaceName: String? = null,
    val tone: String = "scientific",
    // Reminders are opt-in at launch (PRD §23; execution spec).
    val reminders: String = "off",
    val rawMediaRetention: Boolean = true,
    val calcLevel: String = "L1",
    val createdAt: Long = System.currentTimeMillis(),
    val name: String? = null,
    val gender: String? = null,
    val relationshipStatus: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    // "system" = follow device locale; added in schema v3.
    @ColumnInfo(defaultValue = "system") val language: String = "system",
)

@Entity(tableName = "monthly_result")
data class MonthlyResultEntity(
    @PrimaryKey val id: String,
    val monthKey: String,
    val scanSessionId: String,
    val calcLevel: String,
    val confidenceLevel: String,
    val confidenceReasonsJson: String,
    val domainScoresJson: String,
    val subdimScoresJson: String,
    val grade: String,
    val semanticPayloadsJson: String,
    val palmFeatureSummaryJson: String,
    val astroSignalsJson: String,
    val explainabilityJson: String,
    val rulesetVersion: String,
    val contentVersion: String,
    val scanQualityScore: Int,
    val featureCoverage: Float,
    val scanImagePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "delta")
data class DeltaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currentMonthKey: String,
    val prevMonthKey: String,
    val domainDeltasJson: String,
    val subdimDeltasJson: String,
    val gradeShift: String?,
    val comparabilityScore: Int,
    val comparabilityBucket: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "entitlement")
data class EntitlementEntity(
    @PrimaryKey val productId: String,
    val isOwned: Boolean = false,
    val purchasedAt: Long? = null,
    val lastVerifiedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "install_id")
data class InstallIdEntity(
    @PrimaryKey val id: Int = 1,
    val installId: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "journal",
    indices = [Index(value = ["monthKey", "domain"], unique = true)]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val monthKey: String,
    val domain: String? = null,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
