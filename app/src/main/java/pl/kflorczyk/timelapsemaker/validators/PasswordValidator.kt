package pl.kflorczyk.timelapsemaker.validators

/**
 * Created by Kamil on 2017-12-08.
 */
class PasswordValidator {
    fun validate(password: String): Boolean = password.length >= 3
}