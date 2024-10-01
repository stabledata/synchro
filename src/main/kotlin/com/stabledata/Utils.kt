package com.stabledata

import com.fasterxml.uuid.Generators
import java.util.*

fun uuid(): UUID = Generators.timeBasedEpochGenerator().generate()
fun uuidString() = uuid().toString()

