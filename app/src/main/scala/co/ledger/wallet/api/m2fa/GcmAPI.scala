/**
 *
 * GcmAPI
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 11/02/15.
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
package co.ledger.wallet.api.m2fa


import android.content.Context
import co.ledger.wallet.models.PairedDongle
import co.ledger.wallet.net.HttpClient
import co.ledger.wallet.utils.GooglePlayServiceHelper.RegistrationId
import co.ledger.wallet.utils.Preferenceable
import co.ledger.wallet.utils.logs.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import co.ledger.wallet.utils.JsonUtils._
import com.netaporter.uri.dsl._

import scala.util.{Failure, Success}

class GcmAPI(c: Context, client: HttpClient = HttpClient.defaultInstance) extends Preferenceable {
  override def PreferencesName = "GcmAPI"
  implicit val context = c

  def updateDongleToken(dongle: PairedDongle, regId: RegistrationId): Unit = {
    if (preferences.getString(dongle.id.get, null) != regId.value) {
      val pairingId = dongle.id.get
      client.post(s"/2fa/pairings/$pairingId/push_token")
        .body(Map("pairing_id" -> dongle.id.get, "push_token" -> regId.value))
        .noResponseBody.onComplete {
          case Success(_) => {
            Logger.d("POST OK")
            edit()
              .putString(dongle.id.get, regId.value)
              .commit()
          }
          case Failure(ex) =>
            Logger.d("POST KO")
        }
    }
  }

  def removeDongleToken(dongle: PairedDongle): Unit = client.delete("/2fa/pairings" / dongle.id.get / "push_token").noResponseBody
  def updateDonglesToken(regId: RegistrationId): Unit = PairedDongle.all.foreach(updateDongleToken(_, regId))
}

object GcmAPI {

  private[this] var _instance: GcmAPI = _

  def defaultInstance(implicit context: Context): GcmAPI = {
    if (_instance == null)
      _instance = new GcmAPI(context)
    _instance
  }

}