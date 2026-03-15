package com.palmastro.contracts

import kotlinx.serialization.Serializable

@Serializable
enum class Hand { LEFT, RIGHT }

@Serializable
enum class Angle { FRONT, LEFT_TILT, RIGHT_TILT, NEAR, FAR, UP_TILT, DOWN_TILT }

@Serializable
enum class CalcLevel { L1, L2 }

@Serializable
enum class Tone { SCIENTIFIC, HEALING, ROAST_SAFE }

@Serializable
enum class ComparabilityBucket { HIGH, MED, LOW }
