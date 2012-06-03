(ns shop.handler
  (:require [shop.fs :as fs])
  (:import [org.jboss.netty.channel SimpleChannelUpstreamHandler]
           [org.jboss.netty.handler.codec.http
            HttpMethod
            HttpHeaders
            HttpHeaders$Names
            HttpResponseStatus
            HttpVersion
            HttpRequest
            DefaultHttpResponse]
           [org.jboss.netty.handler.codec.http.websocketx
            WebSocketFrame
            CloseWebSocketFrame
            PingWebSocketFrame
            PongWebSocketFrame
            TextWebSocketFrame
            WebSocketServerHandshaker
            WebSocketServerHandshakerFactory]))


(defn handler []
  (proxy [SimpleChannelUpstreamHandler] []
    (messageReceived [ctx e]
      (let [msg (. e getMessage)
            handshaker (atom nil)
            channel (. ctx getChannel)]
        (if (instance? HttpRequest msg)
          (let [host (HttpHeaders$Names/HOST)
                location (str "wss://" (. msg getHeader host) "/ws")
                wsfactory (WebSocketServerHandshakerFactory. location nil false)]
            (if (= "/" (. msg getUri))
              (fs/serve ctx e)
              (do
                (reset! handshaker (. wsfactory newHandshaker msg))
                (if (nil? @handshaker)
                  (. wsfactory sendUnsupportedWebSocketVersionResponse channel)
                  (do
                    (let [f (. @handshaker handshake channel msg)]
                      (. f addListener WebSocketServerHandshaker/HANDSHAKE_LISTENER))))))))
        (if (instance? WebSocketFrame msg)
          (do
            (if (instance? CloseWebSocketFrame msg) (. @handshaker close channel msg))
            (if (instance? PingWebSocketFrame msg) (. channel write (PongWebSocketFrame. (. msg getBinaryData))))
            (if (instance? TextWebSocketFrame msg) (. channel write (TextWebSocketFrame. (. msg getText))))))))
    (exceptionCaught [ctx e]
      (. (. e getChannel) close))))
