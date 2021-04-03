package lc.core

object ParametersEnum extends Enumeration {
  type Parameter = Value

  val SUPPORTEDLEVEL: Value = Value("supportedLevels")
  val SUPPORTEDMEDIA: Value = Value("supportedMedia")
  val SUPPORTEDINPUTTYPE: Value = Value("supportedInputType")

  val ALLOWEDLEVELS: Value = Value("allowedLevels")
  val ALLOWEDMEDIA: Value = Value("allowedMedia")
  val ALLOWEDINPUTTYPE: Value = Value("allowedInputType")

}

object AttributesEnum extends Enumeration {
  type Attribute = Value

  val NAME: Value = Value("name")
  val RANDOM_SEED: Value = Value("randomSeed")
  val PORT: Value = Value("port")
  val CAPTCHA_EXPIRY_TIME_LIMIT: Value = Value("captchaExpiryTimeLimit")
  val THROTTLE: Value = Value("throttle")
  val THREAD_DELAY: Value = Value("threadDelay")
  val CONFIG: Value = Value("config")
}

object ResultEnum extends Enumeration {
  type Result = Value

  val TRUE: Value = Value("True")
  val FALSE: Value = Value("False")
  val EXPIRED: Value = Value("Expired")
}

object ErrorMessageEnum extends Enumeration {
  type ErrorMessage = Value

  val SMW: Value = Value("Oops, something went worng!")
  val INVALID_PARAM: Value = Value("Parameters invalid or missing")
  val NO_CAPTCHA: Value = Value("No captcha for the provided parameters")
  val BAD_METHOD: Value = Value("Bad request method")
}
