/**
 *
 * Device
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 16/01/15.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Ledger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package co.ledger.wallet.models

import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import java.util.Date
import javax.crypto.spec.SecretKeySpec
import co.ledger.wallet.crypto.SecretKey
import co.ledger.wallet.api.m2fa.GcmAPI
import co.ledger.wallet.security.Keystore
import co.ledger.wallet.utils.logs.Logger
import co.ledger.wallet.utils.{Benchmark, GooglePlayServiceHelper}
import org.json.JSONObject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._

import android.content.Context
import co.ledger.wallet.base.model.{Collection, BaseModel}

import scala.concurrent.Future
import scala.util.{Success, Try}

class PairedDongle(_id: String = null, _name: String = null, _date: Date = null) extends BaseModel {

  val id = string("id").set(_id)
  val name = string("name").set(_name)
  val createdAt = date("created_at").set(_date)

  def pairingKey(implicit context: Context, keystore: Keystore): Future[SecretKey] = PairedDongle.retrievePairingKey(keystore, id.get)

  def delete()(implicit context: Context, keystore: Keystore): Unit = {
    PairedDongle.deletePairingKey(context, keystore, id.get)
    GcmAPI.defaultInstance.removeDongleToken(this)
    PairedDongle.delete(this)
  }

  def this() = {
    this(null, null, null)
  }

}

object PairedDongle extends Collection[PairedDongle] {

  def get(id: String)(implicit context: Context): Option[PairedDongle] = {
    val serializedDongle = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(id, null)
    if (serializedDongle == null)
      return None
    val dongle = Try(inflate(new JSONObject(serializedDongle)))
    dongle getOrElse None
  }

  def all(implicit context: Context): Array[PairedDongle] = {
    val serializedDongles = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getAll
    var dongles = Array[PairedDongle]()
    serializedDongles foreach {case (key, value) =>
      value match {
        case json: String =>
          val dongle = Try(inflate(new JSONObject(json)))
          if (dongle.isSuccess && dongle.get.isDefined)
            dongles = dongles :+ dongle.get.get
        case _ =>
      }
    }
    dongles
  }

  def create(keystore: Keystore, id: String, name: String, pairingKey: Array[Byte])(implicit context: Context): PairedDongle = {
    implicit val LogTag = "PairedDongle Creation"
    val dongle = new PairedDongle(id, name, new Date())
    context
      .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
      .edit()
      .putString(id, dongle.toJson.toString)
      .commit()
    storePairingKey(context, keystore, id, pairingKey)
    GooglePlayServiceHelper.getGcmRegistrationId onComplete {
      case Success(regId) => GcmAPI.defaultInstance.updateDonglesToken(regId)
      case _ =>
    }
    dongle
  }

  def delete(dongle: PairedDongle)(implicit context: Context): Unit = {
    context
      .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
      .edit()
      .remove(dongle.id.get)
      .commit()
  }

  def retrievePairingKey(keystore: Keystore, id: String)(implicit context: Context): Future[SecretKey] = SecretKey.get(context, keystore, id)

  def storePairingKey(context: Context, keystore: Keystore, id: String, pairingKey: Array[Byte]): Unit = {
    Logger.d("CREATING SECRET KEY")
    Keystore.defaultInstance(context).safe(context) {
      Logger.d("CREATE SECRET KEY")
      SecretKey.create(context, keystore, id, pairingKey)
    }
  }

  def deletePairingKey(context: Context, keystore: Keystore, id: String): Unit = SecretKey.delete(context, keystore, id)

  val PreferencesName = "PairedDonglePreferences"

}