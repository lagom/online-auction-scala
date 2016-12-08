package controllers

import java.text.NumberFormat
import java.util.Locale

import scala.collection.immutable

case class Currency private (name: String, step: Int, locale: Locale) {
  
  private val currency = java.util.Currency.getInstance(name)
  private val format = NumberFormat.getCurrencyInstance(locale)
  
  def format(value: Int): String = format.format(toDecimal(value))

  def getDisplayName: String = currency.getDisplayName

  private def toDecimal(value: Int): Double = if (currency.getDefaultFractionDigits > 0) {
    value.toDouble / Math.pow(10, currency.getDefaultFractionDigits)
  } else {
    value
  }

  def formatDecimal(value: Int): String = if (currency.getDefaultFractionDigits > 0) {
    String.format("%." + currency.getDefaultFractionDigits + "f", java.lang.Double.valueOf(toDecimal(value)))
  } else {
    Integer.toString(value)
  }

  def getDecimalStep: Double = toDecimal(step)

  def toPriceUnits(value: Double): Int = if (currency.getDefaultFractionDigits > 0) {
    (value * Math.pow(10, currency.getDefaultFractionDigits)).round.toInt
  } else {
    value.round.toInt
  }

  def isValidStep(value: Double): Boolean = {
    val price = toPriceUnits(value)
    price % step == 0
  }
}

object Currency {
  val USD = Currency("USD", 50, Locale.US)
  val EUR = Currency("EUR", 50, Locale.GERMANY)
  val GBP = Currency("GBP", 50, Locale.UK)
  val JPY = Currency("JPY", 50, Locale.JAPAN)
  val CNY = Currency("CNY", 100, Locale.CHINA)
  val CAD = Currency("CAD", 50, Locale.CANADA)
  val AUD = Currency("AUD", 50, Locale.forLanguageTag("en-AU"))

  val values = immutable.Seq(
    USD, EUR, GBP, JPY, CNY, CAD, AUD
  )

  private val map = values.map(c => c.name -> c).toMap

  def isDefined(name: String): Boolean = map.contains(name)
  def valueOf(name: String): Currency = map(name)
}
