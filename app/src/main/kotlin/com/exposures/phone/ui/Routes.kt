package com.exposures.phone.ui

object Routes {
    const val HOME = "home"

    const val CAMERA_BODY_LIST = "cameraBodyList"
    const val CAMERA_BODY_EDIT = "cameraBodyEdit?id={id}"

    const val LENS_LIST = "lensList"
    const val LENS_EDIT = "lensEdit?id={id}"

    const val LIGHT_METER_LIST = "lightMeterList"
    const val LIGHT_METER_EDIT = "lightMeterEdit?id={id}"

    const val FILM_BACK_LIST = "filmBackList"
    const val FILM_BACK_EDIT = "filmBackEdit?id={id}"

    const val FILM_ROLL_LIST = "filmRollList"
    const val FILM_ROLL_EDIT = "filmRollEdit?id={id}"

    const val ARG_ID = "id"

    fun cameraBodyEdit(id: String? = null) = if (id == null) "cameraBodyEdit" else "cameraBodyEdit?id=$id"
    fun lensEdit(id: String? = null) = if (id == null) "lensEdit" else "lensEdit?id=$id"
    fun lightMeterEdit(id: String? = null) = if (id == null) "lightMeterEdit" else "lightMeterEdit?id=$id"
    fun filmBackEdit(id: String? = null) = if (id == null) "filmBackEdit" else "filmBackEdit?id=$id"
    fun filmRollEdit(id: String? = null) = if (id == null) "filmRollEdit" else "filmRollEdit?id=$id"
}
