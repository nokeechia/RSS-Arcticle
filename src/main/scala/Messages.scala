package com.dbaktor.messages

case class ParseHtmlArticle(url: String, htmlString: String)
case class HttpResponse(body: String)
case class ArticleBody(url: String, body: String)
