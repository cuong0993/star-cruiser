package de.bissell.starcruiser

import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.LinkHeader
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.css.*
import kotlinx.css.BorderStyle.solid
import kotlinx.css.Color.Companion.black
import kotlinx.css.Color.Companion.darkGrey
import kotlinx.css.Color.Companion.dimGrey
import kotlinx.css.Color.Companion.lightGrey
import kotlinx.css.Color.Companion.white
import kotlinx.css.Cursor.pointer
import kotlinx.css.Display.block
import kotlinx.css.Display.grid
import kotlinx.css.FontWeight.Companion.bold
import kotlinx.css.Position.fixed
import kotlinx.css.TextAlign.center
import kotlinx.html.*

fun Routing.webUi() {
    static("/js") {
        resources("js")
    }

    get("/status") {
        call.respondText("alive")
    }

    get("/css/client.css") {
        call.respondText(contentType = ContentType.Text.CSS) {
            CSSBuilder(indent = "\t").apply {
                body {
                    margin = 0.px.value
                    padding = 0.px.value
                    backgroundColor = Color("#222")
                    color = white
                    fontFamily = "\"Courier New\", Courier, monospace"
                }
                canvas {
                    position = fixed
                    top = 0.px
                    left = 0.px
                    width = 100.pct
                    height = 100.pct
                }
                ".conn" {
                    position = fixed
                    top = 0.px
                    right = 0.px
                    zIndex = 10
                    padding = 20.px.value
                }
                ".topInfo" {
                    position = fixed
                    top = 0.px
                    zIndex = 10
                    padding(20.px, 0.px)
                }
                ".info" {
                    position = fixed
                    bottom = 0.px
                    zIndex = 10
                    padding = 20.px.value
                }
                ".playerShips" {
                    display = grid
                    gap = Gap(10.px.value)
                    marginTop = 20.px
                }
                "button" {
                    display = block
                    color = lightGrey
                    backgroundColor = black
                    textAlign = center
                    paddingTop = 10.px
                    paddingRight = 16.px
                    paddingBottom = 12.px
                    paddingLeft = 10.px
                    borderWidth = 3.px
                    borderStyle = solid
                    borderColor = darkGrey
                    borderTopLeftRadius = 0.px
                    borderTopRightRadius = 21.px
                    borderBottomRightRadius = 21.px
                    borderBottomLeftRadius = 0.px
                    cursor = pointer
                    fontWeight = bold
                    fontSize = 16.px
                }
                "button:hover" {
                    color = black
                    backgroundColor = darkGrey
                }
                "button:active" {
                    color = black
                    backgroundColor = dimGrey
                    borderColor = dimGrey
                }
            }.toString()
        }
    }

    get("/") {
        call.respondHtml {
            head {
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1"
                }
                script {
                    type = ScriptType.textJavaScript
                    src = "/js/client.js"
                }
                link {
                    rel = LinkHeader.Rel.Stylesheet
                    href = "/css/client.css"
                }
            }
            body {
                joinUi()
                helmUi()
                navigationUi()
            }
        }
    }

    authenticate("myBasicAuth") {
        get("/protected/route/basic") {
            val principal = call.principal<UserIdPrincipal>()!!
            call.respondText("Hello ${principal.name}")
        }
    }
}

private fun BODY.joinUi() {
    div {
        id = "join"
        div(classes = "conn") {
            +"disconnected"
        }
        div(classes = "topInfo") {
            button(classes = "spawn") {
                +"+ Spawn ship"
            }
            div(classes = "playerShips") {}
        }
    }
}

private fun BODY.helmUi() {
    div {
        id = "helm"
        div(classes = "conn") {
            +"disconnected"
        }
        div(classes = "topInfo") {
            button(classes = "exit") {
                +"< Exit ship"
            }
        }
        canvas {}
        div(classes = "info") {
            p {
                +"Heading: "
                span {
                    id = "heading"
                    +"unknown"
                }
            }
            p {
                +"Velocity: "
                span {
                    id = "velocity"
                    +"unknown"
                }
            }
        }
    }
}

private fun BODY.navigationUi() {
    div {
        id = "navigation"
        div(classes = "conn") {
            +"disconnected"
        }
        canvas {}
    }
}
