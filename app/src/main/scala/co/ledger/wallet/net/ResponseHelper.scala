/**
 *
 * ResponseHelper
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 01/07/15.
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
package co.ledger.wallet.net

import java.io.{ByteArrayOutputStream, BufferedInputStream}

import co.ledger.wallet.utils.io.IOUtils
import co.ledger.wallet.utils.logs.Logger
import org.json.{JSONArray, JSONObject}
import co.ledger.wallet.net.HttpRequestExecutor.defaultExecutionContext
import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success}

object ResponseHelper {

  implicit class ResponseFuture(f: Future[HttpClient#Response]) {

    def json: Future[(JSONObject, HttpClient#Response)] = {
      f.string.map { case (body, response) =>
        Logger.d("Converting to json")
        (new JSONObject(body), response)
      }
    }

    def jsonArray: Future[(JSONArray, HttpClient#Response)] = {
      f.string.map { case (body, response) =>
        (new JSONArray(body), response)
      }
    }

    def string: Future[(String, HttpClient#Response)] = {
      f.map { response =>
        Logger.d("Converting to string")
        (Source.fromInputStream(response.body).mkString, response)
      }
    }

    def bytes: Future[(Array[Byte], HttpClient#Response)] = {
      f.map { response =>
        val input = new BufferedInputStream(response.body)
        val output = new ByteArrayOutputStream()
        IOUtils.copy(input, output)
        val result = output.toByteArray
        input.close()
        output.close()
        (result, response)
      }
    }

    def noResponseBody: Future[HttpClient#Response] = {
      f.andThen {
        case Success(response) =>
          response.body.close()
          response
        case Failure(cause) =>
          throw cause
      }
    }

  }

}
