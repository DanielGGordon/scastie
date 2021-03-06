package com.olegych.scastie.client

import com.olegych.scastie.api._

import play.api.libs.json._

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.XMLHttpRequest

import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.util.{Try, Success, Failure}

object ApiClient extends RestApi {

  def tryParse[T: Reads](request: XMLHttpRequest): Option[T] = {
    val rawJson = request.responseText
    if (rawJson.nonEmpty) {
      Try(Json.parse(rawJson)) match {
        case Success(json) => {
          Json.fromJson[T](json).asOpt
        }
        case Failure(e) => {
          e.printStackTrace()
          None
        }
      }
    } else {
      None
    }
  }

  def get[T: Reads](url: String): Future[Option[T]] = {
    Ajax
      .get(
        url = "/api" + url,
        headers = Map("Accept" -> "application/json")
      )
      .map(ret => tryParse[T](ret))
  }

  class Post[O: Reads]() {
    def using[I: Writes](url: String, data: I): Future[Option[O]] = {
      Ajax
        .post(
          url = "/api" + url,
          data = Json.prettyPrint(Json.toJson(data)),
          headers = Map(
            "Content-Type" -> "application/json",
            "Accept" -> "application/json"
          ),
        )
        .map(ret => tryParse[O](ret))
    }
  }

  def post[O: Reads](): Post[O] = new Post[O]

  def run(inputs: Inputs): Future[SnippetId] =
    post[SnippetId].using("/run", inputs).map(_.get)

  def format(request: FormatRequest): Future[FormatResponse] =
    post[FormatResponse].using("/format", request).map(_.get)

  def autocomplete(
      request: AutoCompletionRequest
  ): Future[Option[AutoCompletionResponse]] =
    post[AutoCompletionResponse]
      .using("/autocomplete", request)

  def typeAt(request: TypeAtPointRequest): Future[Option[TypeAtPointResponse]] =
    post[TypeAtPointResponse].using("/typeAt", request)

  def updateEnsimeConfig(
      request: UpdateEnsimeConfigRequest
  ): Future[Option[EnsimeConfigUpdated]] =
    post[EnsimeConfigUpdated].using("/updateEnsimeConfig", request)

  def save(inputs: Inputs): Future[SnippetId] =
    post[SnippetId].using("/save", inputs).map(_.get)

  def amend(editInputs: EditInputs): Future[Boolean] =
    post[Boolean].using("/amend", editInputs).map(_.getOrElse(false))

  def update(editInputs: EditInputs): Future[Option[SnippetId]] =
    post[SnippetId].using("/update", editInputs)

  def fork(editInputs: EditInputs): Future[Option[SnippetId]] =
    post[SnippetId].using("/fork", editInputs)

  def delete(snippetId: SnippetId): Future[Boolean] =
    post[Boolean].using("/delete", snippetId).map(_.getOrElse(false))

  def fetch(snippetId: SnippetId): Future[Option[FetchResult]] =
    get[FetchResult]("/snippets/" + snippetId.url)

  def fetchOld(id: Int): Future[Option[FetchResult]] =
    get[FetchResult](s"/old-snippets/$id")

  def fetchUser(): Future[Option[User]] =
    get[User]("/user/settings")

  def fetchUserSnippets(): Future[List[SnippetSummary]] =
    get[List[SnippetSummary]]("/user/snippets").map(_.getOrElse(Nil))
}
