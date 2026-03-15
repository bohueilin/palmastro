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
    val reminders: String = "monthly",
    val rawMediaRetention: Boolean = true,
    val calcLevel: String = "L1",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
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
