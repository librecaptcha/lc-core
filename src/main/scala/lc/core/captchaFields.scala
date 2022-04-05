package lc.core

object ParametersEnum extends Enumeration {
  type Parameter = Value

  val SUPPORTEDLEVEL: Value = Value("supportedLevels")
  val SUPPORTEDMEDIA: Value = Value("supportedMedia")
  val SUPPORTEDINPUTTYPE: Value = Value("supportedInputType")

  val ALLOWEDLEVELS: Value = Value("allowedLevels")
  val ALLOWEDMEDIA: Value = Value("allowedMedia")
  val ALLOWEDINPUTTYPE: Value = Value("allowedInputType")
  val ALLOWEDSIZES: Value = Value("allowedSizes")
}

object AttributesEnum extends Enumeration {
  type Attribute = Value

  val NAME: Value = Value("name")
  val RANDOM_SEED: Value = Value("randomSeed")
  val PORT: Value = Value("port")
  val ADDRESS: Value = Value("address")
  val CAPTCHA_EXPIRY_TIME_LIMIT: Value = Value("captchaExpiryTimeLimit")
  val BUFFER_COUNT: Value = Value("throttle")
  val THREAD_DELAY: Value = Value("threadDelay")
  val PLAYGROUND_ENABLED: Value = Value("playgroundEnabled")
  val CORS_HEADER: Value = Value("corsHeader")
  val CONFIG: Value = Value("config")
  val MAX_ATTEMPTS: Value = Value("maxAttempts")
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
  val IMG_MISSING: Value = Value("Image missing")
  val IMG_NOT_FOUND: Value = Value("Image not found")
  val NO_CAPTCHA: Value = Value("No captcha for the provided parameters. Change config options.")
  val BAD_METHOD: Value = Value("Bad request method")
}
